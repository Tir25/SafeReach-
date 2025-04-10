package com.example.safereach.presentation.screens.emergency

import com.example.safereach.data.model.EmergencyType

/**
 * Represents the UI state for the emergency screen
 */
sealed class EmergencyUIState {
    /**
     * Initial state - no emergency in progress
     */
    object Idle : EmergencyUIState()
    
    /**
     * Loading state - getting location for emergency
     */
    object GettingLocation : EmergencyUIState()
    
    /**
     * Loading state - contacting emergency services
     */
    object ContactingEmergency : EmergencyUIState()
    
    /**
     * Loading state - sending community alert
     */
    object SendingCommunityAlert : EmergencyUIState()
    
    /**
     * Success state - emergency services contacted
     */
    data class Success(val type: EmergencyType) : EmergencyUIState()
    
    /**
     * Error state - failed to contact emergency services
     */
    data class Error(val message: String, val type: EmergencyType? = null) : EmergencyUIState()
} 