package com.example.safereach.data.auth

import android.content.Context
import android.content.Intent
import com.example.safereach.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class to manage Google Sign-In functionality
 */
@Singleton
class GoogleAuthHelper @Inject constructor() {
    
    private var googleSignInClient: GoogleSignInClient? = null
    
    /**
     * Initialize Google Sign-In client with the web client ID
     */
    fun initialize(context: Context, webClientId: String) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }
    
    /**
     * Get Google Sign-In intent to start the sign-in flow
     */
    fun getSignInIntent(): Intent? {
        return googleSignInClient?.signInIntent
    }
    
    /**
     * Process the sign-in result and extract ID token
     * @return ID token or null if sign-in failed
     */
    fun getIdTokenFromIntent(data: Intent?): String? {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            return account?.idToken
        } catch (e: ApiException) {
            return null
        }
    }
    
    /**
     * Sign out from Google
     */
    fun signOut() {
        googleSignInClient?.signOut()
    }
} 