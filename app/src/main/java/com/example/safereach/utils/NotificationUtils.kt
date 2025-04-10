package com.example.safereach.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.safereach.MainActivity
import com.example.safereach.R
import com.example.safereach.data.model.EmergencyType
import com.example.safereach.domain.model.Alert
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationUtils {
    
    private const val TAG = "NotificationUtils"
    
    // Notification channel constants
    const val CHANNEL_ID_EMERGENCY = "emergency_channel"
    const val CHANNEL_ID_NEARBY_ALERTS = "nearby_alerts_channel"
    const val NOTIFICATION_ID_EMERGENCY = 1001
    const val NOTIFICATION_ID_NEARBY_ALERT_BASE = 2000 // Base ID for nearby alerts
    
    /**
     * Creates all notification channels for the app (only needed on Android O+)
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Create emergency alert channel
            val emergencyChannel = NotificationChannel(
                CHANNEL_ID_EMERGENCY,
                "Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows notifications for emergency alerts"
                enableLights(true)
                enableVibration(true)
            }
            
            // Create nearby alerts channel
            val nearbyAlertsChannel = NotificationChannel(
                CHANNEL_ID_NEARBY_ALERTS,
                "Nearby Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows notifications for nearby emergency alerts in your area"
                enableLights(true)
                enableVibration(true)
            }
            
            // Register channels with the system
            notificationManager.createNotificationChannels(listOf(emergencyChannel, nearbyAlertsChannel))
        }
    }
    
    /**
     * Show emergency alert notification
     */
    fun showEmergencyNotification(
        context: Context,
        emergencyType: EmergencyType,
        locationText: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel if needed
        createNotificationChannels(context)
        
        // Get appropriate icon and color for the emergency type
        val icon = getIconForEmergencyType(emergencyType)
        val color = getColorResourceForEmergencyType(emergencyType)
        
        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_EMERGENCY)
            .setSmallIcon(icon)
            .setContentTitle("Emergency Alert Sent: ${emergencyType.displayName}")
            .setContentText("Your location ($locationText) has been shared with emergency services.")
            .setColor(color)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        
        // Show notification
        notificationManager.notify(NOTIFICATION_ID_EMERGENCY, notification)
        
        LogUtils.d(TAG, "Emergency notification shown for ${emergencyType.name}")
    }
    
    /**
     * Show a notification for a nearby emergency alert
     */
    fun showNearbyAlertNotification(
        context: Context,
        alert: Alert
    ) {
        // Create an intent to open the app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("ALERT_ID", alert.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, alert.id.hashCode(), intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Parse emergency type
        val emergencyType = try {
            EmergencyType.fromString(alert.alertType)
        } catch (e: Exception) {
            EmergencyType.POLICE // Default
        }
        
        // Format time
        val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        val timeString = timeFormatter.format(alert.timestamp)
        
        // Format location
        val locationText = String.format(
            "%.6f, %.6f", 
            alert.location.latitude, 
            alert.location.longitude
        )
        
        // Calculate distance in km from user's last known location
        // This would require the user's location - for now we'll just show a nearby message
        
        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_NEARBY_ALERTS)
            .setSmallIcon(getIconForEmergencyType(emergencyType))
            .setContentTitle("Nearby ${emergencyType.displayName} Alert")
            .setContentText("Emergency alert reported near your location at $timeString")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("A ${emergencyType.displayName} emergency has been reported near your location at $timeString. " +
                    "Location: $locationText"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(ContextCompat.getColor(context, getColorResourceForEmergencyType(emergencyType)))
            .build()
        
        // Generate a unique notification ID based on alert ID
        val notificationId = NOTIFICATION_ID_NEARBY_ALERT_BASE + alert.id.hashCode().rem(1000)
        
        // Show notification
        with(NotificationManagerCompat.from(context)) {
            try {
                notify(notificationId, notification)
            } catch (e: SecurityException) {
                // Handle missing notification permission (Android 13+)
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Get icon resource for the emergency type
     */
    private fun getIconForEmergencyType(emergencyType: EmergencyType): Int {
        return when (emergencyType) {
            EmergencyType.POLICE -> R.drawable.ic_police
            EmergencyType.AMBULANCE -> R.drawable.ic_ambulance
            EmergencyType.FIRE -> R.drawable.ic_fire
        }
    }
    
    /**
     * Get color resource for the emergency type
     */
    private fun getColorResourceForEmergencyType(emergencyType: EmergencyType): Int {
        return when (emergencyType) {
            EmergencyType.POLICE -> android.R.color.holo_blue_dark
            EmergencyType.AMBULANCE -> android.R.color.holo_red_dark
            EmergencyType.FIRE -> android.R.color.holo_orange_dark
        }
    }
} 