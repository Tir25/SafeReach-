package com.example.safereach.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import com.example.safereach.data.model.EmergencyType
import com.example.safereach.domain.model.Alert
import com.example.safereach.domain.model.LatLng
import com.example.safereach.domain.repository.AlertRepository
import com.example.safereach.utils.LogUtils
import com.example.safereach.utils.ResultWrapper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Date
import java.util.UUID

/**
 * Data class to represent emergency alert data for WorkManager
 */
data class EmergencyAlertData(
    val emergencyType: String,
    val latitude: Double,
    val longitude: Double,
    val userId: String
)

/**
 * Worker responsible for sending emergency alerts to the backend
 * Uses Hilt dependency injection
 */
@HiltWorker
class EmergencyAlertWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val alertRepository: AlertRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "EmergencyAlertWorker"
        
        // Input data keys
        private const val KEY_EMERGENCY_TYPE = "emergency_type"
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
        private const val KEY_USER_ID = "user_id"
        
        /**
         * Creates a work request for sending an emergency alert
         */
        fun createWorkRequest(alertData: EmergencyAlertData) = OneTimeWorkRequestBuilder<EmergencyAlertWorker>()
            .setInputData(
                Data.Builder()
                    .putString(KEY_EMERGENCY_TYPE, alertData.emergencyType)
                    .putDouble(KEY_LATITUDE, alertData.latitude)
                    .putDouble(KEY_LONGITUDE, alertData.longitude)
                    .putString(KEY_USER_ID, alertData.userId)
                    .build()
            )
            .build()
    }
    
    override suspend fun doWork(): Result {
        LogUtils.d(TAG, "Starting emergency alert worker")
        
        try {
            // Extract input data
            val emergencyType = inputData.getString(KEY_EMERGENCY_TYPE) ?: return Result.failure()
            val latitude = inputData.getDouble(KEY_LATITUDE, 0.0)
            val longitude = inputData.getDouble(KEY_LONGITUDE, 0.0)
            val userId = inputData.getString(KEY_USER_ID) ?: return Result.failure()
            
            if (latitude == 0.0 && longitude == 0.0) {
                LogUtils.e(TAG, "Invalid location coordinates")
                return Result.failure()
            }
            
            // Create alert object
            val alert = Alert(
                id = UUID.randomUUID().toString(),
                userId = userId,
                alertType = emergencyType,
                location = LatLng(latitude, longitude),
                timestamp = Date()
            )
            
            // Send alert to repository
            val result = alertRepository.createAlert(alert)
            
            // Check result type and handle appropriately
            return when (result) {
                is ResultWrapper.Success -> {
                    LogUtils.d(TAG, "Successfully sent emergency alert: $emergencyType")
                    Result.success()
                }
                is ResultWrapper.Error -> {
                    LogUtils.e(TAG, "Failed to send emergency alert: ${result.message}")
                    Result.retry()
                }
                else -> {
                    LogUtils.d(TAG, "Pending alert creation")
                    Result.retry()
                }
            }
            
        } catch (e: Exception) {
            LogUtils.e(TAG, "Error sending emergency alert", e)
            return Result.failure()
        }
    }
} 