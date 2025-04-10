package com.example.safereach.presentation.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Dialog to show when location permissions are needed
 */
@Composable
fun LocationPermissionsDialog(
    onDismiss: () -> Unit,
    onRequestPermission: () -> Unit,
    rationale: Boolean = false,
    permanentDenial: Boolean = false
) {
    val context = LocalContext.current
    val title = when {
        permanentDenial -> "Location Access Required"
        rationale -> "Why We Need Location"
        else -> "Location Permission Required"
    }
    
    val message = when {
        permanentDenial -> "You've denied location permission permanently. SafeReach needs location " +
                "access to send your location in emergencies. Please enable it in Settings."
        rationale -> "SafeReach needs location permission to send your location during emergencies. " +
                "Without this, emergency services won't be able to find you accurately in a crisis."
        else -> "SafeReach needs your location to send emergency alerts " +
                "with your precise position. This is crucial for emergency services to find you."
    }
    
    val buttonText = when {
        permanentDenial -> "Open Settings"
        rationale -> "Grant Access"
        else -> "Allow"
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = message,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Without this permission, emergency services may not be able to locate you accurately.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onRequestPermission()
                    onDismiss()
                }
            ) {
                Text(text = buttonText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Not Now")
            }
        }
    )
}

/**
 * Open the app settings to allow the user to enable permissions
 */
private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
} 