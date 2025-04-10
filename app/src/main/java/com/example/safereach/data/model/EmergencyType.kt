package com.example.safereach.data.model

import androidx.compose.ui.graphics.Color
import com.example.safereach.ui.theme.EmergencyAmbulance
import com.example.safereach.ui.theme.EmergencyFire
import com.example.safereach.ui.theme.EmergencyPolice

enum class EmergencyType(val displayName: String, val color: Color) {
    POLICE("Police", EmergencyPolice),
    AMBULANCE("Ambulance", EmergencyAmbulance),
    FIRE("Fire", EmergencyFire);

    companion object {
        fun fromString(value: String): EmergencyType {
            return when (value.uppercase()) {
                "POLICE" -> POLICE
                "AMBULANCE" -> AMBULANCE
                "FIRE" -> FIRE
                else -> throw IllegalArgumentException("Unknown emergency type: $value")
            }
        }
    }
} 