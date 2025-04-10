package com.example.safereach.data.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Interface for managing user preferences
 */
interface PreferencesManager {
    /**
     * Sets the timestamp of the last emergency alert
     * @param timestamp Timestamp in milliseconds
     */
    suspend fun setLastAlertTime(timestamp: Long)
    
    // ... existing methods ...
} 