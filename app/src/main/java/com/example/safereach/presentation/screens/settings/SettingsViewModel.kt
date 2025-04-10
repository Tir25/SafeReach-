package com.example.safereach.presentation.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safereach.data.local.PreferencesManager
import com.example.safereach.utils.PermissionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Settings screen
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Load permission states
    private val _locationPermissionState = MutableStateFlow(
        PermissionUtils.hasLocationPermissions(context)
    )
    val locationPermissionState = _locationPermissionState.asStateFlow()
    
    private val _notificationPermissionState = MutableStateFlow(
        PermissionUtils.hasNotificationPermissions(context)
    )
    val notificationPermissionState = _notificationPermissionState.asStateFlow()
    
    private val _preciseLocationState = MutableStateFlow(
        PermissionUtils.hasPreciseLocationPermission(context)
    )
    val preciseLocationState = _preciseLocationState.asStateFlow()
    
    private val _backgroundLocationState = MutableStateFlow(
        PermissionUtils.hasBackgroundLocationPermission(context)
    )
    val backgroundLocationState = _backgroundLocationState.asStateFlow()
    
    private val _locationServicesState = MutableStateFlow(
        PermissionUtils.isLocationEnabled(context)
    )
    val locationServicesState = _locationServicesState.asStateFlow()

    // Add this property after other StateFlow properties
    val nearbyAlertsEnabled = preferencesManager.nearbyAlertsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    // Combine all settings into a single state for the UI
    val settingsState: StateFlow<SettingsState> = combine(
        preferencesManager.autoLocationSharingFlow,
        preferencesManager.offlineFallbackFlow,
        preferencesManager.notificationSoundsFlow,
        _locationPermissionState,
        _notificationPermissionState,
        _locationServicesState
    ) { args: Array<Any> ->
        val autoLocation = args[0] as Boolean
        val offlineFallback = args[1] as Boolean
        val notificationSounds = args[2] as Boolean
        val locationPermission = args[3] as Boolean
        val notificationPermission = args[4] as Boolean
        val locationServices = args[5] as Boolean
        
        SettingsState(
            autoLocationSharing = autoLocation,
            offlineFallback = offlineFallback,
            notificationSounds = notificationSounds,
            locationPermissionGranted = locationPermission,
            notificationPermissionGranted = notificationPermission,
            locationServicesEnabled = locationServices
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsState()
    )

    /**
     * Update auto location sharing preference
     */
    fun updateAutoLocationSharing(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateAutoLocationSharing(enabled)
        }
    }

    /**
     * Update offline fallback preference
     */
    fun updateOfflineFallback(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateOfflineFallback(enabled)
        }
    }

    /**
     * Update notification sounds preference
     */
    fun updateNotificationSounds(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateNotificationSounds(enabled)
        }
    }

    /**
     * Update nearby alerts preference
     */
    fun updateNearbyAlerts(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateNearbyAlerts(enabled)
        }
    }

    /**
     * Refresh permission states
     */
    fun refreshPermissionStates() {
        _locationPermissionState.value = PermissionUtils.hasLocationPermissions(context)
        _notificationPermissionState.value = PermissionUtils.hasNotificationPermissions(context)
        _preciseLocationState.value = PermissionUtils.hasPreciseLocationPermission(context)
        _backgroundLocationState.value = PermissionUtils.hasBackgroundLocationPermission(context)
        _locationServicesState.value = PermissionUtils.isLocationEnabled(context)
    }
    
    /**
     * Open app settings
     */
    fun openAppSettings() {
        PermissionUtils.openAppSettings(context)
    }
    
    /**
     * Open location settings
     */
    fun openLocationSettings() {
        PermissionUtils.openLocationSettings(context)
    }
}

/**
 * Data class representing the settings state
 */
data class SettingsState(
    val autoLocationSharing: Boolean = true,
    val offlineFallback: Boolean = true,
    val notificationSounds: Boolean = true,
    val locationPermissionGranted: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    val locationServicesEnabled: Boolean = false
) 