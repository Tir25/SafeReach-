package com.example.safereach.presentation.screens.home

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.safereach.data.local.PermissionManager
import com.example.safereach.data.workers.AlertSyncWorker
import com.example.safereach.presentation.auth.AuthViewModel
import com.example.safereach.presentation.components.OfflineAlertBanner
import com.example.safereach.presentation.viewmodel.PermissionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToEmergency: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    permissionManager: PermissionManager,
    permissionViewModel: PermissionViewModel
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    val alertsState by viewModel.alertsState.collectAsState()
    val pendingOfflineAlerts by viewModel.pendingOfflineAlerts.collectAsState()
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SafeReach") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu"
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sign Out") },
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.ExitToApp, 
                                    contentDescription = "Sign Out"
                                ) 
                            },
                            onClick = {
                                showMenu = false
                                onSignOut()
                            }
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
        ) {
            // Offline alert banner
            OfflineAlertBanner(
                pendingAlertsCount = pendingOfflineAlerts,
                onSyncClick = {
                    AlertSyncWorker.startOneTimeSync(context)
                }
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // User Info Card
                currentUser?.let { user ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Welcome",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = user.email ?: "User",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }
                }
                
                when (alertsState) {
                    is AlertsState.Loading -> {
                        Text("Loading alerts...")
                    }
                    is AlertsState.Success -> {
                        val alerts = (alertsState as AlertsState.Success).alerts
                        
                        if (alerts.isEmpty()) {
                            Text(
                                text = "No recent alerts",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            Text(
                                text = "You have ${alerts.size} recent alerts",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            // Additional UI for showing alerts could be added here
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Emergency Button
                        Button(
                            onClick = onNavigateToEmergency,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Emergency"
                            )
                            Text(
                                text = "Emergency Alert",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    is AlertsState.Error -> {
                        Text(
                            text = "Error: ${(alertsState as AlertsState.Error).message}",
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Show emergency button anyway
                        Button(
                            onClick = onNavigateToEmergency,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Emergency"
                            )
                            Text(
                                text = "Emergency Alert",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
} 