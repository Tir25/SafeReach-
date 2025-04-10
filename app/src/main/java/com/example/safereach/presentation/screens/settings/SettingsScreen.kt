package com.example.safereach.presentation.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.safereach.BuildConfig
import com.example.safereach.data.local.PermissionManager
import com.example.safereach.presentation.components.PermissionStatusCard
import com.example.safereach.presentation.components.SettingToggle
import com.example.safereach.presentation.viewmodel.PermissionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackPressed: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToDebug: (() -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel(),
    permissionManager: PermissionManager,
    permissionViewModel: PermissionViewModel
) {
    val settingsState by viewModel.settingsState.collectAsState()
    val locationPermissionState by viewModel.locationPermissionState.collectAsState()
    val notificationPermissionState by viewModel.notificationPermissionState.collectAsState()
    val locationServicesState by viewModel.locationServicesState.collectAsState()
    
    // Refresh permission states when screen becomes active
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Account Section
            Text(
                text = "Account",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sign out button
            Button(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Sign Out",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(text = "Sign Out")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Permissions Section
            Text(
                text = "Permissions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Location Permission Card
            PermissionStatusCard(
                title = "Location Permission",
                description = if (locationPermissionState) {
                    "Location permission granted. The app can access your location."
                } else {
                    "Location permission denied. The app cannot access your location, which is required for emergency services."
                },
                isGranted = locationPermissionState,
                onSettingsClick = { viewModel.openAppSettings() },
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Location Services Card
            PermissionStatusCard(
                title = "Location Services",
                description = if (locationServicesState) {
                    "Location services are enabled. The app can get your precise location."
                } else {
                    "Location services are disabled. The app cannot get your location, which is required for emergency services."
                },
                isGranted = locationServicesState,
                onSettingsClick = { viewModel.openLocationSettings() },
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Notification Permission Card
            PermissionStatusCard(
                title = "Notification Permission",
                description = if (notificationPermissionState) {
                    "Notification permission granted. The app can send you alerts."
                } else {
                    "Notification permission denied. The app cannot send you important alerts."
                },
                isGranted = notificationPermissionState,
                onSettingsClick = { viewModel.openAppSettings() },
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            
            // App Settings Section
            Text(
                text = "App Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Auto Location Sharing Setting
            SettingToggle(
                title = "Auto Location Sharing",
                description = "Enable location sharing automatically during emergencies",
                icon = Icons.Default.LocationOn,
                isChecked = settingsState.autoLocationSharing,
                onCheckedChange = { viewModel.updateAutoLocationSharing(it) },
                enabled = locationPermissionState
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Offline Fallback Setting
            SettingToggle(
                title = "Offline Mode",
                description = "Store alerts locally when network is unavailable",
                icon = Icons.Default.WifiOff,
                isChecked = settingsState.offlineFallback,
                onCheckedChange = { viewModel.updateOfflineFallback(it) }
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Notification Sounds Setting
            SettingToggle(
                title = "Notification Sounds",
                description = "Play sounds for emergency notifications",
                icon = Icons.Default.Notifications,
                isChecked = settingsState.notificationSounds,
                onCheckedChange = { viewModel.updateNotificationSounds(it) },
                enabled = notificationPermissionState
            )
            
            // Nearby alerts notifications
            SettingToggle(
                title = "Nearby Emergency Alerts",
                description = "Get notified about emergency alerts in your area",
                icon = Icons.Default.Notifications,
                isChecked = viewModel.nearbyAlertsEnabled.collectAsState().value,
                onCheckedChange = { viewModel.updateNearbyAlerts(it) }
            )
            
            // Add debug section only in debug builds
            if (BuildConfig.DEBUG && onNavigateToDebug != null) {
                Spacer(modifier = Modifier.height(32.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                
                // Debug Section
                Text(
                    text = "Developer Options",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onNavigateToDebug,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Debug",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(text = "Debug & Testing Tools")
                }
            }
            
            // Add spacer at the bottom for comfortable scrolling
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
} 