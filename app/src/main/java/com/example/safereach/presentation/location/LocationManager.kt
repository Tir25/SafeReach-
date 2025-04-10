package com.example.safereach.presentation.location

import android.content.Context
import android.location.Location
import android.util.Log
import com.example.safereach.data.repository.LocationRepository
import com.example.safereach.domain.repository.Result
import com.example.safereach.utils.PermissionUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationRepository: LocationRepository
) {
    private val _locationState = MutableStateFlow(LocationState(
        isLoading = true,
        permissionsGranted = PermissionUtils.hasPermissions(context, PermissionUtils.LOCATION_PERMISSIONS),
        gpsEnabled = PermissionUtils.isLocationEnabled(context)
    ))
    val locationState: StateFlow<LocationState> = _locationState
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    companion object {
        private const val TAG = "LocationManager"
    }
    
    init {
        if (_locationState.value.permissionsGranted) {
            startLocationUpdates()
        }
    }
    
    /**
     * Start location updates if permissions are granted and GPS is enabled
     */
    fun startLocationUpdates() {
        if (!PermissionUtils.hasPermissions(context, PermissionUtils.LOCATION_PERMISSIONS)) {
            _locationState.value = _locationState.value.copy(
                error = "Location permissions not granted",
                errorType = LocationErrorType.PERMISSION_DENIED,
                isLoading = false,
                permissionsGranted = false
            )
            return
        }
        
        if (!PermissionUtils.isLocationEnabled(context)) {
            _locationState.value = _locationState.value.copy(
                error = "GPS is disabled",
                errorType = LocationErrorType.GPS_DISABLED,
                isLoading = false,
                gpsEnabled = false
            )
            return
        }
        
        _locationState.value = _locationState.value.copy(
            isLoading = true,
            error = null,
            errorType = null,
            permissionsGranted = true,
            gpsEnabled = true
        )
        
        coroutineScope.launch {
            try {
                locationRepository.getLocationUpdates().onEach { result ->
                    when (result) {
                        is Result.Success -> {
                            val location = result.data
                            Log.d(TAG, "Location update: ${location.toLatLngString()}")
                            _locationState.value = _locationState.value.copy(
                                location = location,
                                isLoading = false,
                                error = null,
                                errorType = null,
                                lastLocationUpdateTime = System.currentTimeMillis()
                            )
                        }
                        is Result.Error -> {
                            Log.e(TAG, "Location error: ${result.message}")
                            
                            // Try to get last known location as fallback
                            val lastLocationResult = locationRepository.getLastKnownLocation()
                            if (lastLocationResult is Result.Success) {
                                _locationState.value = _locationState.value.copy(
                                    location = lastLocationResult.data,
                                    isLoading = false,
                                    error = "Using last known location: ${result.message}",
                                    errorType = LocationErrorType.USING_LAST_KNOWN,
                                    lastLocationUpdateTime = System.currentTimeMillis()
                                )
                            } else {
                                _locationState.value = _locationState.value.copy(
                                    isLoading = false,
                                    error = result.message,
                                    errorType = LocationErrorType.LOCATION_UNAVAILABLE
                                )
                            }
                        }
                        Result.Loading -> {
                            _locationState.value = _locationState.value.copy(
                                isLoading = true,
                                error = null,
                                errorType = null
                            )
                        }
                    }
                }.collect()
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting location updates", e)
                _locationState.value = _locationState.value.copy(
                    isLoading = false,
                    error = "Error collecting location updates: ${e.message}",
                    errorType = LocationErrorType.UNKNOWN_ERROR
                )
            }
        }
    }
    
    /**
     * Force a refresh of the location
     */
    fun refreshLocation() {
        val permissionsGranted = PermissionUtils.hasPermissions(context, PermissionUtils.LOCATION_PERMISSIONS)
        val gpsEnabled = PermissionUtils.isLocationEnabled(context)
        
        _locationState.value = _locationState.value.copy(
            isLoading = true,
            error = null,
            errorType = null,
            permissionsGranted = permissionsGranted,
            gpsEnabled = gpsEnabled
        )
        
        if (permissionsGranted && gpsEnabled) {
            startLocationUpdates()
        } else {
            if (!permissionsGranted) {
                _locationState.value = _locationState.value.copy(
                    error = "Location permissions not granted",
                    errorType = LocationErrorType.PERMISSION_DENIED,
                    isLoading = false
                )
            } else if (!gpsEnabled) {
                _locationState.value = _locationState.value.copy(
                    error = "GPS is disabled",
                    errorType = LocationErrorType.GPS_DISABLED,
                    isLoading = false
                )
            }
        }
    }
    
    /**
     * Get location for emergency situation - tries harder to get a location
     * even in difficult conditions
     */
    suspend fun getEmergencyLocation(): LocationState {
        Log.d(TAG, "Getting emergency location")
        
        // Update permission and GPS status
        val permissionsGranted = PermissionUtils.hasPermissions(context, PermissionUtils.LOCATION_PERMISSIONS)
        val gpsEnabled = PermissionUtils.isLocationEnabled(context)
        
        _locationState.value = _locationState.value.copy(
            permissionsGranted = permissionsGranted,
            gpsEnabled = gpsEnabled,
            isLoading = true,
            error = null,
            errorType = null
        )
        
        if (!permissionsGranted) {
            Log.e(TAG, "Cannot get emergency location: permissions not granted")
            return _locationState.value.copy(
                isLoading = false,
                error = "Location permissions not granted for emergency",
                errorType = LocationErrorType.PERMISSION_DENIED
            )
        }
        
        // Continue even if GPS is disabled, we'll try fallbacks
        if (!gpsEnabled) {
            Log.w(TAG, "GPS disabled during emergency location request, will try fallbacks")
        }
        
        try {
            val result = locationRepository.getEmergencyLocation()
            
            when (result) {
                is Result.Success -> {
                    val location = result.data
                    Log.d(TAG, "Got emergency location: ${location.toLatLngString()}")
                    
                    val newState = _locationState.value.copy(
                        location = location,
                        isLoading = false,
                        error = null,
                        errorType = null,
                        lastLocationUpdateTime = System.currentTimeMillis()
                    )
                    _locationState.value = newState
                    return newState
                }
                is Result.Error -> {
                    Log.e(TAG, "Failed to get emergency location: ${result.message}")
                    
                    // If we already have a location, use it regardless of age
                    val existingLocation = _locationState.value.location
                    if (existingLocation != null) {
                        Log.d(TAG, "Using existing location for emergency: ${existingLocation.toLatLngString()}")
                        
                        val newState = _locationState.value.copy(
                            isLoading = false,
                            error = "Using existing location for emergency: ${result.message}",
                            errorType = LocationErrorType.USING_EXISTING
                        )
                        _locationState.value = newState
                        return newState
                    }
                    
                    val newState = _locationState.value.copy(
                        isLoading = false,
                        error = "Failed to get location for emergency: ${result.message}",
                        errorType = LocationErrorType.EMERGENCY_FAILED
                    )
                    _locationState.value = newState
                    return newState
                }
                else -> {
                    // Should not reach here, but just in case
                    Log.w(TAG, "Unexpected result type from getEmergencyLocation")
                    val newState = _locationState.value.copy(
                        isLoading = false,
                        error = "Unexpected error getting emergency location",
                        errorType = LocationErrorType.UNKNOWN_ERROR
                    )
                    _locationState.value = newState
                    return newState
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting emergency location", e)
            
            val newState = _locationState.value.copy(
                isLoading = false,
                error = "Exception getting emergency location: ${e.message}",
                errorType = LocationErrorType.UNKNOWN_ERROR
            )
            _locationState.value = newState
            return newState
        }
    }
    
    /**
     * Update the permission status
     */
    fun updatePermissionStatus(granted: Boolean) {
        _locationState.value = _locationState.value.copy(
            permissionsGranted = granted,
            error = if (!granted) "Location permissions not granted" else _locationState.value.error,
            errorType = if (!granted) LocationErrorType.PERMISSION_DENIED else _locationState.value.errorType
        )
        
        if (granted) {
            startLocationUpdates()
        }
    }
    
    /**
     * Check if we have a usable location (not null and reasonably recent)
     */
    fun hasUsableLocation(): Boolean {
        val location = _locationState.value.location ?: return false
        val lastUpdateTime = _locationState.value.lastLocationUpdateTime ?: return false
        
        // Consider location usable if it's less than 5 minutes old
        val locationAge = System.currentTimeMillis() - lastUpdateTime
        return locationAge < 5 * 60 * 1000 // 5 minutes
    }
} 