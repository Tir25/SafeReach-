package com.example.safereach.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safereach.data.local.PermissionManager
import com.example.safereach.utils.PermissionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for centrally managing permission states across the app
 */
@HiltViewModel
class PermissionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionManager: PermissionManager
) : ViewModel() {
    
    // Location permission state
    private val _locationPermissionState = MutableStateFlow(
        PermissionUtils.hasLocationPermissions(context)
    )
    val locationPermissionState: StateFlow<Boolean> = _locationPermissionState.asStateFlow()
    
    // Notification permission state
    private val _notificationPermissionState = MutableStateFlow(
        PermissionUtils.hasNotificationPermissions(context)
    )
    val notificationPermissionState: StateFlow<Boolean> = _notificationPermissionState.asStateFlow()
    
    // Permission states from PermissionManager
    val shouldShowRationale = permissionManager.shouldShowPermissionRationale
    val permissionPermanentlyDenied = permissionManager.permissionPermanentlyDenied
    
    init {
        // Check for persisted permission state
        viewModelScope.launch {
            val alreadyRequested = permissionManager.hasAlreadyRequestedPermissions(context)
            val permanentlyDenied = permissionManager.isPermissionPermanentlyDenied(context)
            
            // Log permission state for debugging
            android.util.Log.d(
                "PermissionViewModel", 
                "Permissions: already requested = $alreadyRequested, " +
                "permanently denied = $permanentlyDenied"
            )
        }
    }
    
    /**
     * Update location permission state
     */
    fun updateLocationPermission(granted: Boolean) {
        _locationPermissionState.value = granted
        
        // If permission is granted, reset permanent denial state
        if (granted) {
            viewModelScope.launch {
                permissionManager.markPermissionPermanentlyDenied(context, false)
                permissionManager.markPermissionsAsRequested(context)
            }
        }
    }
    
    /**
     * Update notification permission state
     */
    fun updateNotificationPermission(granted: Boolean) {
        _notificationPermissionState.value = granted
    }
    
    /**
     * Reset permission state (e.g., after user manually grants from settings)
     */
    fun resetPermissionState() {
        viewModelScope.launch {
            permissionManager.resetPermissionState(context)
            
            // Re-check current state
            _locationPermissionState.value = PermissionUtils.hasLocationPermissions(context)
            _notificationPermissionState.value = PermissionUtils.hasNotificationPermissions(context)
        }
    }
    
    /**
     * Check permission state and update flows
     */
    fun refreshPermissionState() {
        _locationPermissionState.value = PermissionUtils.hasLocationPermissions(context)
        _notificationPermissionState.value = PermissionUtils.hasNotificationPermissions(context)
    }
} 