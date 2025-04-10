package com.example.safereach.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.firebase.Timestamp
import com.example.safereach.data.model.Alert
import com.example.safereach.data.model.User
import kotlinx.coroutines.delay
import java.util.Date
import java.util.UUID

/**
 * Utility class for test and debugging purposes
 * Contains methods to simulate edge cases and test scenarios
 */
object TestUtils {
    private const val TAG = "TestUtils"
    
    /**
     * Check if the device is connected to the internet
     * @param context Application context
     * @return true if connected, false otherwise
     */
    fun isNetworkConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnected ?: false
        }
    }
    
    /**
     * Check if the required location permissions are granted
     * @param context Application context
     * @return true if permissions are granted, false otherwise
     */
    fun hasLocationPermissions(context: Context): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        LogUtils.permission(TAG, "ACCESS_FINE_LOCATION", fineLocation)
        LogUtils.permission(TAG, "ACCESS_COARSE_LOCATION", coarseLocation)
        
        return fineLocation || coarseLocation
    }
    
    /**
     * Check if the required notification permissions are granted
     * @param context Application context
     * @return true if permissions are granted, false otherwise
     */
    fun hasNotificationPermissions(context: Context): Boolean {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Always true for Android < 13
        }
        
        LogUtils.permission(TAG, "POST_NOTIFICATIONS", hasPermission)
        return hasPermission
    }
    
    /**
     * Generate a mock location for testing
     * @param latitude Base latitude
     * @param longitude Base longitude
     * @param randomize Whether to add random offset for testing
     * @return Mock Location object
     */
    fun generateMockLocation(
        latitude: Double = 37.422160, // Google Plex default
        longitude: Double = -122.084270,
        randomize: Boolean = false
    ): Location {
        val location = Location("TestProvider")
        
        // Add slight randomization for testing
        val finalLatitude = if (randomize) {
            latitude + (Math.random() - 0.5) * 0.01
        } else {
            latitude
        }
        
        val finalLongitude = if (randomize) {
            longitude + (Math.random() - 0.5) * 0.01
        } else {
            longitude
        }
        
        location.latitude = finalLatitude
        location.longitude = finalLongitude
        location.accuracy = 10f
        location.time = System.currentTimeMillis()
        
        LogUtils.location(TAG, finalLatitude, finalLongitude, 10f)
        
        return location
    }
    
    /**
     * Generate a mock alert for testing
     * @param userId User ID
     * @param type Alert type
     * @return Mock Alert object
     */
    fun generateMockAlert(
        userId: String,
        type: String
    ): Alert {
        val location = generateMockLocation(randomize = true)
        
        return Alert(
            alertId = UUID.randomUUID().toString(),
            userId = userId,
            type = type,
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = Timestamp(Date()).toDate().time,
            resolved = false,
            offlineSynced = false
        )
    }
    
    /**
     * Generate a mock user for testing
     * @return Mock User object
     */
    fun generateMockUser(): User {
        val userId = UUID.randomUUID().toString()
        
        return User(
            userId = userId,
            email = "test$userId@example.com",
            displayName = "Test User",
            phoneNumber = "",
            profileCompleted = false,
            emergencyContacts = emptyList(),
            notificationsEnabled = true
        )
    }
    
    /**
     * Simulate operation with network delay
     * @param success Whether the operation should succeed
     * @param delayMs Delay in milliseconds
     * @return Result of type T
     */
    suspend fun <T> simulateNetworkOperation(
        result: T,
        success: Boolean = true,
        delayMs: Long = 1000
    ): Result<T> {
        delay(delayMs)
        
        return if (success) {
            Result.success(result)
        } else {
            Result.failure(Exception("Simulated network failure"))
        }
    }
    
    /**
     * Simulate repeated alert triggers and check rate limiting
     * @param alertType Type of alert
     * @param count Number of alerts to trigger
     * @param intervalMs Interval between alerts in milliseconds
     * @return List of results
     */
    suspend fun simulateRepeatedAlerts(
        alertType: String,
        count: Int,
        intervalMs: Long = 100
    ): List<Result<Alert>> {
        val results = mutableListOf<Result<Alert>>()
        val userId = "test-user-${UUID.randomUUID()}"
        
        repeat(count) { index ->
            val alert = generateMockAlert(userId, alertType)
            LogUtils.alert(TAG, alertType, true, "Simulated alert #$index")
            
            results.add(Result.success(alert))
            delay(intervalMs)
        }
        
        return results
    }
} 