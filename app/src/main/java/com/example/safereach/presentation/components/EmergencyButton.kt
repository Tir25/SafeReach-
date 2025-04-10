package com.example.safereach.presentation.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.safereach.ui.theme.EmergencyButtonShape
import com.example.safereach.ui.theme.EmergencyButtonTextStyle

/**
 * A reusable emergency button component for Police, Ambulance, and Fire services
 * 
 * @param text The text to display on the button
 * @param icon The icon to display on the button
 * @param color The background color of the button
 * @param onClick The action to perform when the button is clicked
 * @param modifier Additional modifier for customization
 * @param enabled Whether the button is enabled or disabled
 */
@Composable
fun EmergencyButton(
    text: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        enabled = enabled,
        shape = EmergencyButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White,
            disabledContainerColor = color.copy(alpha = 0.6f),
            disabledContentColor = Color.White.copy(alpha = 0.6f)
        ),
        contentPadding = PaddingValues(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = Color.White
        )
        Text(
            text = text,
            style = EmergencyButtonTextStyle,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Police emergency button with blue background
 */
@Composable
fun PoliceEmergencyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector,
    text: String = "Police"
) {
    EmergencyButton(
        text = text,
        icon = icon,
        color = MaterialTheme.colorScheme.primary,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    )
}

/**
 * Ambulance emergency button with red background
 */
@Composable
fun AmbulanceEmergencyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector,
    text: String = "Ambulance"
) {
    EmergencyButton(
        text = text,
        icon = icon,
        color = com.example.safereach.ui.theme.Ambulance,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    )
}

/**
 * Fire emergency button with orange background
 */
@Composable
fun FireEmergencyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector,
    text: String = "Fire"
) {
    EmergencyButton(
        text = text,
        icon = icon,
        color = com.example.safereach.ui.theme.Fire,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    )
} 