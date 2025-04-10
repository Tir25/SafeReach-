package com.example.safereach.data.service

import android.util.Log
import com.example.safereach.data.model.EmergencyType
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake service to simulate contacting emergency services
 * This is used for demo purposes only
 */
@Singleton
class FakeEmergencyService @Inject constructor() {
    
    companion object {
        private const val TAG = "FakeEmergencyService"
        private const val CONTACT_DELAY_MS = 3000L // 3 seconds delay to simulate network call
        private const val SUCCESS_RATE = 0.9 // 90% success rate
    }
    
    /**
     * Simulate contacting emergency services
     * @param type The type of emergency service to contact
     * @return True if the contact was successful, false otherwise
     */
    suspend fun contactService(type: EmergencyType): Boolean {
        Log.d(TAG, "Contacting ${type.name} emergency service...")
        
        // Simulate network delay
        delay(CONTACT_DELAY_MS)
        
        // Simulate occasional failures (10% chance)
        val isSuccessful = Math.random() > (1 - SUCCESS_RATE)
        
        if (isSuccessful) {
            Log.d(TAG, "Successfully contacted ${type.name} emergency service")
        } else {
            Log.e(TAG, "Failed to contact ${type.name} emergency service")
        }
        
        return isSuccessful
    }
    
    /**
     * Simulate sending a community alert
     * @param type The type of emergency
     * @param latitude The latitude coordinate
     * @param longitude The longitude coordinate
     * @return True if the alert was sent successfully, false otherwise
     */
    suspend fun sendCommunityAlert(
        type: EmergencyType,
        latitude: Double,
        longitude: Double
    ): Boolean {
        Log.d(TAG, "Sending community alert for ${type.name} at location ($latitude, $longitude)")
        
        // Simulate shorter delay for community alert
        delay(1500)
        
        // Always succeed for community alerts
        Log.d(TAG, "Community alert sent successfully for ${type.name}")
        return true
    }
} 