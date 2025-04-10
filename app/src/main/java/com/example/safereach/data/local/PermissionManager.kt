package com.example.safereach.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Extension property for Context to create a DataStore for permissions
private val Context.permissionsDataStore by preferencesDataStore(name = "permissions_preferences")

/**
 * Manages permission states and preferences
 */
@Singleton
class PermissionManager @Inject constructor(
    private val context: Context
) {
    private object PreferencesKeys {
        val PERMISSION_ALREADY_REQUESTED = booleanPreferencesKey("permission_already_requested")
        val PERMISSION_PERMANENTLY_DENIED = booleanPreferencesKey("permission_permanently_denied")
        val LAST_PERMISSION_REQUEST_TIME = longPreferencesKey("last_permission_request_time")
    }
    
    // Set of permissions that have been requested during this session
    private val sessionRequestedPermissions = mutableSetOf<String>()
    
    // Flow for tracking permission request status
    private val _shouldShowPermissionRationale = MutableStateFlow(false)
    val shouldShowPermissionRationale: StateFlow<Boolean> = _shouldShowPermissionRationale.asStateFlow()
    
    private val _permissionPermanentlyDenied = MutableStateFlow(false)
    val permissionPermanentlyDenied: StateFlow<Boolean> = _permissionPermanentlyDenied.asStateFlow()
    
    /**
     * Check if a permission has already been requested in this session
     */
    fun isPermissionRequestedInSession(permission: String): Boolean {
        return sessionRequestedPermissions.contains(permission)
    }
    
    /**
     * Mark a permission as requested in this session
     */
    fun markPermissionRequestedInSession(permission: String) {
        sessionRequestedPermissions.add(permission)
    }
    
    /**
     * Get whether permissions have already been requested from persistent storage
     */
    suspend fun hasAlreadyRequestedPermissions(context: Context): Boolean {
        return context.permissionsDataStore.data.map { preferences ->
            preferences[PreferencesKeys.PERMISSION_ALREADY_REQUESTED] ?: false
        }.first()
    }
    
    /**
     * Mark permissions as requested in persistent storage
     */
    suspend fun markPermissionsAsRequested(context: Context) {
        context.permissionsDataStore.edit { preferences ->
            preferences[PreferencesKeys.PERMISSION_ALREADY_REQUESTED] = true
            preferences[PreferencesKeys.LAST_PERMISSION_REQUEST_TIME] = System.currentTimeMillis()
        }
    }
    
    /**
     * Check if permissions have been permanently denied
     */
    suspend fun isPermissionPermanentlyDenied(context: Context): Boolean {
        return context.permissionsDataStore.data.map { preferences ->
            preferences[PreferencesKeys.PERMISSION_PERMANENTLY_DENIED] ?: false
        }.first()
    }
    
    /**
     * Mark permissions as permanently denied
     */
    suspend fun markPermissionPermanentlyDenied(context: Context, isDenied: Boolean) {
        context.permissionsDataStore.edit { preferences ->
            preferences[PreferencesKeys.PERMISSION_PERMANENTLY_DENIED] = isDenied
        }
        _permissionPermanentlyDenied.value = isDenied
    }
    
    /**
     * Reset permission request state (e.g., after user manually grants permission from settings)
     */
    suspend fun resetPermissionState(context: Context) {
        context.permissionsDataStore.edit { preferences ->
            preferences[PreferencesKeys.PERMISSION_ALREADY_REQUESTED] = false
            preferences[PreferencesKeys.PERMISSION_PERMANENTLY_DENIED] = false
        }
        sessionRequestedPermissions.clear()
        _shouldShowPermissionRationale.value = false
        _permissionPermanentlyDenied.value = false
    }
    
    /**
     * Update permission rationale state
     */
    fun updateShouldShowRationale(shouldShow: Boolean) {
        _shouldShowPermissionRationale.value = shouldShow
    }
} 