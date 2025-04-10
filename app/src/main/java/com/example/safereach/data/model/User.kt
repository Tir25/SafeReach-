package com.example.safereach.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Data model for user profile information stored in Firestore
 */
data class User(
    @DocumentId val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val phoneNumber: String = "",
    val profileCompleted: Boolean = false,
    val emergencyContacts: List<String> = emptyList(),
    val notificationsEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSignInAt: Long = System.currentTimeMillis()
) 