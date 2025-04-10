package com.example.safereach.data.repository

import android.content.Intent
import android.util.Log
import com.example.safereach.domain.repository.Result
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TAG = "AuthRepository"
    }

    // Current Firebase user state flow
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    init {
        // Listen for authentication state changes
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
            Log.d(TAG, "Auth state changed: ${firebaseAuth.currentUser?.uid ?: "No user"}")
        }
    }

    /**
     * Sign in with email and password
     */
    suspend fun signInWithEmailPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("Authentication failed")
            Log.d(TAG, "signInWithEmail:success")
            Result.Success(user)
        } catch (e: Exception) {
            Log.w(TAG, "signInWithEmail:failure", e)
            Result.Error("Authentication failed: ${e.localizedMessage}", e)
        }
    }
    
    /**
     * Create a new account with email and password
     */
    suspend fun createAccountWithEmailPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("Account creation failed")
            Log.d(TAG, "createUserWithEmail:success")
            Result.Success(user)
        } catch (e: Exception) {
            Log.w(TAG, "createUserWithEmail:failure", e)
            Result.Error("Account creation failed: ${e.localizedMessage}", e)
        }
    }
    
    /**
     * Sign in with Google. This requires the Intent from Google Sign-In flow.
     */
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Google authentication failed")
            Log.d(TAG, "signInWithGoogle:success")
            Result.Success(user)
        } catch (e: Exception) {
            Log.w(TAG, "signInWithGoogle:failure", e)
            Result.Error("Google sign-in failed: ${e.localizedMessage}", e)
        }
    }
    
    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Log.d(TAG, "Password reset email sent to $email")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "sendPasswordResetEmail:failure", e)
            Result.Error("Failed to send password reset email: ${e.localizedMessage}", e)
        }
    }
    
    /**
     * Sign out current user
     */
    fun signOut() {
        auth.signOut()
        Log.d(TAG, "User signed out")
    }
    
    /**
     * Check if user is currently logged in
     */
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
} 