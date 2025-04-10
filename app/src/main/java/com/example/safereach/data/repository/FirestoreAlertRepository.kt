package com.example.safereach.data.repository

import android.util.Log
import com.example.safereach.data.mapper.toDataModel
import com.example.safereach.data.mapper.toDomainModel
import com.example.safereach.data.model.Alert as DataAlert
import com.example.safereach.domain.model.Alert
import com.example.safereach.domain.model.LatLng
import com.example.safereach.domain.repository.AlertRepository
import com.example.safereach.utils.ResultWrapper
import com.example.safereach.utils.Constants.ALERTS_COLLECTION
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Firestore implementation of the AlertRepository interface.
 */
@Singleton
class FirestoreAlertRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : AlertRepository {

    companion object {
        private const val TAG = "FirestoreAlertRepository"
        private const val EARTH_RADIUS_KM = 6371.0 // Earth radius in kilometers
    }

    /**
     * Creates a new alert in Firestore based on the Alert domain model
     */
    override suspend fun createAlert(alert: Alert): ResultWrapper<String> {
        try {
            // Create Firestore data object
            val alertData = hashMapOf(
                "userId" to alert.userId,
                "type" to alert.alertType,
                "location" to GeoPoint(alert.location.latitude, alert.location.longitude),
                "timestamp" to alert.timestamp,
                "resolved" to false
            )
            
            // Add to Firestore
            val alertRef = firestore.collection(ALERTS_COLLECTION).document()
            alertRef.set(alertData).await()
            
            Log.d(TAG, "Created alert with ID: ${alertRef.id}")
            return ResultWrapper.Success(alertRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create alert", e)
            return ResultWrapper.Error(e, "Failed to create alert: ${e.message}")
        }
    }

    /**
     * Gets a flow of nearby alerts within the specified radius
     */
    override fun getNearbyAlerts(
        latitude: Double, 
        longitude: Double, 
        radiusInMeters: Double
    ): Flow<List<Alert>> = flow {
        try {
            // Get all alerts from the last 24 hours
            val oneDayAgo = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
            val querySnapshot = firestore.collection(ALERTS_COLLECTION)
                .whereGreaterThan("timestamp", oneDayAgo)
                .get()
                .await()
            
            // Convert to domain models
            val alerts = querySnapshot.documents
                .mapNotNull { doc -> doc.toDataAlert()?.toDomainModel() }
                .filter { alert ->
                    // Calculate distance and filter by radius
                    val distance = calculateDistance(
                        latitude, longitude,
                        alert.location.latitude, alert.location.longitude
                    )
                    distance <= (radiusInMeters / 1000.0) // Convert meters to km
                }
            
            emit(alerts)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting nearby alerts", e)
            emit(emptyList<Alert>())
        }
    }

    /**
     * Marks an alert as resolved in Firestore
     */
    override suspend fun resolveAlert(alertId: String): ResultWrapper<Boolean> {
        return try {
            firestore.collection(ALERTS_COLLECTION)
                .document(alertId)
                .update("resolved", true)
                .await()
            
            Log.d(TAG, "Alert $alertId marked as resolved")
            ResultWrapper.Success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve alert $alertId", e)
            ResultWrapper.Error(e, "Failed to resolve alert: ${e.message}")
        }
    }
    
    /**
     * Gets alerts created by a specific user
     */
    override suspend fun getUserAlerts(
        userId: String,
        includeOffline: Boolean
    ): ResultWrapper<List<Alert>> {
        return try {
            // Use simple query without complex conditions that might cause issues
            val querySnapshot = firestore.collection(ALERTS_COLLECTION)
                .get()
                .await()
            
            // Filter manually in code
            val alerts = querySnapshot.documents
                .mapNotNull { doc: DocumentSnapshot -> doc.toDataAlert() }
                .filter { it.userId == userId } // Filter by user ID manually
                .sortedByDescending { it.timestamp } // Sort by timestamp manually
                .map { it.toDomainModel() }
            
            // If includeOffline is true, we would also fetch locally stored alerts here
            
            Log.d(TAG, "Retrieved ${alerts.size} alerts for user $userId")
            ResultWrapper.Success(alerts)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user alerts for $userId", e)
            ResultWrapper.Error(e, "Failed to get user alerts: ${e.message}")
        }
    }
    
    override suspend fun getAlertById(id: String): ResultWrapper<Alert> {
        return try {
            val alertDoc = firestore.collection(ALERTS_COLLECTION).document(id).get().await()
            if (alertDoc.exists()) {
                val alert = alertDoc.toDataAlert()
                if (alert != null) {
                    ResultWrapper.Success(alert.toDomainModel())
                } else {
                    ResultWrapper.Error(Exception("Invalid alert data"), "Invalid alert data")
                }
            } else {
                ResultWrapper.Error(Exception("Alert not found"), "Alert not found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get alert by ID: $id", e)
            ResultWrapper.Error(e, "Failed to get alert: ${e.message}")
        }
    }
    
    override suspend fun saveAlertLocally(alert: Alert): ResultWrapper<Boolean> {
        // This would be implemented using Room or SharedPreferences
        // For now, we'll just return a failure
        return ResultWrapper.Error(Exception("Not implemented"), "Local storage not implemented")
    }
    
    override suspend fun getLocalUnsynedAlerts(): ResultWrapper<List<Alert>> {
        // This would be implemented using Room or SharedPreferences
        return ResultWrapper.Success(emptyList())
    }
    
    override suspend fun syncLocalAlert(alertId: String): ResultWrapper<String> {
        // This would involve reading from local storage and pushing to Firestore
        return ResultWrapper.Error(Exception("Not implemented"), "Local storage sync not implemented")
    }
    
    override suspend fun hasPendingLocalAlerts(): ResultWrapper<Boolean> {
        // This would check if there are any unsynced alerts in local storage
        return ResultWrapper.Success(false)
    }
    
    /**
     * Helper function to convert a Firestore DocumentSnapshot to a DataAlert
     */
    private fun DocumentSnapshot.toDataAlert(): DataAlert? {
        return try {
            val id = this.id
            val userId = this.getString("userId") ?: return null
            val type = this.getString("type") ?: return null
            val geoPoint = this.getGeoPoint("location") ?: return null
            val timestamp = this.getTimestamp("timestamp")?.toDate()?.time 
                ?: this.getDate("timestamp")?.time 
                ?: return null
            val resolved = this.getBoolean("resolved") ?: false
            
            DataAlert(
                alertId = id,
                userId = userId,
                type = type,
                latitude = geoPoint.latitude,
                longitude = geoPoint.longitude,
                timestamp = timestamp,
                resolved = resolved
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error converting document to alert", e)
            null
        }
    }
    
    /**
     * Calculate distance between two points using the Haversine formula
     */
    private fun calculateHaversineDistance(
        lat1: Double, 
        lng1: Double, 
        lat2: Double, 
        lng2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) + 
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * 
                sin(dLng / 2).pow(2)
        val c = 2 * Math.atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }
    
    private fun sin(value: Double): Double = kotlin.math.sin(value)

    /**
     * Gets the current user ID or empty string if not authenticated
     */
    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: ""
    }

    /**
     * Converts a domain model Alert to a Firestore data model
     */
    private fun Alert.toDataModel(): DataAlert {
        return DataAlert(
            id = id,
            userId = userId,
            type = alertType,
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = timestamp.time,
            resolved = false // Always false when creating a new alert
        )
    }

    /**
     * Calculate distance between two points using the Haversine formula
     */
    private fun calculateDistance(
        lat1: Double, 
        lng1: Double, 
        lat2: Double, 
        lng2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) + 
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * 
                sin(dLng / 2).pow(2)
        val c = 2 * Math.atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }
} 