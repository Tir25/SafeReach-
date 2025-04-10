package com.example.safereach.domain.model

import java.util.Date

/**
 * Model representing an emergency alert
 */
data class Alert(
    val id: String,
    val userId: String,
    val alertType: String,
    val location: LatLng,
    val timestamp: Date
) {
    /**
     * Data class representing geographical coordinates.
     *
     * @property latitude The latitude coordinate
     * @property longitude The longitude coordinate
     */
    data class Location(
        val latitude: Double,
        val longitude: Double
    )
} 