package com.example.safereach.presentation.screens.emergency

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.safereach.data.model.EmergencyType
import com.example.safereach.presentation.base.UiState
import com.example.safereach.presentation.components.GpsDisabledDialog
import com.example.safereach.presentation.components.LocationPermissionsDialog
import com.example.safereach.presentation.location.toLatLngString
import com.example.safereach.utils.PermissionUtils
import com.example.safereach.data.local.PermissionManager
import com.example.safereach.presentation.viewmodel.PermissionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyTriggerScreen(
    onBackPressed: () -> Unit,
    viewModel: EmergencyTriggerViewModel = hiltViewModel(),
    permissionManager: PermissionManager,
    permissionViewModel: PermissionViewModel = hiltViewModel()
) {
    val alertSent by viewModel.alertSent.collectAsState()
    val selectedEmergencyType by viewModel.selectedEmergencyType.collectAsState()
    val locationState by viewModel.locationState.collectAsState()
    val canTriggerEmergency by viewModel.canTriggerEmergency.collectAsState()
    val cooldownTimeText by viewModel.cooldownTimeText.collectAsState()
    val isContactingEmergency by viewModel.isContactingEmergency.collectAsState()
    val isSendingCommunityAlert by viewModel.isSendingCommunityAlert.collectAsState()
    val alertState by viewModel.alertState.collectAsState()
    
    // Get the emergency UI state
    val emergencyUIState by viewModel.emergencyUIState.collectAsState()
    
    // Get permission states from view model
    val locationPermissionGranted by permissionViewModel.locationPermissionState.collectAsState()
    
    val context = LocalContext.current
    
    // Scaffold state for Snackbar messages
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show error messages as Snackbars based on emergency UI state
    LaunchedEffect(emergencyUIState) {
        when (emergencyUIState) {
            is EmergencyUIState.Error -> {
                val error = emergencyUIState as EmergencyUIState.Error
                snackbarHostState.showSnackbar(
                    message = error.message,
                    duration = SnackbarDuration.Long
                )
            }
            is EmergencyUIState.Success -> {
                val success = emergencyUIState as EmergencyUIState.Success
                snackbarHostState.showSnackbar(
                    message = "Emergency contact successful! ${success.type.displayName} services and community notified.",
                    duration = SnackbarDuration.Long
                )
            }
            else -> { /* No action needed for other states */ }
        }
    }
    
    // Request location permission if needed
    PermissionUtils.RequestLocationPermission(
        permissionManager = permissionManager,
        snackbarHostState = snackbarHostState,
        onPermissionResult = { granted ->
            viewModel.updateLocationPermission(granted)
            permissionViewModel.updateLocationPermission(granted)
        }
    )
    
    // GPS dialog - only show if permissions are granted but GPS is disabled
    var showGpsDialog by remember { mutableStateOf(false) }
    LaunchedEffect(locationState) {
        if (locationPermissionGranted && !locationState.gpsEnabled) {
            showGpsDialog = true
        }
    }
    
    if (showGpsDialog) {
        GpsDisabledDialog(
            onDismiss = { showGpsDialog = false },
            onOpenSettings = {
                PermissionUtils.openLocationSettings(context)
            }
        )
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Emergency Alert") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    LocationStatusIcon(
                        hasLocation = locationState.hasLocationData,
                        isLoading = locationState.isLoading,
                        onClick = {
                            // Refresh permissions and location when icon is clicked
                            permissionViewModel.refreshPermissionState()
                            viewModel.refreshLocation()
                        }
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (alertSent) {
                AlertSentConfirmation(
                    onBackPressed = onBackPressed,
                    emergencyType = selectedEmergencyType,
                    location = locationState.location
                )
            } else {
                EmergencyOptions(
                    viewModel = viewModel,
                    canTrigger = canTriggerEmergency,
                    cooldownText = cooldownTimeText,
                    emergencyUIState = emergencyUIState
                )
            }
        }
    }
}

@Composable
private fun LocationStatusIcon(
    hasLocation: Boolean, 
    isLoading: Boolean,
    onClick: () -> Unit = {}
) {
    val tint = if (hasLocation) MaterialTheme.colorScheme.primary else Color.Gray
    IconButton(onClick = onClick) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location Status",
                tint = tint
            )
        }
    }
}

@Composable
private fun EmergencyOptions(
    viewModel: EmergencyTriggerViewModel,
    canTrigger: Boolean,
    cooldownText: String,
    emergencyUIState: EmergencyUIState
) {
    Text(
        text = "Select Emergency Type",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Show emergency process status based on UI state
    val isProcessing = emergencyUIState is EmergencyUIState.ContactingEmergency || 
                      emergencyUIState is EmergencyUIState.SendingCommunityAlert ||
                      emergencyUIState is EmergencyUIState.GettingLocation
    
    if (isProcessing || emergencyUIState is EmergencyUIState.Error) {
        EmergencyProcessStatus(
            emergencyUIState = emergencyUIState,
            viewModel = viewModel
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
    
    // Show cooldown message if needed
    if (!canTrigger) {
        CooldownMessage(cooldownText)
        Spacer(modifier = Modifier.height(16.dp))
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        EmergencyTypeButton(
            label = EmergencyType.POLICE.displayName,
            icon = Icons.Default.LocalPolice,
            color = EmergencyType.POLICE.color,
            enabled = canTrigger && !isProcessing && !(emergencyUIState is EmergencyUIState.Error),
            onClick = {
                viewModel.triggerEmergency(EmergencyType.POLICE)
            }
        )
        
        EmergencyTypeButton(
            label = EmergencyType.AMBULANCE.displayName,
            icon = Icons.Default.LocalHospital,
            color = EmergencyType.AMBULANCE.color,
            enabled = canTrigger && !isProcessing && !(emergencyUIState is EmergencyUIState.Error),
            onClick = {
                viewModel.triggerEmergency(EmergencyType.AMBULANCE)
            }
        )
        
        EmergencyTypeButton(
            label = EmergencyType.FIRE.displayName,
            icon = Icons.Default.LocalFireDepartment,
            color = EmergencyType.FIRE.color,
            enabled = canTrigger && !isProcessing && !(emergencyUIState is EmergencyUIState.Error),
            onClick = {
                viewModel.triggerEmergency(EmergencyType.FIRE)
            }
        )
    }
    
    Spacer(modifier = Modifier.height(32.dp))
    
    Text(
        text = when {
            emergencyUIState is EmergencyUIState.GettingLocation -> "Getting your location for emergency..."
            emergencyUIState is EmergencyUIState.ContactingEmergency -> "Contacting emergency services..."
            emergencyUIState is EmergencyUIState.SendingCommunityAlert -> "Sending community alert..."
            emergencyUIState is EmergencyUIState.Error -> "Please resolve the error to continue"
            isProcessing -> "Please wait while we process your emergency alert"
            !canTrigger -> "Please wait for the cooldown period to end before sending another alert"
            else -> "Tap an emergency type to send an alert with your current location"
        },
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun EmergencyProcessStatus(
    emergencyUIState: EmergencyUIState,
    viewModel: EmergencyTriggerViewModel
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        when (emergencyUIState) {
            is EmergencyUIState.GettingLocation -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Getting your location...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            is EmergencyUIState.ContactingEmergency -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Contacting emergency services...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            is EmergencyUIState.SendingCommunityAlert -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Sending community alert...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            is EmergencyUIState.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = emergencyUIState.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    // If error is location-related, show a refresh button
                    if (emergencyUIState.message.contains("location", ignoreCase = true)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.refreshEmergencyLocation() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Refresh Location"
                                )
                                Text("Refresh Location")
                            }
                        }
                    }
                }
            }
            
            else -> {
                // No status to show for Idle or Success states
            }
        }
    }
}

@Composable
private fun CooldownMessage(cooldownText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = "Cooldown",
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "You must wait $cooldownText before sending another alert",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun EmergencyTypeButton(
    label: String,
    icon: ImageVector,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = if (enabled) 0.2f else 0.05f))
                .padding(8.dp)
                .alpha(if (enabled) 1f else 0.5f),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(84.dp),
                enabled = enabled
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) color else color.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AlertSentConfirmation(
    onBackPressed: () -> Unit,
    emergencyType: EmergencyType?,
    location: android.location.Location?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Emergency Alert Sent!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            emergencyType?.let {
                Text(
                    text = "Emergency type: ${it.displayName}",
                    style = MaterialTheme.typography.titleMedium,
                    color = it.color,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            location?.let {
                Text(
                    text = "Location: ${it.toLatLngString()}",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            } ?: Text(
                text = "Using last known location.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Your location has been shared with emergency services.",
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onBackPressed,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Return to Home")
            }
        }
    }
} 