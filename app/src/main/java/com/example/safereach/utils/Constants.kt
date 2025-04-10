package com.example.safereach.utils

/**
 * Application-wide constants.
 */
object Constants {
    // Firestore Collections
    const val USERS_COLLECTION = "users"
    const val ALERTS_COLLECTION = "alerts"
    
    // Notification Channels
    const val EMERGENCY_CHANNEL_ID = "emergency_channel"
    const val ALERT_CHANNEL_ID = "alert_channel"
    
    // Worker Tags
    const val ALERT_SYNC_WORKER = "alert_sync_worker"
    
    // Shared Preferences
    const val PREFS_NAME = "safereach_prefs"
    const val PREF_LAST_ALERT_TIME = "last_alert_time"
    const val PREF_COOLDOWN_PERIOD_MS = 10 * 60 * 1000L // 10 minutes
    
    // Location Updates
    const val LOCATION_UPDATE_INTERVAL = 5000L // 5 seconds
    const val LOCATION_FASTEST_INTERVAL = 2000L // 2 seconds
    
    // Map Settings
    const val DEFAULT_ZOOM = 15f
    const val DEFAULT_ALERT_RADIUS_KM = 10.0 // 10 kilometers
    
    // Auth
    const val MIN_PASSWORD_LENGTH = 6
    
    // Emergency Types
    const val EMERGENCY_TYPE_POLICE = "police"
    const val EMERGENCY_TYPE_AMBULANCE = "ambulance"
    const val EMERGENCY_TYPE_FIRE = "fire"
    
    // Preferences
    const val PREFERENCES_NAME = "safereach_settings"
    const val KEY_AUTO_LOCATION_SHARING = "auto_location_sharing"
    const val KEY_OFFLINE_FALLBACK = "offline_fallback"
    const val KEY_NOTIFICATION_SOUNDS = "notification_sounds"
    const val KEY_NEARBY_ALERTS = "nearby_alerts"
    const val KEY_NEARBY_ALERTS_RADIUS = "nearby_alerts_radius"
    
    // Emergency Throttling
    const val EMERGENCY_TRIGGER_COOLDOWN = 600000L  // 10 minutes
    
    // Offline Storage
    const val OFFLINE_ALERT_PREFIX = "offline_"
    const val SYNC_INTERVAL_HOURS = 1L
    const val MAX_SYNC_RETRIES = 3
    
    // Notification Channels
    const val CHANNEL_EMERGENCY_ALERTS = "emergency_alerts"
    const val CHANNEL_SYNC_NOTIFICATIONS = "sync_notifications"
    const val CHANNEL_NEARBY_ALERTS = "nearby_alerts"
    
    // Emergency trigger cooldown (5 minutes)
    const val ALERT_COOLDOWN_MS = 5 * 60 * 1000L
    
    // Location update interval (30 seconds)
    const val LOCATION_UPDATE_INTERVAL_MS = 30 * 1000L
    
    // Minimum distance for location update (10 meters)
    const val LOCATION_MIN_DISTANCE_M = 10f
    
    // Nearby alerts radius in meters (1km)
    const val NEARBY_ALERTS_RADIUS_M = 1000.0
} 