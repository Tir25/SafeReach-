package com.example.safereach.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.safereach.MainActivity
import com.example.safereach.R
import com.example.safereach.data.local.PreferencesManager
import com.example.safereach.data.repository.FirestoreAlertRepository
import com.example.safereach.presentation.location.LocationManager
import com.example.safereach.utils.Constants
import com.example.safereach.utils.NotificationUtils
import com.example.safereach.utils.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Service that listens for nearby alerts and shows notifications
 */
@AndroidEntryPoint
class AlertNotificationService : Service() {
    
    @Inject
    lateinit var alertRepository: FirestoreAlertRepository
    
    @Inject
    lateinit var locationManager: LocationManager
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isMonitoring = false
    
    companion object {
        private const val TAG = "AlertNotificationSvc"
        private const val MIN_ALERT_NOTIFICATION_INTERVAL_MS = 60000L // 1 minute
        private const val FOREGROUND_SERVICE_ID = 3000
        private const val CHANNEL_ID_SERVICE = "alert_notification_service"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AlertNotificationService created")
        createServiceNotificationChannel()
        
        // Check for required permissions before starting as foreground
        if (PermissionUtils.hasLocationPermissions(this)) {
            startForeground()
        } else {
            Log.e(TAG, "Missing location permissions, cannot start foreground service")
            stopSelf()
            return
        }
    }
    
    private fun createServiceNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_SERVICE,
                "Alert Notification Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background service for monitoring nearby alerts"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun startForeground() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_SERVICE)
            .setContentTitle("SafeReach Alert Monitoring")
            .setContentText("Monitoring for nearby emergency alerts")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(FOREGROUND_SERVICE_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(FOREGROUND_SERVICE_ID, notification)
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "AlertNotificationService started")
        
        // Verify permissions again in case they changed
        if (!PermissionUtils.hasLocationPermissions(this)) {
            Log.e(TAG, "Missing location permissions, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }
        
        if (!isMonitoring) {
            startMonitoringAlerts()
        }
        
        // Keep service running
        return START_STICKY
    }
    
    private fun startMonitoringAlerts() {
        Log.d(TAG, "Starting to monitor alerts")
        isMonitoring = true
        
        serviceScope.launch {
            // Combine preferences for nearby alerts and radius
            val nearbyAlertsEnabled = preferencesManager.nearbyAlertsFlow.first()
            
            if (!nearbyAlertsEnabled) {
                Log.d(TAG, "Nearby alerts notifications are disabled")
                return@launch
            }
            
            // Monitor location updates
            locationManager.startLocationUpdates()
            
            // Combine location updates and preferences
            combine(
                locationManager.locationState,
                preferencesManager.nearbyAlertsRadiusFlow
            ) { locationState, radius ->
                Pair(locationState, radius)
            }
            .filter { (locationState, _): Pair<com.example.safereach.presentation.location.LocationState, Double> -> 
                locationState.hasLocationData 
            }
            .distinctUntilChanged { old: Pair<com.example.safereach.presentation.location.LocationState, Double>, 
                                   new: Pair<com.example.safereach.presentation.location.LocationState, Double> ->
                // Only process if location changed significantly or radius changed
                val oldLocation = old.first.location
                val newLocation = new.first.location
                val oldRadius = old.second
                val newRadius = new.second
                
                // If either changed by more than 100 meters, process
                val locationDistance = if (oldLocation != null && newLocation != null) {
                    val results = FloatArray(1)
                    android.location.Location.distanceBetween(
                        oldLocation.latitude, oldLocation.longitude,
                        newLocation.latitude, newLocation.longitude,
                        results
                    )
                    results[0].toDouble()
                } else {
                    Double.MAX_VALUE // Force processing if either location is null
                }
                
                val significantChange = locationDistance > 100 || oldRadius != newRadius
                !significantChange
            }
            .collect { (locationState, radius): Pair<com.example.safereach.presentation.location.LocationState, Double> ->
                val location = locationState.location
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    
                    Log.d(TAG, "Checking for nearby alerts at ($latitude, $longitude) with radius ${radius}km")
                    
                    // Listen for nearby alerts
                    alertRepository.getNearbyAlerts(latitude, longitude, radius)
                        .distinctUntilChanged()
                        .collect { alerts: List<com.example.safereach.domain.model.Alert> ->
                            if (alerts.isNotEmpty()) {
                                Log.d(TAG, "Found ${alerts.size} nearby alerts")
                                
                                // Only show for recent alerts (last 30 minutes)
                                val currentTime = System.currentTimeMillis()
                                val recentAlerts = alerts.filter { alert: com.example.safereach.domain.model.Alert ->
                                    currentTime - alert.timestamp.time < 30 * 60 * 1000L
                                }
                                
                                if (recentAlerts.isNotEmpty()) {
                                    Log.d(TAG, "Found ${recentAlerts.size} recent nearby alerts")
                                    
                                    // Get current user ID to filter out the user's own alerts
                                    val currentUserId = alertRepository.getCurrentUserId()
                                    
                                    // Show notification for each alert (except user's own)
                                    recentAlerts.filter { it.userId != currentUserId }
                                        .forEach { alert: com.example.safereach.domain.model.Alert ->
                                            NotificationUtils.showNearbyAlertNotification(
                                                context = applicationContext,
                                                alert = alert
                                            )
                                        }
                                }
                            } else {
                                Log.d(TAG, "No nearby alerts found")
                            }
                        }
                } else {
                    Log.w(TAG, "Location is null, skipping nearby alerts check")
                }
            }
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AlertNotificationService destroyed")
        serviceScope.cancel()
        isMonitoring = false
    }
} 