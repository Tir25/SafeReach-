package com.example.safereach.data.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.safereach.domain.model.Alert
import com.example.safereach.domain.model.LatLng
import com.example.safereach.domain.repository.AlertRepository
import com.example.safereach.utils.Constants
import com.example.safereach.utils.ResultWrapper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Worker responsible for syncing locally saved alerts with the backend
 */
@HiltWorker
class AlertSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val alertRepository: AlertRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "AlertSyncWorker"
        
        private const val SYNC_WORK_NAME = "alert_sync_work"
        private const val SYNC_PERIODIC_WORK_NAME = "alert_sync_periodic_work"
        
        /**
         * Schedule a one-time sync
         */
        fun startOneTimeSync(context: Context) {
            val syncWork = OneTimeWorkRequestBuilder<AlertSyncWorker>()
                .addTag(Constants.ALERT_SYNC_WORKER)
                .build()
                
            WorkManager.getInstance(context).enqueueUniqueWork(
                SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncWork
            )
        }
        
        /**
         * Schedule periodic sync
         */
        fun schedulePeriodicSync(context: Context) {
            val periodicSyncRequest = PeriodicWorkRequestBuilder<AlertSyncWorker>(
                Constants.SYNC_INTERVAL_HOURS, TimeUnit.HOURS
            )
                .addTag(Constants.ALERT_SYNC_WORKER)
                .build()
                
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SYNC_PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicSyncRequest
            )
        }
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting alert sync worker")
        
        // Check if there are pending alerts to sync
        val hasPendingAlertsResult = alertRepository.hasPendingLocalAlerts()
        
        if (hasPendingAlertsResult is ResultWrapper.Success && !hasPendingAlertsResult.data) {
            Log.d(TAG, "No pending alerts to sync")
            return Result.success()
        }
        
        // Get unsynced alerts
        val unsyncedAlertsResult = alertRepository.getLocalUnsynedAlerts()
        
        if (unsyncedAlertsResult is ResultWrapper.Error) {
            Log.e(TAG, "Failed to get unsynced alerts: ${unsyncedAlertsResult.message}")
            return Result.retry()
        }
        
        if (unsyncedAlertsResult !is ResultWrapper.Success) {
            Log.d(TAG, "No unsynced alerts to sync")
            return Result.success()
        }
        
        val unsyncedAlerts = unsyncedAlertsResult.data
        if (unsyncedAlerts.isEmpty()) {
            Log.d(TAG, "No unsynced alerts to sync")
            return Result.success()
        }
        
        Log.d(TAG, "Found ${unsyncedAlerts.size} alerts to sync")
        
        // Sync each alert
        var syncFailed = false
        for (alert in unsyncedAlerts) {
            val result = alertRepository.createAlert(alert)
            
            if (result is ResultWrapper.Success) {
                Log.d(TAG, "Successfully synced alert ${alert.id}")
                // Mark as synced by syncing the local alert
                alertRepository.syncLocalAlert(alert.id)
            } else {
                Log.e(TAG, "Failed to sync alert ${alert.id}")
                syncFailed = true
            }
        }
        
        return if (syncFailed) {
            Result.retry()
        } else {
            Result.success()
        }
    }
} 