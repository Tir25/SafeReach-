package com.example.safereach.data.repository

import com.example.safereach.domain.model.Alert
import com.example.safereach.domain.model.LatLng
import com.example.safereach.domain.repository.AlertRepository
import com.example.safereach.utils.LogUtils
import com.example.safereach.utils.ResultWrapper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAlertRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : AlertRepository {
    
    companion object {
        private const val TAG = "FirebaseAlertRepository"
        private const val COLLECTION_ALERTS = "alerts"
        private const val EARTH_RADIUS_KM = 6371.0 // Earth's radius in km
    }
    
    override suspend fun createAlert(alert: Alert): ResultWrapper<String> {
        return try {
            val alertMap = mapOf(
                "userId" to alert.userId,
                "alertType" to alert.alertType,
                "location" to GeoPoint(alert.location.latitude, alert.location.longitude),
                "timestamp" to alert.timestamp
            )
            
            val reference = firestore.collection(COLLECTION_ALERTS).document()
            reference.set(alertMap).await()
            ResultWrapper.Success(reference.id)
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to create alert", e)
            ResultWrapper.Error(e, "Failed to create alert")
        }
    }
    
    override fun getNearbyAlerts(
        latitude: Double, 
        longitude: Double, 
        radiusInMeters: Double
    ): Flow<List<Alert>> = flow {
        try {
            // Convert meters to km for our calculation
            val radiusKm = radiusInMeters / 1000.0
            
            // Get all alerts from the last 24 hours
            val oneDayAgo = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
            val snapshot = firestore.collection(COLLECTION_ALERTS)
                .whereGreaterThan("timestamp", oneDayAgo)
                .get()
                .await()
                
            val alerts = snapshot.toAlerts()
            val nearbyAlerts = alerts.filter { alert ->
                val distance = calculateDistance(
                    latitude, longitude,
                    alert.location.latitude, alert.location.longitude
                )
                distance <= radiusKm
            }
            
            emit(nearbyAlerts)
        } catch (e: Exception) {
            LogUtils.e(TAG, "Error getting nearby alerts", e)
            emit(emptyList())
        }
    }
    
    override suspend fun resolveAlert(alertId: String): ResultWrapper<Boolean> {
        return try {
            firestore.collection(COLLECTION_ALERTS)
                .document(alertId)
                .update("resolved", true)
                .await()
                
            ResultWrapper.Success(true)
        } catch (e: Exception) {
            ResultWrapper.Error(e, "Failed to resolve alert")
        }
    }
    
    override suspend fun getUserAlerts(
        userId: String, 
        includeOffline: Boolean
    ): ResultWrapper<List<Alert>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_ALERTS)
                .whereEqualTo("userId", userId)
                .get()
                .await()
                
            ResultWrapper.Success(snapshot.toAlerts())
        } catch (e: Exception) {
            ResultWrapper.Error(e, "Failed to get user alerts")
        }
    }
    
    override suspend fun getAlertById(alertId: String): ResultWrapper<Alert> {
        return try {
            val document = firestore.collection(COLLECTION_ALERTS)
                .document(alertId)
                .get()
                .await()
                
            if (document.exists()) {
                val userId = document.getString("userId") ?: return ResultWrapper.Error(Exception("Invalid data"), "Invalid alert data")
                val alertType = document.getString("alertType") ?: return ResultWrapper.Error(Exception("Invalid data"), "Invalid alert data")
                val geoPoint = document.getGeoPoint("location") ?: return ResultWrapper.Error(Exception("Invalid data"), "Invalid alert data")
                val timestamp = document.getDate("timestamp") ?: return ResultWrapper.Error(Exception("Invalid data"), "Invalid alert data")
                
                val alert = Alert(
                    id = document.id,
                    userId = userId,
                    alertType = alertType,
                    location = LatLng(geoPoint.latitude, geoPoint.longitude),
                    timestamp = timestamp
                )
                
                ResultWrapper.Success(alert)
            } else {
                ResultWrapper.Error(Exception("Not found"), "Alert not found")
            }
        } catch (e: Exception) {
            ResultWrapper.Error(e, "Failed to get alert")
        }
    }
    
    override suspend fun saveAlertLocally(alert: Alert): ResultWrapper<Boolean> {
        // Not implemented in Firebase repository
        return ResultWrapper.Error(Exception("Not supported"), "Operation not supported")
    }
    
    override suspend fun getLocalUnsynedAlerts(): ResultWrapper<List<Alert>> {
        // Not implemented in Firebase repository
        return ResultWrapper.Success(emptyList())
    }
    
    override suspend fun syncLocalAlert(alertId: String): ResultWrapper<String> {
        // Not implemented in Firebase repository
        return ResultWrapper.Error(Exception("Not supported"), "Operation not supported")
    }
    
    override suspend fun hasPendingLocalAlerts(): ResultWrapper<Boolean> {
        // Not implemented in Firebase repository
        return ResultWrapper.Success(false)
    }
    
    private fun QuerySnapshot.toAlerts(): List<Alert> {
        return this.documents.mapNotNull { document ->
            try {
                val userId = document.getString("userId") ?: return@mapNotNull null
                val alertType = document.getString("alertType") ?: document.getString("type") ?: return@mapNotNull null
                val geoPoint = document.getGeoPoint("location") ?: return@mapNotNull null
                val timestamp = document.getDate("timestamp") ?: return@mapNotNull null
                
                Alert(
                    id = document.id,
                    userId = userId,
                    alertType = alertType,
                    location = LatLng(geoPoint.latitude, geoPoint.longitude),
                    timestamp = timestamp
                )
            } catch (e: Exception) {
                LogUtils.e(TAG, "Error parsing alert document", e)
                null
            }
        }
    }
    
    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val lon1Rad = Math.toRadians(lon1)
        val lon2Rad = Math.toRadians(lon2)
        
        val dLat = lat2Rad - lat1Rad
        val dLon = lon2Rad - lon1Rad
        
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return EARTH_RADIUS_KM * c
    }
} 
