package com.example.safereach.data.workers

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.safereach.data.model.EmergencyAlertData
import com.example.safereach.data.model.EmergencyType
import com.example.safereach.presentation.location.toLatLngString
import com.example.safereach.utils.NotificationUtils
import com.google.gson.Gson
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class EmergencyAlertWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "EmergencyAlertWorker"
        private const val KEY_ALERT_DATA = "alert_data"
        private const val UNIQUE_WORK_NAME = "emergency_alert_work"
        private const val MAX_RETRIES = 3
        private const val BACKOFF_DELAY_MINUTES = 5L
        
        /**
         * Create a work request for the emergency alert
         */
        fun createWorkRequest(alertData: EmergencyAlertData): androidx.work.OneTimeWorkRequest {
            val alertJson = Gson().toJson(alertData)
            val inputData = workDataOf(KEY_ALERT_DATA to alertJson)
            
            // Define constraints - need network for the alert to be sent
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            // Create the work request
            return OneTimeWorkRequestBuilder<EmergencyAlertWorker>()
                .setInputData(inputData)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.LINEAR,
                    BACKOFF_DELAY_MINUTES,
                    TimeUnit.MINUTES
                )
                .build()
        }
        
        /**
         * Enqueue a one-time work for emergency alert
         */
        fun enqueueWork(
            context: Context,
            alertData: EmergencyAlertData
        ) {
            // Create the work request
            val workRequest = createWorkRequest(alertData)
            
            // Enqueue the work
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    UNIQUE_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            
            Log.i(TAG, "Emergency alert work enqueued with type: ${alertData.emergencyType}")
        }
    }
    
    override suspend fun doWork(): Result {
        try {
            // Extract alert data from input
            val alertJson = inputData.getString(KEY_ALERT_DATA)
            if (alertJson.isNullOrEmpty()) {
                Log.e(TAG, "No alert data provided")
                return Result.failure()
            }
            
            // Parse alert data
            val alertData = Gson().fromJson(alertJson, EmergencyAlertData::class.java)
            
            // Create location string for the notification
            val location = android.location.Location("").apply {
                latitude = alertData.latitude
                longitude = alertData.longitude
            }
            val locationString = location.toLatLngString()
            
            // Get emergency type from string
            val emergencyType = try {
                EmergencyType.valueOf(alertData.emergencyType)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Invalid emergency type: ${alertData.emergencyType}")
                EmergencyType.POLICE // Default to police
            }
            
            // In a real app, this is where you would make a network call to your backend
            sendEmergencyAlert(alertData)
            
            // Show notification regardless of network success
            NotificationUtils.showEmergencyNotification(
                context = applicationContext,
                emergencyType = emergencyType,
                locationText = locationString
            )
            
            Log.i(TAG, "Emergency alert work completed: ${alertData.emergencyType} at $locationString")
            
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing emergency alert", e)
            
            // Determine if we should retry
            return if (shouldRetry(e, runAttemptCount)) {
                Log.i(TAG, "Retrying emergency alert, attempt: $runAttemptCount")
                Result.retry()
            } else {
                // If we can't retry, show a notification anyway
                val alertJson = inputData.getString(KEY_ALERT_DATA)
                if (!alertJson.isNullOrEmpty()) {
                    try {
                        val alertData = Gson().fromJson(alertJson, EmergencyAlertData::class.java)
                        val emergencyType = EmergencyType.valueOf(alertData.emergencyType)
                        NotificationUtils.showEmergencyNotification(
                            context = applicationContext,
                            emergencyType = emergencyType,
                            locationText = "Last known location (offline mode)"
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to show offline notification", e)
                    }
                }
                
                Result.failure()
            }
        }
    }
    
    /**
     * Determine if we should retry based on the exception and attempt count
     */
    private fun shouldRetry(e: Exception, attempts: Int): Boolean {
        // Retry for network-related issues
        val isNetworkIssue = e is UnknownHostException || e.cause is UnknownHostException
        return isNetworkIssue && attempts < MAX_RETRIES
    }
    
    /**
     * Send the emergency alert to a backend service
     * In a real app, this would make an API call to your backend
     */
    private suspend fun sendEmergencyAlert(alertData: EmergencyAlertData) {
        // Simulate network delay
        kotlinx.coroutines.delay(1000)
        
        // Simulate network failure based on a random chance for testing
        if (Math.random() < 0.2) {
            throw UnknownHostException("Simulated network failure")
        }
        
        // In a real app, you would make an API call to your backend here
        Log.d(TAG, "Emergency alert sent to backend: ${alertData.emergencyType}")
    }
} 