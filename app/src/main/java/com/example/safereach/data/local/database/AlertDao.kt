package com.example.safereach.data.local.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for accessing offline alerts in the Room database
 */
@Dao
interface AlertDao {
    
    /**
     * Insert a new offline alert into the database
     * @param alert The alert to insert
     * @return The row ID of the inserted alert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: OfflineAlert): Long
    
    /**
     * Get all pending offline alerts
     * @return A list of all offline alerts that need to be synced
     */
    @Query("SELECT * FROM offline_alerts WHERE synced = 0 ORDER BY timestamp ASC")
    suspend fun getPendingAlerts(): List<OfflineAlert>
    
    /**
     * Get a flow of all pending offline alerts
     * @return A flow of all offline alerts that need to be synced
     */
    @Query("SELECT * FROM offline_alerts WHERE synced = 0 ORDER BY timestamp ASC")
    fun getPendingAlertsFlow(): Flow<List<OfflineAlert>>
    
    /**
     * Count how many offline alerts are pending
     * @return The number of offline alerts that need to be synced
     */
    @Query("SELECT COUNT(*) FROM offline_alerts WHERE synced = 0")
    suspend fun countPendingAlerts(): Int
    
    /**
     * Mark an alert as synced
     * @param alertId The local ID of the alert to mark as synced
     */
    @Query("UPDATE offline_alerts SET synced = 1 WHERE id = :alertId")
    suspend fun markAlertAsSynced(alertId: Long)
    
    /**
     * Delete an alert from the offline database
     * @param alert The alert to delete
     */
    @Delete
    suspend fun deleteAlert(alert: OfflineAlert)
    
    /**
     * Delete all synced alerts
     * @return The number of alerts deleted
     */
    @Query("DELETE FROM offline_alerts WHERE synced = 1")
    suspend fun deleteSyncedAlerts(): Int
} 