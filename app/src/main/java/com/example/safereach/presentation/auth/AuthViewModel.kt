package com.example.safereach.presentation.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safereach.data.auth.GoogleAuthHelper
import com.example.safereach.data.repository.AuthRepository
import com.example.safereach.domain.repository.Result
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for handling authentication state and operations
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleAuthHelper: GoogleAuthHelper,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "AuthViewModel"
        private const val GOOGLE_WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID" // Replace with actual web client ID from Firebase console
    }

    // Initialize Google Sign-In
    init {
        googleAuthHelper.initialize(context, GOOGLE_WEB_CLIENT_ID)
    }

    // UI state
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Current user from repository
    val currentUser = authRepository.currentUser

    /**
     * Sign in with email and password
     */
    fun signInWithEmailPassword(email: String, password: String) {
        if (!validateCredentials(email, password)) return

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            when (val result = authRepository.signInWithEmailPassword(email, password)) {
                is Result.Success -> {
                    _authState.value = AuthState.Success(result.data)
                }
                is Result.Error -> {
                    _authState.value = AuthState.Error(result.message)
                }
                is Result.Loading -> {
                    // Already handled by setting _authState.value to Loading above
                }
            }
        }
    }

    /**
     * Create a new account with email and password
     */
    fun createAccount(email: String, password: String, confirmPassword: String) {
        if (!validateNewAccount(email, password, confirmPassword)) return

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            when (val result = authRepository.createAccountWithEmailPassword(email, password)) {
                is Result.Success -> {
                    _authState.value = AuthState.Success(result.data)
                }
                is Result.Error -> {
                    _authState.value = AuthState.Error(result.message)
                }
                is Result.Loading -> {
                    // Already handled by setting _authState.value to Loading above
                }
            }
        }
    }

    /**
     * Get Google Sign-In intent to start the sign-in flow
     */
    fun getGoogleSignInIntent(): Intent? {
        return googleAuthHelper.getSignInIntent()
    }

    /**
     * Process Google Sign-In result
     */
    fun handleGoogleSignInResult(data: Intent?) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val idToken = googleAuthHelper.getIdTokenFromIntent(data)
            if (idToken != null) {
                when (val result = authRepository.signInWithGoogle(idToken)) {
                    is Result.Success -> {
                        _authState.value = AuthState.Success(result.data)
                    }
                    is Result.Error -> {
                        _authState.value = AuthState.Error(result.message)
                    }
                    is Result.Loading -> {
                        // Already handled by setting _authState.value to Loading above
                    }
                }
            } else {
                _authState.value = AuthState.Error("Google sign-in failed")
            }
        }
    }

    /**
     * Send password reset email
     */
    fun sendPasswordResetEmail(email: String) {
        if (email.isBlank()) {
            _authState.value = AuthState.Error("Email cannot be empty")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            when (val result = authRepository.sendPasswordResetEmail(email)) {
                is Result.Success -> {
                    _authState.value = AuthState.PasswordResetSent
                }
                is Result.Error -> {
                    _authState.value = AuthState.Error(result.message)
                }
                is Result.Loading -> {
                    // Already handled by setting _authState.value to Loading above
                }
            }
        }
    }

    /**
     * Sign out the current user
     */
    fun signOut() {
        authRepository.signOut()
        googleAuthHelper.signOut()
        _authState.value = AuthState.SignedOut
    }

    /**
     * Reset the auth state to idle
     */
    fun resetState() {
        _authState.value = AuthState.Idle
    }

    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return authRepository.isUserLoggedIn()
    }

    /**
     * Validate email and password
     */
    private fun validateCredentials(email: String, password: String): Boolean {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password cannot be empty")
            return false
        }
        return true
    }

    /**
     * Validate new account information
     */
    private fun validateNewAccount(email: String, password: String, confirmPassword: String): Boolean {
        if (email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            _authState.value = AuthState.Error("All fields are required")
            return false
        }

        if (password != confirmPassword) {
            _authState.value = AuthState.Error("Passwords do not match")
            return false
        }

        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters")
            return false
        }

        return true
    }
}

/**
 * Represents the possible authentication states
 */
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object SignedOut : AuthState()
    object PasswordResetSent : AuthState()
    data class Success(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
} 