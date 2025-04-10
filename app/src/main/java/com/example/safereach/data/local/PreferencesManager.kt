package com.example.safereach.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.safereach.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Extension property for Context to create a DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = Constants.PREFERENCES_NAME
)

/**
 * Manager for handling app preferences using DataStore
 */
@Singleton
class PreferencesManager @Inject constructor(
    private val context: Context
) {
    // Keys for preferences
    private object PreferencesKeys {
        val AUTO_LOCATION_SHARING = booleanPreferencesKey(Constants.KEY_AUTO_LOCATION_SHARING)
        val OFFLINE_FALLBACK = booleanPreferencesKey(Constants.KEY_OFFLINE_FALLBACK)
        val NOTIFICATION_SOUNDS = booleanPreferencesKey(Constants.KEY_NOTIFICATION_SOUNDS)
        val NEARBY_ALERTS = booleanPreferencesKey(Constants.KEY_NEARBY_ALERTS)
        val NEARBY_ALERTS_RADIUS = doublePreferencesKey(Constants.KEY_NEARBY_ALERTS_RADIUS)
        val LAST_ALERT_TIMESTAMP = longPreferencesKey(Constants.PREF_LAST_ALERT_TIME)
    }

    // Default values
    private val defaultAutoLocationSharing = true
    private val defaultOfflineFallback = true
    private val defaultNotificationSounds = true
    private val defaultNearbyAlerts = true
    private val defaultNearbyAlertsRadius = 10.0 // 10 kilometers

    /**
     * Get auto location sharing preference as flow
     */
    val autoLocationSharingFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.AUTO_LOCATION_SHARING] ?: defaultAutoLocationSharing
        }

    /**
     * Get offline fallback preference as flow
     */
    val offlineFallbackFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.OFFLINE_FALLBACK] ?: defaultOfflineFallback
        }

    /**
     * Get notification sounds preference as flow
     */
    val notificationSoundsFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_SOUNDS] ?: defaultNotificationSounds
        }
    
    /**
     * Get nearby alerts preference as flow
     */
    val nearbyAlertsFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.NEARBY_ALERTS] ?: defaultNearbyAlerts
        }
    
    /**
     * Get nearby alerts radius preference as flow (in kilometers)
     */
    val nearbyAlertsRadiusFlow: Flow<Double> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.NEARBY_ALERTS_RADIUS] ?: defaultNearbyAlertsRadius
        }

    /**
     * Get last alert timestamp as flow
     */
    val lastAlertTimestampFlow: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.LAST_ALERT_TIMESTAMP] ?: 0L
        }

    /**
     * Update auto location sharing preference
     */
    suspend fun updateAutoLocationSharing(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_LOCATION_SHARING] = enabled
        }
    }

    /**
     * Update offline fallback preference
     */
    suspend fun updateOfflineFallback(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OFFLINE_FALLBACK] = enabled
        }
    }

    /**
     * Update notification sounds preference
     */
    suspend fun updateNotificationSounds(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_SOUNDS] = enabled
        }
    }
    
    /**
     * Update nearby alerts preference
     */
    suspend fun updateNearbyAlerts(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NEARBY_ALERTS] = enabled
        }
    }
    
    /**
     * Update nearby alerts radius
     */
    suspend fun updateNearbyAlertsRadius(radiusKm: Double) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NEARBY_ALERTS_RADIUS] = radiusKm
        }
    }

    /**
     * Update last alert timestamp
     */
    suspend fun updateLastAlertTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_ALERT_TIMESTAMP] = timestamp
        }
    }

    /**
     * Check if user can trigger emergency (based on cooldown period)
     */
    fun canTriggerEmergency(): Boolean {
        val lastAlertTime = context.getSharedPreferences(
            Constants.PREFS_NAME, Context.MODE_PRIVATE
        ).getLong(Constants.PREF_LAST_ALERT_TIME, 0L)

        val cooldownPeriod = Constants.PREF_COOLDOWN_PERIOD_MS
        val currentTime = System.currentTimeMillis()

        return currentTime - lastAlertTime >= cooldownPeriod
    }

    /**
     * Get remaining cooldown time in milliseconds
     */
    fun getRemainingCooldownTime(): Long {
        val lastAlertTime = context.getSharedPreferences(
            Constants.PREFS_NAME, Context.MODE_PRIVATE
        ).getLong(Constants.PREF_LAST_ALERT_TIME, 0L)

        val cooldownPeriod = Constants.PREF_COOLDOWN_PERIOD_MS
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastAlertTime
        
        return if (elapsed >= cooldownPeriod) 0L else cooldownPeriod - elapsed
    }
    
    /**
     * Save last emergency trigger time using SharedPreferences for backwards compatibility
     */
    fun saveLastEmergencyTriggerTime(timestamp: Long) {
        context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(Constants.PREF_LAST_ALERT_TIME, timestamp)
            .apply()
    }
} 