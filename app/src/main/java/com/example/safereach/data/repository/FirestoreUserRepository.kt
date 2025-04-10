package com.example.safereach.data.repository

import android.util.Log
import com.example.safereach.data.mapper.toDomainModel
import com.example.safereach.data.mapper.toDataModel
import com.example.safereach.data.model.User as DataUser
import com.example.safereach.domain.model.User as DomainUser
import com.example.safereach.domain.repository.Result
import com.example.safereach.domain.repository.UserRepository
import com.example.safereach.utils.Constants.USERS_COLLECTION
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreUserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : UserRepository {

    companion object {
        private const val TAG = "FirestoreUserRepository"
    }

    private val _currentUserProfile = MutableStateFlow<DomainUser?>(null)
    val currentUserProfile: StateFlow<DomainUser?> = _currentUserProfile

    /**
     * Create or update a user profile in Firestore
     */
    override suspend fun saveUser(user: DomainUser): Result<DomainUser> {
        return try {
            val dataUser = user.toDataModel()
            
            // Set data with merge option to update only provided fields
            firestore.collection(USERS_COLLECTION)
                .document(dataUser.userId)
                .set(dataUser, SetOptions.merge())
                .await()
            
            // Return updated user
            Result.Success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user", e)
            Result.Error("Failed to save user: ${e.message}", e)
        }
    }

    /**
     * Get user profile by ID
     */
    override suspend fun getUserById(userId: String): Result<DomainUser> {
        return try {
            val documentSnapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (documentSnapshot.exists()) {
                val dataUser = documentSnapshot.toObject(DataUser::class.java)
                if (dataUser != null) {
                    Result.Success(dataUser.toDomainModel())
                } else {
                    Result.Error("Failed to parse user data")
                }
            } else {
                Result.Error("User not found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user", e)
            Result.Error("Failed to get user: ${e.message}", e)
        }
    }

    /**
     * Get current authenticated user's profile
     */
    override suspend fun getCurrentUserProfile(): Result<DomainUser> {
        val currentUser = auth.currentUser ?: return Result.Error("No authenticated user")
        
        return try {
            val result = getUserById(currentUser.uid)
            
            if (result is Result.Success) {
                _currentUserProfile.value = result.data
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current user profile", e)
            Result.Error("Failed to get current user profile: ${e.message}", e)
        }
    }

    /**
     * Create user profile for newly registered user
     */
    override suspend fun createUserProfile(firebaseUser: FirebaseUser): Result<DomainUser> {
        val now = Date()
        val domainUser = DomainUser(
            id = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            displayName = firebaseUser.displayName ?: "",
            phoneNumber = firebaseUser.phoneNumber ?: "",
            profileCompleted = false,
            emergencyContacts = emptyList(),
            notificationsEnabled = true,
            createdAt = now,
            lastSignInAt = now
        )
        
        return saveUser(domainUser)
    }

    /**
     * Update user's last sign-in timestamp
     */
    override suspend fun updateLastSignIn(userId: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("lastSignInAt", System.currentTimeMillis())
                .await()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating last sign in", e)
            Result.Error("Failed to update last sign in: ${e.message}", e)
        }
    }
    
    /**
     * Update emergency contacts for a user
     */
    override suspend fun updateEmergencyContacts(userId: String, contacts: List<String>): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("emergencyContacts", contacts)
                .await()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating emergency contacts", e)
            Result.Error("Failed to update emergency contacts: ${e.message}", e)
        }
    }
} 