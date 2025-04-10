package com.example.safereach.data.repository

import android.util.Log
import com.example.safereach.data.mapper.toDataModel
import com.example.safereach.data.mapper.toDomainModel
import com.example.safereach.data.model.Alert
import com.example.safereach.domain.repository.Result
import com.example.safereach.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TAG = "AlertRepository"
    }
    
    /**
     * Save an emergency alert to Firestore
     * @param alert The alert to save
     * @return Result containing the saved alert with its ID
     */
    suspend fun saveAlert(alert: Alert): Result<Alert> {
        return try {
            // Get current user ID or use anonymous ID if not logged in
            val userId = auth.currentUser?.uid ?: "anonymous_user"
            
            // Create a new alert with the user ID
            val alertWithUser = alert.copy(userId = userId)
            
            // Prepare alert data for Firestore
            val alertData = hashMapOf(
                "userId" to alertWithUser.userId,
                "type" to alertWithUser.type,
                "location" to GeoPoint(alertWithUser.latitude, alertWithUser.longitude),
                "timestamp" to FieldValue.serverTimestamp(),
                "resolved" to false
            )
            
            // Add alert to Firestore
            val alertRef = firestore.collection(Constants.ALERTS_COLLECTION)
                .document()
            
            alertRef.set(alertData).await()
            
            // Return success with the alert including the document ID
            val savedAlert = alertWithUser.copy(alertId = alertRef.id)
            Log.d(TAG, "Alert saved to Firestore with ID: ${savedAlert.alertId}")
            
            Result.Success(savedAlert)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving alert to Firestore", e)
            Result.Error("Failed to save alert: ${e.message}", e)
        }
    }
    
    /**
     * Get all alerts within a specific radius of a location
     * Note: This is a simplified implementation. In a real app, you would use
     * Firestore's GeoQuery or implement your own geohashing solution.
     */
    suspend fun getNearbyAlerts(
        latitude: Double,
        longitude: Double,
        radiusKm: Double = 5.0
    ): Result<List<Alert>> {
        return try {
            // For now, just get recent alerts (last 24 hours)
            // In a production app, you would implement proper geo-queries
            val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            
            val alertsSnapshot = firestore.collection(Constants.ALERTS_COLLECTION)
                .whereGreaterThan("timestamp", oneDayAgo)
                .get()
                .await()
            
            val alerts = alertsSnapshot.documents.mapNotNull { doc ->
                try {
                    val geoPoint = doc.getGeoPoint("location")
                    if (geoPoint != null) {
                        Alert(
                            alertId = doc.id,
                            userId = doc.getString("userId") ?: "",
                            type = doc.getString("type") ?: "",
                            latitude = geoPoint.latitude,
                            longitude = geoPoint.longitude,
                            timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis(),
                            resolved = doc.getBoolean("resolved") ?: false
                        )
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing alert document", e)
                    null
                }
            }
            
            Log.d(TAG, "Retrieved ${alerts.size} alerts from Firestore")
            Result.Success(alerts)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting alerts from Firestore", e)
            Result.Error("Failed to get alerts: ${e.message}", e)
        }
    }
    
    /**
     * Map a domain model alert to a data model and save it to Firestore
     */
    suspend fun saveDomainAlert(domainAlert: com.example.safereach.domain.model.Alert): Result<com.example.safereach.domain.model.Alert> {
        // Convert domain model to data model
        val dataAlert = domainAlert.toDataModel()
        
        // Save data model to Firestore
        val result = saveAlert(dataAlert)
        
        // Return domain model result
        return when (result) {
            is Result.Success -> Result.Success(result.data.toDomainModel())
            is Result.Error -> Result.Error(result.message, result.exception)
            is Result.Loading -> Result.Loading
        }
    }
    
    /**
     * Mark an alert as resolved
     */
    suspend fun resolveAlert(alertId: String): Result<Boolean> {
        return try {
            firestore.collection(Constants.ALERTS_COLLECTION)
                .document(alertId)
                .update("resolved", true)
                .await()
            
            Log.d(TAG, "Alert marked as resolved: $alertId")
            Result.Success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving alert", e)
            Result.Error("Failed to resolve alert: ${e.message}", e)
        }
    }
} 