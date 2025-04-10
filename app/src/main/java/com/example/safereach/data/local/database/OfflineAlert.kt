package com.example.safereach.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing an alert stored offline, waiting to be synced with Firestore
 */
@Entity(tableName = "offline_alerts")
data class OfflineAlert(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val type: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val resolved: Boolean = false,
    val synced: Boolean = false,  // Indicates if this alert has been successfully uploaded to Firestore
    val firestoreId: String = ""  // If synced, contains the Firestore ID of the alert
) 