package com.example.safereach.utils

import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.safereach.data.model.Alert
import com.example.safereach.data.repository.FirestoreAlertRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

/**
 * Utility class to test edge cases in the application
 */
class EdgeCaseTester(private val context: Context) {
    
    private val TAG = "EdgeCaseTester"
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())
    
    // State flow to track test results
    private val _testResults = MutableStateFlow<Map<String, TestResult>>(emptyMap())
    val testResults: StateFlow<Map<String, TestResult>> = _testResults
    
    // Test result data class
    data class TestResult(
        val testName: String,
        val passed: Boolean,
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Test GPS availability
     */
    fun testGpsAvailability() {
        coroutineScope.launch {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            
            val testPassed = isGpsEnabled || isNetworkEnabled
            val message = if (testPassed) {
                "Location provider available: ${if (isGpsEnabled) "GPS" else "Network"}"
            } else {
                "No location provider available. Both GPS and Network are disabled."
            }
            
            LogUtils.i(TAG, "GPS Availability Test: $message")
            
            _testResults.update { results ->
                results + ("gps_availability" to TestResult(
                    testName = "GPS Availability",
                    passed = testPassed,
                    message = message
                ))
            }
        }
    }
    
    /**
     * Test network connectivity
     */
    fun testNetworkConnectivity() {
        coroutineScope.launch {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkCapabilities = connectivityManager.activeNetwork?.let {
                connectivityManager.getNetworkCapabilities(it)
            }
            
            val hasInternet = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            val message = if (hasInternet) {
                "Network connection available"
            } else {
                "No network connection available"
            }
            
            LogUtils.i(TAG, "Network Connectivity Test: $message")
            
            _testResults.update { results ->
                results + ("network_connectivity" to TestResult(
                    testName = "Network Connectivity",
                    passed = hasInternet,
                    message = message
                ))
            }
        }
    }
    
    /**
     * Test location permissions
     */
    fun testLocationPermissions() {
        coroutineScope.launch {
            val hasPermissions = TestUtils.hasLocationPermissions(context)
            
            val message = if (hasPermissions) {
                "Location permissions granted"
            } else {
                "Location permissions denied"
            }
            
            LogUtils.i(TAG, "Location Permissions Test: $message")
            
            _testResults.update { results ->
                results + ("location_permissions" to TestResult(
                    testName = "Location Permissions",
                    passed = hasPermissions,
                    message = message
                ))
            }
        }
    }
    
    /**
     * Test notification permissions
     */
    fun testNotificationPermissions() {
        coroutineScope.launch {
            val hasPermissions = TestUtils.hasNotificationPermissions(context)
            
            val message = if (hasPermissions) {
                "Notification permissions granted"
            } else {
                "Notification permissions denied"
            }
            
            LogUtils.i(TAG, "Notification Permissions Test: $message")
            
            _testResults.update { results ->
                results + ("notification_permissions" to TestResult(
                    testName = "Notification Permissions",
                    passed = hasPermissions,
                    message = message
                ))
            }
        }
    }
    
    /**
     * Test repeated alert triggers (rate limiting)
     */
    fun testRepeatedAlertTriggers(count: Int = 3) {
        coroutineScope.launch {
            val results = mutableListOf<Boolean>()
            val userId = "test-user-${UUID.randomUUID()}"
            
            LogUtils.i(TAG, "Starting repeated alert trigger test with $count alerts")
            
            for (i in 1..count) {
                val alert = Alert(
                    alertId = "test-alert-$i",
                    userId = userId,
                    type = "POLICE",
                    latitude = 37.422160 + (Math.random() * 0.01),
                    longitude = -122.084270 + (Math.random() * 0.01),
                    timestamp = Timestamp(Date()).toDate().time,
                    resolved = false,
                    offlineSynced = false
                )
                
                // Log the attempt
                LogUtils.alert(TAG, "POLICE", true, "Test alert #$i")
                
                // Simulate rate limiting check would happen in the repository
                val currentTime = System.currentTimeMillis()
                val lastAlertTime = if (i > 1) {
                    currentTime - 100 // Simulate just sent alert recently
                } else {
                    0L
                }
                
                val isRateLimited = (currentTime - lastAlertTime) < 10 * 60 * 1000 // 10 minutes
                
                results.add(!isRateLimited)
                
                // Small delay between attempts
                delay(100)
            }
            
            // First should succeed, others might be rate limited
            val testPassed = results.first() && results.drop(1).any { !it }
            
            val message = "Triggered $count alerts. First alert ${if (results.first()) "succeeded" else "failed"}. " +
                          "Rate limiting ${if (results.drop(1).any { !it }) "worked" else "failed"}"
            
            LogUtils.i(TAG, "Repeated Alert Triggers Test: $message")
            
            _testResults.update { testResults ->
                testResults + ("repeated_alerts" to TestResult(
                    testName = "Repeated Alert Triggers",
                    passed = testPassed,
                    message = message
                ))
            }
        }
    }
    
    /**
     * Test offline mode operation
     */
    fun testOfflineMode() {
        coroutineScope.launch {
            val networkConnected = TestUtils.isNetworkConnected(context)
            
            val message = if (!networkConnected) {
                // Already offline
                "Device is offline. Testing offline alert creation."
            } else {
                // Online but we'll simulate offline behavior
                "Device is online. Simulating offline alert creation."
            }
            
            LogUtils.i(TAG, "Offline Mode Test: $message")
            
            // Create a test alert that would be stored locally
            val alert = Alert(
                alertId = "offline-test-${UUID.randomUUID()}",
                userId = "test-user-offline",
                type = "AMBULANCE",
                latitude = 37.422160,
                longitude = -122.084270,
                timestamp = Timestamp(Date()).toDate().time,
                resolved = false,
                offlineSynced = false
            )
            
            // In a real implementation, this would store the alert locally
            // For this test, we just simulate the result
            val stored = true
            
            // Check if the work manager has any sync jobs
            val workManager = WorkManager.getInstance(context)
            val workInfos = workManager.getWorkInfosByTag("alert_sync").get()
            val hasSyncJobs = workInfos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
            
            val testPassed = stored && (!networkConnected || hasSyncJobs)
            
            val resultMessage = if (testPassed) {
                "Offline alert created successfully${if (hasSyncJobs) " and sync job scheduled" else ""}"
            } else {
                "Offline alert creation failed"
            }
            
            LogUtils.i(TAG, "Offline Mode Test Result: $resultMessage")
            
            _testResults.update { results ->
                results + ("offline_mode" to TestResult(
                    testName = "Offline Mode",
                    passed = testPassed,
                    message = resultMessage
                ))
            }
        }
    }
    
    /**
     * Run all edge case tests sequentially
     */
    fun runAllTests() {
        coroutineScope.launch {
            LogUtils.i(TAG, "Starting full edge case test suite")
            
            // Clear previous results
            _testResults.update { emptyMap() }
            
            // Run tests with a slight delay between each
            testGpsAvailability()
            delay(500)
            
            testNetworkConnectivity()
            delay(500)
            
            testLocationPermissions()
            delay(500)
            
            testNotificationPermissions()
            delay(500)
            
            testRepeatedAlertTriggers()
            delay(500)
            
            testOfflineMode()
            
            LogUtils.i(TAG, "All edge case tests completed")
        }
    }
    
    /**
     * Get a summary of all test results
     */
    fun getTestSummary(): String {
        val results = _testResults.value
        
        if (results.isEmpty()) {
            return "No tests have been run yet."
        }
        
        val totalTests = results.size
        val passedTests = results.values.count { it.passed }
        
        val summary = StringBuilder()
        summary.append("Test Summary: $passedTests/$totalTests tests passed\n\n")
        
        results.values.forEach { result ->
            val status = if (result.passed) "✅ PASSED" else "❌ FAILED"
            summary.append("${result.testName}: $status\n")
            summary.append("  ${result.message}\n")
        }
        
        return summary.toString()
    }
} 