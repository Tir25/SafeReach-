package com.example.safereach.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data class to hold emergency alert data for WorkManager
 */
@Parcelize
data class EmergencyAlertData(
    val emergencyType: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = "",
    val isRetry: Boolean = false
) : Parcelable 