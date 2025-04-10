package com.example.safereach.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Banner that shows information about pending offline alerts
 */
@Composable
fun OfflineAlertBanner(
    pendingAlertsCount: Int,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (pendingAlertsCount <= 0) return
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = "Offline",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(end = 8.dp)
            )
            
            Text(
                text = "$pendingAlertsCount alert${if (pendingAlertsCount > 1) "s" else ""} pending sync",
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            
            TextButton(
                onClick = onSyncClick
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sync now",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "SYNC NOW",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
} 