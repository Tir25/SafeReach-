package com.example.safereach.presentation.screens.emergency

import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.safereach.data.local.PreferencesManager
import com.example.safereach.data.model.Alert
import com.example.safereach.data.model.EmergencyAlertData
import com.example.safereach.data.model.EmergencyType
import com.example.safereach.data.service.FakeEmergencyService
import com.example.safereach.data.workers.EmergencyAlertWorker
import com.example.safereach.domain.repository.AlertRepository
import com.example.safereach.domain.repository.Result
import com.example.safereach.presentation.base.BaseViewModel
import com.example.safereach.presentation.base.UiState
import com.example.safereach.presentation.location.LocationManager
import com.example.safereach.presentation.location.toLatLngString
import com.example.safereach.utils.Constants
import com.example.safereach.utils.NotificationUtils
import com.example.safereach.utils.TimeUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import java.util.Date
import java.util.UUID
import com.example.safereach.domain.model.LatLng
import com.example.safereach.presentation.location.LocationErrorType

@HiltViewModel
class EmergencyTriggerViewModel @Inject constructor(
    private val locationManager: LocationManager,
    private val preferencesManager: PreferencesManager,
    private val alertRepository: AlertRepository,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager,
    private val fakeEmergencyService: FakeEmergencyService
) : BaseViewModel() {
    
    private val _alertState = MutableStateFlow<UiState<Alert>>(UiState.Loading)
    val alertState: StateFlow<UiState<Alert>> = _alertState.asStateFlow()
    
    private val _alertSent = MutableStateFlow(false)
    val alertSent: StateFlow<Boolean> = _alertSent.asStateFlow()
    
    private val _selectedEmergencyType = MutableStateFlow<EmergencyType?>(null)
    val selectedEmergencyType: StateFlow<EmergencyType?> = _selectedEmergencyType.asStateFlow()
    
    // New emergency UI state
    private val _emergencyUIState = MutableStateFlow<EmergencyUIState>(EmergencyUIState.Idle)
    val emergencyUIState: StateFlow<EmergencyUIState> = _emergencyUIState.asStateFlow()
    
    // Location state from the LocationManager
    val locationState = locationManager.locationState
    
    // Cooldown state
    private val _canTriggerEmergency = MutableStateFlow(preferencesManager.canTriggerEmergency())
    val canTriggerEmergency: StateFlow<Boolean> = _canTriggerEmergency
    
    private val _cooldownTimeRemaining = MutableStateFlow(preferencesManager.getRemainingCooldownTime())
    val cooldownTimeRemaining: StateFlow<Long> = _cooldownTimeRemaining
    
    // New UI states for emergency process
    private val _isContactingEmergency = MutableStateFlow(false)
    val isContactingEmergency: StateFlow<Boolean> = _isContactingEmergency
    
    private val _isSendingCommunityAlert = MutableStateFlow(false)
    val isSendingCommunityAlert: StateFlow<Boolean> = _isSendingCommunityAlert
    
    // Format cooldown time for display
    val cooldownTimeFormatted: StateFlow<String> = MutableStateFlow("").also { flow ->
        cooldownTimeRemaining.onEach { timeRemaining ->
            (flow as MutableStateFlow).value = TimeUtils.formatCooldownTime(timeRemaining)
        }.launchIn(viewModelScope)
    }
    
    val cooldownTimeText: StateFlow<String> = MutableStateFlow("").also { flow ->
        cooldownTimeRemaining.onEach { timeRemaining ->
            (flow as MutableStateFlow).value = TimeUtils.formatCooldownTimeText(timeRemaining)
        }.launchIn(viewModelScope)
    }
    
    private var cooldownTimer: CountDownTimer? = null
    
    companion object {
        private const val TAG = "EmergencyTriggerVM"
        private const val COOLDOWN_TIMER_UPDATE_INTERVAL = 1000L // 1 second
        private const val EMERGENCY_CONTACT_TIMEOUT = 10000L // 10 seconds timeout
        private const val EMERGENCY_GEOFENCE_RADIUS_KM = 5.0 // 5km radius for community alerts
    }
    
    init {
        // Initialize location updates
        locationManager.startLocationUpdates()
        
        // Monitor location updates
        locationManager.locationState
            .onEach { state ->
                Log.d(TAG, "Location state updated: hasLocation=${state.hasLocationData}, " +
                        "isLoading=${state.isLoading}, error=${state.error}")
            }
            .launchIn(viewModelScope)
        
        // Start cooldown timer if needed
        updateCooldownState()
    }
    
    private fun updateCooldownState() {
        _canTriggerEmergency.value = preferencesManager.canTriggerEmergency()
        _cooldownTimeRemaining.value = preferencesManager.getRemainingCooldownTime()
        
        if (!_canTriggerEmergency.value) {
            startCooldownTimer()
        }
    }
    
    private fun startCooldownTimer() {
        cooldownTimer?.cancel()
        
        cooldownTimer = object : CountDownTimer(Constants.ALERT_COOLDOWN_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _cooldownTimeRemaining.value = millisUntilFinished
            }
            
            override fun onFinish() {
                _canTriggerEmergency.value = true
                _cooldownTimeRemaining.value = 0
            }
        }.start()
    }
    
    /**
     * Trigger an emergency using the fake emergency service
     */
    fun triggerEmergency(emergencyType: EmergencyType) {
        viewModelScope.launch {
            if (!_canTriggerEmergency.value) {
                Log.d(TAG, "Cannot trigger emergency during cooldown period")
                return@launch
            }
            
            // Try to get emergency location - this uses our enhanced emergency location retrieval
            _emergencyUIState.value = EmergencyUIState.GettingLocation
            
            val locationState = locationManager.getEmergencyLocation()
            val location = locationState.location
            
            if (location == null) {
                // Handle location retrieval failure
                val errorMsg = locationState.error ?: "Unable to get your location"
                val actionMsg = when (locationState.errorType) {
                    LocationErrorType.PERMISSION_DENIED -> 
                        "Please grant location permissions to use emergency alerts."
                    LocationErrorType.GPS_DISABLED -> 
                        "Please enable location services to use emergency alerts."
                    else -> 
                        "Please ensure location services are enabled and permissions are granted."
                }
                
                Log.e(TAG, "Location error for emergency: $errorMsg (${locationState.errorType})")
                _emergencyUIState.value = EmergencyUIState.Error(
                    "$errorMsg $actionMsg"
                )
                return@launch
            }
            
            // Set emergency type
            _selectedEmergencyType.value = emergencyType
            
            // Update UI state to contacting emergency
            _emergencyUIState.value = EmergencyUIState.ContactingEmergency
            _isContactingEmergency.value = true
            
            try {
                Log.d(TAG, "Contacting emergency services for ${emergencyType.name} at location: ${location.latitude},${location.longitude}")
                // Use fake emergency service
                val emergencyContactSuccess = fakeEmergencyService.contactService(emergencyType)
                
                if (!emergencyContactSuccess) {
                    // If emergency contact failed, show error
                    Log.e(TAG, "Failed to contact emergency services")
                    _emergencyUIState.value = EmergencyUIState.Error(
                        "Failed to contact emergency services. Please try again.",
                        emergencyType
                    )
                    _isContactingEmergency.value = false
                    return@launch
                }
                
                // Update UI state to sending community alert
                _isContactingEmergency.value = false
                _isSendingCommunityAlert.value = true
                _emergencyUIState.value = EmergencyUIState.SendingCommunityAlert
                
                // Send community alert
                Log.d(TAG, "Sending community alert for ${emergencyType.name}")
                val communityAlertSuccess = fakeEmergencyService.sendCommunityAlert(
                    emergencyType,
                    location.latitude,
                    location.longitude
                )
                
                // Set cooldown
                preferencesManager.saveLastEmergencyTriggerTime(System.currentTimeMillis())
                _canTriggerEmergency.value = false
                _cooldownTimeRemaining.value = Constants.ALERT_COOLDOWN_MS
                startCooldownTimer()
                
                // Reset UI state
                _isSendingCommunityAlert.value = false
                _alertSent.value = true
                
                // Show success message
                _emergencyUIState.value = EmergencyUIState.Success(emergencyType)
                _alertState.value = UiState.Success(Alert(
                    id = "",
                    userId = auth.currentUser?.uid ?: "",
                    type = emergencyType.name,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = System.currentTimeMillis()
                ))
                
                // Also create a real alert in the repository for demonstration
                createRealAlert(emergencyType, location)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in emergency flow", e)
                _isContactingEmergency.value = false
                _isSendingCommunityAlert.value = false
                _emergencyUIState.value = EmergencyUIState.Error(
                    "Unexpected error: ${e.message}",
                    emergencyType
                )
                _alertState.value = UiState.Error("Failed to send emergency alert: ${e.message}")
            }
        }
    }
    
    /**
     * Create a real alert in the repository
     * This is just for demonstration purposes
     */
    private suspend fun createRealAlert(emergencyType: EmergencyType, location: Location) {
        try {
            val alert = Alert(
                id = UUID.randomUUID().toString(),
                userId = auth.currentUser?.uid ?: "",
                type = emergencyType.name,
                latitude = location.latitude,
                longitude = location.longitude,
                timestamp = System.currentTimeMillis()
            )
            
            // Create alert in repository
            val result = alertRepository.createAlert(
                com.example.safereach.domain.model.Alert(
                    id = alert.id,
                    userId = alert.userId,
                    alertType = alert.type,
                    location = LatLng(alert.latitude, alert.longitude),
                    timestamp = Date(alert.timestamp)
                )
            )
            
            Log.d(TAG, "Alert creation result: $result")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create alert in repository", e)
        }
    }

    /**
     * Legacy methods kept for compatibility
     */
    private suspend fun contactEmergencyService(emergencyType: EmergencyType, location: Location) {
        try {
            // Just delegate to the fake service now
            fakeEmergencyService.contactService(emergencyType)
        } catch (e: Exception) {
            Log.e(TAG, "Error contacting emergency service", e)
            throw e
        }
    }
    
    private suspend fun sendCommunityAlert(emergencyType: EmergencyType, location: Location) {
        try {
            // Delegate to fake service
            fakeEmergencyService.sendCommunityAlert(
                emergencyType, 
                location.latitude, 
                location.longitude
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error sending community alert", e)
            throw e
        }
    }
    
    fun resetState() {
        _alertSent.value = false
        _alertState.value = UiState.Loading
        _selectedEmergencyType.value = null
        _emergencyUIState.value = EmergencyUIState.Idle
    }
    
    /**
     * Request location permission updates
     */
    fun updateLocationPermission(granted: Boolean) {
        locationManager.updatePermissionStatus(granted)
    }
    
    /**
     * Force a location refresh for emergency
     */
    fun refreshEmergencyLocation() {
        viewModelScope.launch {
            _emergencyUIState.value = EmergencyUIState.GettingLocation
            val locationState = locationManager.getEmergencyLocation()
            
            if (locationState.location != null) {
                _emergencyUIState.value = EmergencyUIState.Idle
            } else {
                val errorMsg = locationState.error ?: "Unable to get your location"
                val actionMsg = when (locationState.errorType) {
                    LocationErrorType.PERMISSION_DENIED -> 
                        "Please grant location permissions to use emergency alerts."
                    LocationErrorType.GPS_DISABLED -> 
                        "Please enable location services to use emergency alerts."
                    else -> 
                        "Please ensure location services are enabled and permissions are granted."
                }
                
                _emergencyUIState.value = EmergencyUIState.Error(
                    "$errorMsg $actionMsg"
                )
            }
        }
    }
    
    /**
     * Legacy method - Maintains backward compatibility
     * Delegates to refreshEmergencyLocation()
     */
    fun refreshLocation() {
        refreshEmergencyLocation()
    }
    
    override fun onCleared() {
        super.onCleared()
        cooldownTimer?.cancel()
    }
} 