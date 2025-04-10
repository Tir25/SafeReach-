package com.example.safereach.presentation.location

import android.location.Location

/**
 * Represents the different types of location errors
 */
enum class LocationErrorType {
    PERMISSION_DENIED,      // User denied location permissions
    GPS_DISABLED,           // GPS/location services are disabled
    LOCATION_UNAVAILABLE,   // Cannot get location from providers
    USING_LAST_KNOWN,       // Using fallback to last known location
    USING_EXISTING,         // Using existing location in emergency
    EMERGENCY_FAILED,       // Failed to get location in emergency
    UNKNOWN_ERROR           // Other errors
}

/**
 * Represents the UI state for location
 */
data class LocationState(
    val location: Location? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val errorType: LocationErrorType? = null,
    val permissionsGranted: Boolean = false,
    val gpsEnabled: Boolean = false,
    val lastLocationUpdateTime: Long? = null
) {
    val hasLocationData: Boolean
        get() = location != null
        
    val hasError: Boolean
        get() = error != null
    
    val isLocationRecent: Boolean
        get() {
            if (location == null || lastLocationUpdateTime == null) return false
            val age = System.currentTimeMillis() - lastLocationUpdateTime
            return age < 5 * 60 * 1000 // 5 minutes
        }
        
    /**
     * Returns true if there's a usable location (either recent or at least some location for emergency)
     */
    fun hasUsableLocation(isEmergency: Boolean = false): Boolean {
        return if (isEmergency) {
            // For emergency, any location is better than no location
            hasLocationData
        } else {
            // For normal use, location should be recent
            hasLocationData && isLocationRecent
        }
    }
}

/**
 * Extension function to get a String representation of the location
 */
fun Location.toLatLngString(): String {
    return "Lat: ${latitude.toFixed(6)}, Long: ${longitude.toFixed(6)}"
}

/**
 * Helper function to format a Double with a fixed number of decimal places
 */
fun Double.toFixed(decimals: Int): String {
    return "%.${decimals}f".format(this)
} 