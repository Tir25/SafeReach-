package com.example.safereach.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "safereach_preferences")

@Singleton
class PreferencesManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesManager {

    companion object {
        private val LAST_ALERT_TIME = longPreferencesKey("last_alert_time")
        // Add other preference keys as needed
    }
    
    /**
     * Sets the timestamp of the last emergency alert
     */
    override suspend fun setLastAlertTime(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_ALERT_TIME] = timestamp
        }
    }
    
    // Implement other methods from the PreferencesManager interface
} 