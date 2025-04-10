package com.example.safereach.utils

import android.location.Location
import java.text.DecimalFormat

/**
 * Extension functions for Location class
 */

/**
 * Convert location to a readable string format (e.g. "37.7749°N, 122.4194°W")
 */
fun Location.toLatLngString(): String {
    val df = DecimalFormat("0.####")
    
    val latDirection = if (latitude >= 0) "N" else "S"
    val longDirection = if (longitude >= 0) "E" else "W"
    
    val latAbs = Math.abs(latitude)
    val longAbs = Math.abs(longitude)
    
    return "${df.format(latAbs)}°$latDirection, ${df.format(longAbs)}°$longDirection"
} 