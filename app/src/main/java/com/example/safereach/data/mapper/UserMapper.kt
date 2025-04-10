package com.example.safereach.data.mapper

import com.example.safereach.data.model.User as DataUser
import com.example.safereach.domain.model.User as DomainUser
import java.util.Date

/**
 * Maps a domain model User to a data model User
 */
fun DomainUser.toDataModel(): DataUser = DataUser(
    userId = this.id,
    email = this.email,
    displayName = this.displayName,
    phoneNumber = this.phoneNumber,
    profileCompleted = this.profileCompleted,
    emergencyContacts = this.emergencyContacts,
    notificationsEnabled = this.notificationsEnabled,
    createdAt = this.createdAt.time,
    lastSignInAt = this.lastSignInAt.time
)

/**
 * Maps a data model User to a domain model User
 */
fun DataUser.toDomainModel(): DomainUser = DomainUser(
    id = this.userId,
    email = this.email,
    displayName = this.displayName,
    phoneNumber = this.phoneNumber,
    profileCompleted = this.profileCompleted,
    emergencyContacts = this.emergencyContacts,
    notificationsEnabled = this.notificationsEnabled,
    createdAt = Date(this.createdAt),
    lastSignInAt = Date(this.lastSignInAt)
) 