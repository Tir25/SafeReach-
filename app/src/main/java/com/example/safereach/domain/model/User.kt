package com.example.safereach.domain.model

import java.util.Date

/**
 * Domain model for user profile information
 */
data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val phoneNumber: String,
    val profileCompleted: Boolean,
    val emergencyContacts: List<String>,
    val notificationsEnabled: Boolean,
    val createdAt: Date,
    val lastSignInAt: Date
) 