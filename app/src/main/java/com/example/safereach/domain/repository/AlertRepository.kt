package com.example.safereach.domain.repository

import com.example.safereach.domain.model.Alert
import com.example.safereach.utils.ResultWrapper
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for emergency alerts
 */
interface AlertRepository {
    /**
     * Create a new alert
     * @return Result containing success or failure information
     */
    suspend fun createAlert(alert: Alert): ResultWrapper<String>
    
    /**
     * Get alerts near the specified location
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @param radiusInMeters Search radius in meters
     * @return Flow of alerts near the location
     */
    fun getNearbyAlerts(latitude: Double, longitude: Double, radiusInMeters: Double): Flow<List<Alert>>
    
    /**
     * Marks an alert as resolved
     * @param alertId The ID of the alert to resolve
     * @return Result indicating whether the operation was successful
     */
    suspend fun resolveAlert(alertId: String): ResultWrapper<Boolean>
    
    /**
     * Gets alerts created by a specific user
     * @param userId The ID of the user to get alerts for
     * @param includeOffline Whether to include offline alerts
     * @return Result containing a list of alerts
     */
    suspend fun getUserAlerts(
        userId: String,
        includeOffline: Boolean = true
    ): ResultWrapper<List<Alert>>
    
    /**
     * Gets alert details by ID
     * @param alertId The ID of the alert to get details for
     * @return Result containing the alert details
     */
    suspend fun getAlertById(alertId: String): ResultWrapper<Alert>
    
    /**
     * Saves an alert locally for offline use
     * @param alert The alert to save locally
     * @return Result indicating whether the operation was successful
     */
    suspend fun saveAlertLocally(alert: Alert): ResultWrapper<Boolean>
    
    /**
     * Gets locally stored alerts that need to be synced
     * @return Result containing a list of locally stored alerts
     */
    suspend fun getLocalUnsynedAlerts(): ResultWrapper<List<Alert>>
    
    /**
     * Syncs a locally stored alert with the server
     * @param alertId The ID of the locally stored alert to sync
     * @return Result containing the ID of the synced alert on the server
     */
    suspend fun syncLocalAlert(alertId: String): ResultWrapper<String>
    
    /**
     * Checks if there are any locally stored alerts that need to be synced
     * @return Result containing a boolean indicating if there are pending alerts
     */
    suspend fun hasPendingLocalAlerts(): ResultWrapper<Boolean>
} 