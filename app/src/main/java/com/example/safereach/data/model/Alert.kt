package com.example.safereach.data.model

/**
 * Data model for alerts used in data layer
 */
data class Alert(
    val id: String = "",
    val alertId: String = "", // Keep for backward compatibility
    val userId: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val resolved: Boolean = false,
    val offlineSynced: Boolean = true  // Tracks whether alert was created offline
) 