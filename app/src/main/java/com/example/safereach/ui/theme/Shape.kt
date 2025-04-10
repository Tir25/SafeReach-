package com.example.safereach.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

// Specific shape overrides
val ButtonShape = RoundedCornerShape(24.dp)
val CardShape = RoundedCornerShape(16.dp)
val AlertCardShape = RoundedCornerShape(20.dp)
val EmergencyButtonShape = RoundedCornerShape(50.dp) // Fully rounded for emergency buttons 