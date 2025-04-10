package com.example.safereach.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import com.example.safereach.domain.repository.Result
import com.example.safereach.utils.Constants
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }
    
    private val _locationState = MutableStateFlow<Result<Location>>(Result.Loading)
    val locationState: StateFlow<Result<Location>> = _locationState
    
    companion object {
        private const val TAG = "LocationRepository"
        private const val LOCATION_TIMEOUT_MS = 5000L // Reduced from 10 seconds to 5 seconds for faster emergency response
        private const val LOCATION_ACCURACY_THRESHOLD = 100f // Acceptable accuracy threshold in meters
    }
    
    /**
     * Checks if location permissions are granted
     */
    fun hasLocationPermissions(): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Checks if GPS is enabled
     */
    fun isGpsEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
               locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    
    /**
     * Gets the current location as a Flow. Will emit updates as they come in.
     * Falls back to last known location if GPS is unavailable.
     */
    @SuppressLint("MissingPermission")
    fun getLocationUpdates(): Flow<Result<Location>> = callbackFlow {
        if (!hasLocationPermissions()) {
            trySend(Result.Error("Location permissions not granted"))
            awaitClose()
            return@callbackFlow
        }
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            Constants.LOCATION_UPDATE_INTERVAL
        ).apply {
            setMinUpdateIntervalMillis(Constants.LOCATION_FASTEST_INTERVAL)
            setWaitForAccurateLocation(false)
        }.build()
        
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    Log.d(TAG, "Location update received: $location")
                    _locationState.value = Result.Success(location)
                    trySend(Result.Success(location))
                }
            }
        }
        
        try {
            // Request location updates
            Log.d(TAG, "Requesting location updates")
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            
            // While waiting for location updates, try to get the last known location
            if (_locationState.value is Result.Loading) {
                getLastKnownLocation()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting location updates", e)
            trySend(Result.Error("Failed to request location updates: ${e.message}"))
        }
        
        awaitClose {
            Log.d(TAG, "Removing location updates")
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }.catch { e ->
        Log.e(TAG, "Error in location flow", e)
        emit(Result.Error("Error getting location updates: ${e.message}"))
    }
    
    /**
     * Gets the last known location. Used as a fallback when GPS is unavailable.
     */
    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): Result<Location> {
        if (!hasLocationPermissions()) {
            return Result.Error("Location permissions not granted")
        }
        
        return try {
            Log.d(TAG, "Getting last known location")
            val location = fusedLocationClient.lastLocation.await()
            if (location != null) {
                Log.d(TAG, "Last known location: $location")
                _locationState.value = Result.Success(location)
                Result.Success(location)
            } else {
                Log.d(TAG, "Last known location is null")
                Result.Error("Last known location not available")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting last known location", e)
            Result.Error("Failed to get last known location: ${e.message}")
        }
    }
    
    /**
     * Emergency location getter - tries multiple methods to get location quickly
     * with fallbacks for emergency situations
     * 
     * @return A Result containing either a Location or an error
     */
    @SuppressLint("MissingPermission")
    suspend fun getEmergencyLocation(): Result<Location> {
        if (!hasLocationPermissions()) {
            Log.e(TAG, "Emergency location retrieval failed: Missing location permissions")
            return Result.Error("Location permissions not granted")
        }
        
        // Check GPS status but continue with fallbacks regardless
        val gpsEnabled = isGpsEnabled()
        if (!gpsEnabled) {
            Log.w(TAG, "GPS is disabled, will attempt fallbacks for emergency location")
        }

        // Strategy 1: Try to get a fresh location with a shorter timeout for emergencies
        try {
            Log.d(TAG, "Getting current location for emergency with timeout: $LOCATION_TIMEOUT_MS ms")
            val currentLocation = getCurrentLocationWithTimeout(LOCATION_TIMEOUT_MS)
            if (currentLocation != null) {
                Log.d(TAG, "Got current location for emergency: $currentLocation (accuracy: ${currentLocation.accuracy}m)")
                _locationState.value = Result.Success(currentLocation)
                return Result.Success(currentLocation)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current location for emergency", e)
            // Continue to fallback strategies
        }
        
        // Strategy 2: Check if we already have a recent location in state
        val stateLocation = (_locationState.value as? Result.Success)?.data
        if (stateLocation != null) {
            val locationAge = System.currentTimeMillis() - stateLocation.time
            // Accept locations up to 15 minutes old for emergencies (increased from 5 minutes)
            if (locationAge < 15 * 60 * 1000) { 
                Log.d(TAG, "Using recent location from state: $stateLocation (age: ${locationAge/1000}s)")
                return Result.Success(stateLocation)
            } else {
                Log.w(TAG, "State location too old: ${locationAge/1000}s old")
            }
        }

        // Strategy 3: Fall back to last known location regardless of age
        try {
            Log.d(TAG, "Attempting to get last known location for emergency")
            val lastLocationResult = getLastKnownLocation()
            if (lastLocationResult is Result.Success) {
                val location = lastLocationResult.data
                Log.d(TAG, "Using last known location for emergency: $location")
                return lastLocationResult
            } else {
                Log.w(TAG, "Last known location unavailable: ${(lastLocationResult as? Result.Error)?.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting last known location", e)
        }
        
        // Strategy 4: Try to get at least a coarse location with network provider
        try {
            Log.d(TAG, "Attempting to get coarse network location")
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (networkLocation != null) {
                    Log.d(TAG, "Using network provider location: $networkLocation")
                    return Result.Success(networkLocation)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting network location", e)
        }
        
        // Last resort: Create a mock location for emulator testing or when all else fails
        if (isEmulator()) {
            Log.d(TAG, "Running on emulator, creating mock location")
            val mockLocation = Location("mock").apply {
                latitude = 37.7749 // San Francisco
                longitude = -122.4194
                accuracy = 10f
                time = System.currentTimeMillis()
            }
            return Result.Success(mockLocation)
        }
        
        Log.e(TAG, "All location retrieval strategies failed for emergency")
        return Result.Error("Could not get location for emergency. Please ensure location services are enabled and permissions are granted.")
    }
    
    /**
     * Gets the current location with a specified timeout.
     * This is useful for emergency situations where we need a location quickly.
     */
    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocationWithTimeout(timeoutMs: Long): Location? {
        if (!hasLocationPermissions()) return null
        
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                val locationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    1000 // 1 second interval (reduced from 5 seconds)
                ).apply {
                    setMaxUpdates(2) // Try for 2 updates instead of 1
                    setMinUpdateDistanceMeters(0f) // Any distance change
                    setWaitForAccurateLocation(false) // Don't wait for high accuracy in emergency
                }.build()
                
                val locationCallback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        result.lastLocation?.let { location ->
                            Log.d(TAG, "Emergency location update: ${location.latitude},${location.longitude} (accuracy: ${location.accuracy}m)")
                            
                            // Accept first location update immediately for emergency
                            if (continuation.isActive) {
                                fusedLocationClient.removeLocationUpdates(this)
                                continuation.resume(location)
                            }
                        }
                    }
                }
                
                try {
                    Log.d(TAG, "Requesting emergency location updates")
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                    )
                    
                    continuation.invokeOnCancellation {
                        fusedLocationClient.removeLocationUpdates(locationCallback)
                        Log.d(TAG, "Emergency location request canceled or timed out")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error requesting emergency location updates", e)
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }
        }
    }
    
    /**
     * Detect if running on an emulator for testing purposes
     */
    private fun isEmulator(): Boolean {
        return android.os.Build.FINGERPRINT.startsWith("generic") ||
                android.os.Build.FINGERPRINT.startsWith("unknown") ||
                android.os.Build.MODEL.contains("google_sdk") ||
                android.os.Build.MODEL.contains("Emulator") ||
                android.os.Build.MODEL.contains("Android SDK built for x86") ||
                android.os.Build.MANUFACTURER.contains("Genymotion") ||
                (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic")) ||
                "google_sdk" == android.os.Build.PRODUCT
    }
} 