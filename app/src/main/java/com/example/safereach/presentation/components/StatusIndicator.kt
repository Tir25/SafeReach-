package com.example.safereach.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.safereach.ui.theme.Error
import com.example.safereach.ui.theme.Info
import com.example.safereach.ui.theme.Success
import com.example.safereach.ui.theme.Warning

/**
 * Status type enum for StatusIndicator
 */
enum class StatusType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

/**
 * A status indicator component for displaying different types of status messages
 * 
 * @param type The type of status to display
 * @param message The message to display
 * @param modifier Additional modifier for customization
 */
@Composable
fun StatusIndicator(
    type: StatusType,
    message: String,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, contentColor, icon) = when (type) {
        StatusType.SUCCESS -> Triple(
            Success.copy(alpha = 0.2f),
            Success,
            Icons.Filled.CheckCircle
        )
        StatusType.ERROR -> Triple(
            Error.copy(alpha = 0.2f),
            Error,
            Icons.Filled.Error
        )
        StatusType.WARNING -> Triple(
            Warning.copy(alpha = 0.2f),
            Warning,
            Icons.Filled.Warning
        )
        StatusType.INFO -> Triple(
            Info.copy(alpha = 0.2f),
            Info,
            Icons.Filled.Info
        )
    }
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, contentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = type.name.lowercase(),
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp),
            textAlign = TextAlign.Start
        )
    }
}

/**
 * A small status dot for displaying status in compact spaces
 */
@Composable
fun StatusDot(
    type: StatusType,
    modifier: Modifier = Modifier,
    size: Int = 12
) {
    val color = when (type) {
        StatusType.SUCCESS -> Success
        StatusType.ERROR -> Error
        StatusType.WARNING -> Warning
        StatusType.INFO -> Info
    }
    
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color)
    )
} 