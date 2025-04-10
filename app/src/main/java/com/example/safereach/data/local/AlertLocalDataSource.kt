package com.example.safereach.data.local

import com.example.safereach.data.local.database.AlertDao
import com.example.safereach.data.local.database.OfflineAlert
import com.example.safereach.data.model.Alert
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local data source for alerts using Room database
 */
@Singleton
class AlertLocalDataSource @Inject constructor(
    private val alertDao: AlertDao
) {
    /**
     * Save an alert locally
     * @param alert The alert to save
     * @return The ID of the saved alert
     */
    suspend fun saveAlert(alert: Alert): Long {
        val offlineAlert = OfflineAlert(
            userId = alert.userId,
            type = alert.type,
            latitude = alert.latitude,
            longitude = alert.longitude,
            timestamp = alert.timestamp,
            resolved = alert.resolved,
            synced = false
        )
        return alertDao.insertAlert(offlineAlert)
    }
    
    /**
     * Get all pending alerts that need to be synced
     * @return List of pending alerts
     */
    suspend fun getPendingAlerts(): List<OfflineAlert> {
        return alertDao.getPendingAlerts()
    }
    
    /**
     * Get a flow of all pending alerts
     * @return Flow of pending alerts
     */
    fun getPendingAlertsFlow(): Flow<List<OfflineAlert>> {
        return alertDao.getPendingAlertsFlow()
    }
    
    /**
     * Count the number of pending alerts
     * @return Number of pending alerts
     */
    suspend fun countPendingAlerts(): Int {
        return alertDao.countPendingAlerts()
    }
    
    /**
     * Mark an alert as synced with Firestore
     * @param alertId The local ID of the alert
     * @param firestoreId The Firestore ID of the alert
     */
    suspend fun markAlertAsSynced(alertId: Long) {
        alertDao.markAlertAsSynced(alertId)
    }
    
    /**
     * Clean up synced alerts from the database
     * @return The number of alerts cleaned up
     */
    suspend fun cleanupSyncedAlerts(): Int {
        return alertDao.deleteSyncedAlerts()
    }
    
    /**
     * Convert an OfflineAlert to an Alert model
     */
    fun OfflineAlert.toAlert(): Alert {
        return Alert(
            alertId = firestoreId.takeIf { it.isNotEmpty() } ?: "",
            userId = userId,
            type = type,
            latitude = latitude,
            longitude = longitude,
            timestamp = timestamp,
            resolved = resolved,
            offlineSynced = synced
        )
    }
} 