package com.example.safereach

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.safereach.data.service.AlertNotificationService
import com.example.safereach.data.workers.AlertSyncWorker
import com.example.safereach.utils.NotificationUtils
import com.example.safereach.utils.LogUtils
import com.example.safereach.utils.PermissionUtils
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SafeReachApp : Application(), androidx.work.Configuration.Provider {

    companion object {
        private const val TAG = "SafeReachApp"
    }
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    override fun onCreate() {
        super.onCreate()
        LogUtils.i(TAG, "SafeReach application starting")
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        LogUtils.i(TAG, "Firebase initialized")
        
        // Initialize Firebase App Check
        initializeAppCheck()
        
        // Create notification channels (no-op on Android < O)
        NotificationUtils.createNotificationChannels(this)
        
        // Schedule periodic alert sync
        AlertSyncWorker.schedulePeriodicSync(this)
        
        // Start the alert notification service if permissions are granted
        if (PermissionUtils.hasLocationPermissions(this)) {
            startAlertNotificationService()
        } else {
            LogUtils.w(TAG, "Location permissions not granted, alert service not started")
        }
    }
    
    private fun startAlertNotificationService() {
        // Only start the service if we have location permissions
        if (!PermissionUtils.hasLocationPermissions(this)) {
            LogUtils.w(TAG, "Cannot start alert service: location permissions not granted")
            return
        }

        LogUtils.i(TAG, "Starting alert notification service")
        val serviceIntent = Intent(this, AlertNotificationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    /**
     * Initializes Firebase App Check for enhanced security
     * Debug provider is used for debug builds, Play Integrity for release
     */
    private fun initializeAppCheck() {
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        
        if (BuildConfig.DEBUG) {
            // Debug build: Use debug provider
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            LogUtils.w(TAG, "Using DEBUG App Check Provider - NOT SECURE FOR PRODUCTION")
        } else {
            // Release build: Use Play Integrity provider
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
            LogUtils.i(TAG, "Using PlayIntegrity App Check Provider")
        }
    }

    override val workManagerConfiguration: Configuration
        get() {
            LogUtils.d(TAG, "Configuring WorkManager")
            return Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.INFO)
                .build()
        }
} 