package com.example.safereach.presentation.screens.home

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safereach.data.local.AlertLocalDataSource
import com.example.safereach.data.local.PreferencesManager
import com.example.safereach.domain.model.Alert
import com.example.safereach.domain.repository.AlertRepository
import com.example.safereach.presentation.location.LocationManager
import com.example.safereach.utils.Constants
import com.example.safereach.utils.ResultWrapper
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val alertRepository: AlertRepository,
    private val locationManager: LocationManager,
    private val preferencesManager: PreferencesManager,
    private val alertLocalDataSource: AlertLocalDataSource
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val _alertsState = MutableStateFlow<AlertsState>(AlertsState.Loading)
    val alertsState: StateFlow<AlertsState> = _alertsState.asStateFlow()
    
    private val _pendingOfflineAlerts = MutableStateFlow(0)
    val pendingOfflineAlerts: StateFlow<Int> = _pendingOfflineAlerts.asStateFlow()
    
    // UI states
    private val _nearbyAlerts = MutableStateFlow<List<Alert>>(emptyList())
    val nearbyAlerts: StateFlow<List<Alert>> = _nearbyAlerts
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    init {
        loadUserAlerts()
        countPendingOfflineAlerts()
        
        // Observe pending alerts flow
        viewModelScope.launch {
            alertLocalDataSource.getPendingAlertsFlow()
                .map { alerts: List<*> -> alerts.size }
                .collect { count: Int ->
                    _pendingOfflineAlerts.value = count
                }
        }

        // Start location updates and listen for nearby alerts
        viewModelScope.launch {
            locationManager.startLocationUpdates()
            
            // Get radius preference
            val radius = preferencesManager.nearbyAlertsRadiusFlow.first() ?: Constants.NEARBY_ALERTS_RADIUS_M
            
            // Observe location and update alerts when location changes
            locationManager.locationState.collectLatest { state ->
                if (state.hasLocationData && state.location != null) {
                    loadNearbyAlerts(state.location, radius)
                }
            }
        }
    }
    
    fun loadUserAlerts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val userId = auth.currentUser?.uid
            if (userId == null) {
                _error.value = "User not logged in"
                _isLoading.value = false
                return@launch
            }
            
            val result = alertRepository.getUserAlerts(userId)
            
            when (result) {
                is ResultWrapper.Success -> {
                    // Process user alerts - for now we just log them
                    Log.d(TAG, "Fetched ${result.data.size} user alerts")
                    _alertsState.value = AlertsState.Success(result.data)
                }
                is ResultWrapper.Error -> {
                    Log.e(TAG, "Failed to get user alerts", result.exception)
                    _error.value = "Failed to fetch your alerts: ${result.message}"
                    _alertsState.value = AlertsState.Error(result.message)
                }
                else -> {
                    // Do nothing for other states
                }
            }
            
            _isLoading.value = false
        }
    }
    
    private fun countPendingOfflineAlerts() {
        viewModelScope.launch {
            _pendingOfflineAlerts.value = alertLocalDataSource.countPendingAlerts()
        }
    }
    
    fun resolveAlert(alertId: String) {
        viewModelScope.launch {
            val result = alertRepository.resolveAlert(alertId)
            when (result) {
                is ResultWrapper.Success -> {
                    if (result.data) {
                        loadUserAlerts()
                    }
                }
                is ResultWrapper.Error -> {
                    // Could update the UI with an error message here
                    // For now, just log it
                    Log.e(TAG, "Failed to resolve alert: ${result.message}")
                }
                else -> {
                    // Ignore loading state for this operation
                }
            }
        }
    }

    private fun loadNearbyAlerts(location: Location, radiusInMeters: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // Get alerts using repository
                alertRepository.getNearbyAlerts(
                    location.latitude,
                    location.longitude,
                    radiusInMeters
                ).collectLatest { alerts ->
                    _nearbyAlerts.value = alerts
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading nearby alerts", e)
                _error.value = "Failed to load nearby alerts: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun refreshAlerts() {
        viewModelScope.launch {
            val location = locationManager.locationState.value.location ?: return@launch
            val radius = preferencesManager.nearbyAlertsRadiusFlow.first() ?: Constants.NEARBY_ALERTS_RADIUS_M
            loadNearbyAlerts(location, radius)
        }
    }
}

sealed class AlertsState {
    object Loading : AlertsState()
    data class Success(val alerts: List<Alert>) : AlertsState()
    data class Error(val message: String?) : AlertsState()
} 