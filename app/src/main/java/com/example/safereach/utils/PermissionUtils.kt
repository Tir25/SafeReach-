package com.example.safereach.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.safereach.data.local.PermissionManager
import com.example.safereach.presentation.components.LocationPermissionsDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Utility class for handling permissions
 */
object PermissionUtils {
    // Location permissions required by the app
    val LOCATION_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
    
    // Background location permission (requested separately per Android guidelines)
    val BACKGROUND_LOCATION_PERMISSION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else {
        emptyArray()
    }

    // Notification permissions required for Android 13+
    val NOTIFICATION_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        emptyArray()
    }
    
    // Observable permission states
    private val _locationPermissionGranted = MutableStateFlow(false)
    val locationPermissionGranted = _locationPermissionGranted.asStateFlow()
    
    private val _notificationPermissionGranted = MutableStateFlow(false)
    val notificationPermissionGranted = _notificationPermissionGranted.asStateFlow()
    
    /**
     * Generic function to check if permissions are granted
     */
    fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Check if rationale should be shown for permissions
     */
    fun shouldShowRationale(activity: Activity, permissions: Array<String>): Boolean {
        return permissions.any {
            ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }
    }
    
    /**
     * Check if all location permissions are granted
     */
    fun hasLocationPermissions(context: Context): Boolean {
        val allGranted = LOCATION_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        _locationPermissionGranted.value = allGranted
        return allGranted
    }
    
    /**
     * Check if notification permissions are granted
     */
    fun hasNotificationPermissions(context: Context): Boolean {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Automatically granted before Android 13
        }
        _notificationPermissionGranted.value = granted
        return granted
    }
    
    /**
     * Check if precise location permission is granted
     */
    fun hasPreciseLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if background location permission is granted (Android 10+)
     */
    fun hasBackgroundLocationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required before Android 10
        }
    }
    
    /**
     * Open application settings screen
     */
    fun openAppSettings(context: Context) {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).also { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
    
    /**
     * Open location settings screen
     */
    fun openLocationSettings(context: Context) {
        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).also { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
    
    /**
     * Check if location services are enabled
     */
    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    
    /**
     * Advanced composable for handling location permissions with proper state persistence
     */
    @Composable
    fun RequestLocationPermission(
        permissionManager: PermissionManager,
        snackbarHostState: SnackbarHostState? = null,
        onPermissionResult: (Boolean) -> Unit
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        
        // Use rememberSaveable to persist across configuration changes
        var permissionRequested by rememberSaveable { mutableStateOf(false) }
        var showRationale by rememberSaveable { mutableStateOf(false) }
        var showSettings by rememberSaveable { mutableStateOf(false) }
        
        // Permission launcher
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            // Check all main location permissions
            val allGranted = LOCATION_PERMISSIONS.all { 
                permissions[it] == true 
            }
            
            if (allGranted) {
                // Success - permissions granted
                scope.launch {
                    permissionManager.markPermissionsAsRequested(context)
                    permissionManager.markPermissionPermanentlyDenied(context, false)
                    onPermissionResult(true)
                }
                
                Toast.makeText(
                    context, 
                    "Location permission granted", 
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Determine if we should show rationale or settings
                val activity = context as? Activity
                val canShowRationale = activity != null && shouldShowRationale(
                    activity, LOCATION_PERMISSIONS
                )
                
                if (canShowRationale) {
                    // Show rationale dialog
                    showRationale = true
                    permissionManager.updateShouldShowRationale(true)
                } else if (permissionRequested) {
                    // Likely permanently denied - need to go to settings
                    scope.launch {
                        permissionManager.markPermissionPermanentlyDenied(context, true)
                        showSettings = true
                        
                        // Show snackbar if available
                        snackbarHostState?.showSnackbar(
                            "Location permission denied. Please enable in app settings."
                        )
                    }
                }
                
                onPermissionResult(false)
            }
            
            // Mark as requested
            permissionRequested = true
        }
        
        // Only request permission if not already granted
        LaunchedEffect(Unit) {
            if (!hasLocationPermissions(context) && !permissionRequested) {
                // Check if we've already requested it before
                val alreadyRequested = permissionManager.hasAlreadyRequestedPermissions(context)
                val permanentlyDenied = permissionManager.isPermissionPermanentlyDenied(context)
                
                when {
                    permanentlyDenied -> {
                        // User has previously denied and checked "Don't ask again"
                        showSettings = true
                    }
                    alreadyRequested -> {
                        // We've asked before, but user hasn't checked "Don't ask again"
                        val activity = context as? Activity
                        if (activity != null && shouldShowRationale(activity, LOCATION_PERMISSIONS)) {
                            showRationale = true
                        } else {
                            // Request again after a reasonable delay
                            permissionLauncher.launch(LOCATION_PERMISSIONS)
                            permissionRequested = true
                        }
                    }
                    else -> {
                        // First time asking
                        permissionLauncher.launch(LOCATION_PERMISSIONS)
                        permissionRequested = true
                    }
                }
            } else {
                onPermissionResult(hasLocationPermissions(context))
            }
        }
        
        // Handle showing rationale dialog if needed
        if (showRationale) {
            LocationPermissionsDialog(
                onDismiss = { 
                    showRationale = false 
                    onPermissionResult(false)
                },
                onRequestPermission = {
                    showRationale = false
                    permissionLauncher.launch(LOCATION_PERMISSIONS)
                },
                rationale = true
            )
        }
        
        // Handle showing settings dialog if needed
        if (showSettings) {
            LocationPermissionsDialog(
                onDismiss = { 
                    showSettings = false 
                    onPermissionResult(false)
                },
                onRequestPermission = {
                    showSettings = false
                    openAppSettings(context)
                },
                permanentDenial = true
            )
        }
    }
    
    /**
     * Composable function to request notification permission
     */
    @Composable
    fun RequestNotificationPermission(
        permissionManager: PermissionManager,
        onResult: (Boolean) -> Unit
    ) {
        val context = LocalContext.current
        var permissionRequested by rememberSaveable { mutableStateOf(false) }
        
        // Check if permission is even needed on this Android version
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        
        // If permission not needed, auto-success
        if (!needsPermission) {
            LaunchedEffect(Unit) {
                onResult(true)
            }
            return
        }
        
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            onResult(granted)
            permissionRequested = true
            
            if (granted) {
                _notificationPermissionGranted.value = true
            }
        }
        
        LaunchedEffect(Unit) {
            if (!hasNotificationPermissions(context) && !permissionRequested) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onResult(hasNotificationPermissions(context))
            }
        }
    }
} 