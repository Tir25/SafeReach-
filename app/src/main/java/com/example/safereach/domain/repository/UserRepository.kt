package com.example.safereach.domain.repository

import com.example.safereach.domain.model.User
import com.google.firebase.auth.FirebaseUser

/**
 * Repository interface for managing user profiles
 */
interface UserRepository {
    
    /**
     * Create or update a user profile
     * @param user The user profile to save
     * @return Result containing the saved user
     */
    suspend fun saveUser(user: User): Result<User>
    
    /**
     * Get a user profile by ID
     * @param userId ID of the user to retrieve
     * @return Result containing the user profile
     */
    suspend fun getUserById(userId: String): Result<User>
    
    /**
     * Get the current authenticated user's profile
     * @return Result containing the current user's profile
     */
    suspend fun getCurrentUserProfile(): Result<User>
    
    /**
     * Create a new user profile from Firebase User
     * @param firebaseUser The authenticated Firebase user
     * @return Result containing the created user profile
     */
    suspend fun createUserProfile(firebaseUser: FirebaseUser): Result<User>
    
    /**
     * Update the user's last sign-in timestamp
     * @param userId ID of the user to update
     * @return Result indicating success or failure
     */
    suspend fun updateLastSignIn(userId: String): Result<Unit>
    
    /**
     * Update emergency contacts for a user
     * @param userId ID of the user to update
     * @param contacts List of contact IDs
     * @return Result indicating success or failure
     */
    suspend fun updateEmergencyContacts(userId: String, contacts: List<String>): Result<Unit>
} 