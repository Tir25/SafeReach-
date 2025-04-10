package com.example.safereach.data.repository

import android.content.Context
import android.util.Log
import androidx.work.WorkManager
import com.example.safereach.data.local.AlertLocalDataSource
import com.example.safereach.data.local.PreferencesManager
import com.example.safereach.data.model.Alert as DataAlert
import com.example.safereach.data.workers.AlertSyncWorker
import com.example.safereach.domain.model.Alert
import com.example.safereach.domain.model.LatLng
import com.example.safereach.domain.repository.AlertRepository
import com.example.safereach.utils.NetworkUtils
import com.example.safereach.utils.ResultWrapper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AlertRepository that supports offline functionality
 */
@Singleton
class AlertRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestoreRepository: FirestoreAlertRepository,
    private val localDataSource: AlertLocalDataSource,
    private val preferencesManager: PreferencesManager,
    private val workManager: WorkManager
) : AlertRepository {
    
    companion object {
        private const val TAG = "AlertRepositoryImpl"
    }
    
    /**
     * Creates a new alert, using Firestore if online or local storage if offline
     */
    override suspend fun createAlert(alert: Alert): ResultWrapper<String> {
        // Check user preference for offline fallback
        val offlineFallbackEnabled = preferencesManager.offlineFallbackFlow.first()
        
        // Check network connectivity
        val isOnline = NetworkUtils.isNetworkAvailable(context)
        
        return if (isOnline) {
            // Online - try to create in Firestore directly
            try {
                val result = firestoreRepository.createAlert(alert)
                
                if (result is ResultWrapper.Success) {
                    Log.d(TAG, "Alert created online with ID: ${result.data}")
                    
                    // Schedule a sync in case there are any pending offline alerts
                    AlertSyncWorker.startOneTimeSync(context)
                }
                
                result
            } catch (e: Exception) {
                // Failed to create online, fallback to offline if enabled
                handleOfflineFallback(alert, e, offlineFallbackEnabled)
            }
        } else {
            // Offline - store locally if enabled
            handleOfflineFallback(alert, null, offlineFallbackEnabled)
        }
    }
    
    /**
     * Handle storing an alert offline
     */
    private suspend fun handleOfflineFallback(
        alert: Alert,
        error: Exception?,
        offlineFallbackEnabled: Boolean
    ): ResultWrapper<String> {
        if (!offlineFallbackEnabled) {
            val errorMsg = "Failed to create alert and offline fallback is disabled"
            Log.e(TAG, errorMsg, error)
            return ResultWrapper.Error(error ?: Exception(errorMsg), errorMsg)
        }
        
        try {
            // Create offline alert model
            val dataAlert = DataAlert(
                alertId = "offline_${System.currentTimeMillis()}",
                userId = alert.userId,
                type = alert.alertType,
                latitude = alert.location.latitude,
                longitude = alert.location.longitude,
                timestamp = alert.timestamp.time,
                resolved = false,
                offlineSynced = false
            )
            
            // Save locally
            val localId = localDataSource.saveAlert(dataAlert)
            
            if (localId > 0) {
                Log.d(TAG, "Alert saved offline with local ID: $localId")
                
                // Schedule sync when online
                if (error == null) {
                    Log.d(TAG, "Device is offline, alert will be synced when online")
                } else {
                    Log.d(TAG, "Online creation failed, alert saved offline and will be synced later", error)
                }
                
                return ResultWrapper.Success("offline_$localId")
            } else {
                val errorMsg = "Failed to save alert offline"
                Log.e(TAG, errorMsg)
                return ResultWrapper.Error(Exception(errorMsg), errorMsg)
            }
        } catch (e: Exception) {
            val errorMsg = "Error saving alert offline"
            Log.e(TAG, errorMsg, e)
            return ResultWrapper.Error(e, errorMsg)
        }
    }
    
    /**
     * Gets a flow of nearby unresolved alerts
     */
    override fun getNearbyAlerts(
        latitude: Double,
        longitude: Double,
        radiusInMeters: Double
    ): Flow<List<Alert>> {
        // For now, just use the Firestore implementation
        // In a more complete implementation, we might combine online and offline alerts
        return firestoreRepository.getNearbyAlerts(latitude, longitude, radiusInMeters)
    }
    
    /**
     * Marks an alert as resolved
     */
    override suspend fun resolveAlert(alertId: String): ResultWrapper<Boolean> {
        // Handle offline alert IDs
        if (alertId.startsWith("offline_")) {
            val localId = alertId.substringAfter("offline_").toLong()
            // For offline alerts, we just mark them as synced to remove them from pending queue
            localDataSource.markAlertAsSynced(localId)
            return ResultWrapper.Success(true)
        }
        
        // For online alerts, pass to Firestore
        return firestoreRepository.resolveAlert(alertId)
    }
    
    /**
     * Gets alerts created by a specific user
     */
    override suspend fun getUserAlerts(
        userId: String,
        includeOffline: Boolean
    ): ResultWrapper<List<Alert>> {
        return firestoreRepository.getUserAlerts(userId, includeOffline)
    }
    
    /**
     * Gets alert details by ID
     */
    override suspend fun getAlertById(alertId: String): ResultWrapper<Alert> {
        return firestoreRepository.getAlertById(alertId)
    }
    
    /**
     * Saves an alert locally for offline use
     */
    override suspend fun saveAlertLocally(alert: Alert): ResultWrapper<Boolean> {
        return firestoreRepository.saveAlertLocally(alert)
    }
    
    /**
     * Gets locally stored alerts that need to be synced
     */
    override suspend fun getLocalUnsynedAlerts(): ResultWrapper<List<Alert>> {
        return firestoreRepository.getLocalUnsynedAlerts()
    }
    
    /**
     * Syncs a locally stored alert with the server
     */
    override suspend fun syncLocalAlert(alertId: String): ResultWrapper<String> {
        return firestoreRepository.syncLocalAlert(alertId)
    }
    
    /**
     * Checks if there are any locally stored alerts that need to be synced
     */
    override suspend fun hasPendingLocalAlerts(): ResultWrapper<Boolean> {
        return firestoreRepository.hasPendingLocalAlerts()
    }
} 