package com.example.safereach.presentation.screens.debug

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.safereach.BuildConfig
import com.example.safereach.presentation.components.CustomCard
import com.example.safereach.presentation.components.StatusIndicator
import com.example.safereach.presentation.components.StatusType
import com.example.safereach.utils.EdgeCaseTester
import com.example.safereach.utils.TestUtils
import kotlinx.coroutines.launch

/**
 * Screen for debugging purposes
 * Only visible in debug builds for developers to test edge cases
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    viewModel: DebugViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize the EdgeCaseTester
    val edgeCaseTester = remember { EdgeCaseTester(context) }
    val testResults by edgeCaseTester.testResults.collectAsState()
    
    var testSummary by remember { mutableStateOf("No tests have been run yet.") }
    var isOfflineModeEnabled by remember { mutableStateOf(false) }
    var isRateLimitingEnabled by remember { mutableStateOf(true) }
    
    LaunchedEffect(testResults) {
        if (testResults.isNotEmpty()) {
            testSummary = edgeCaseTester.getTestSummary()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug & Testing") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App info section
            CustomCard(
                title = "App Information",
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Version", style = MaterialTheme.typography.bodyMedium)
                        Text(BuildConfig.VERSION_NAME, style = MaterialTheme.typography.bodyMedium)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Build Type", style = MaterialTheme.typography.bodyMedium)
                        Text(BuildConfig.BUILD_TYPE, style = MaterialTheme.typography.bodyMedium)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Debug Mode", style = MaterialTheme.typography.bodyMedium)
                        Icon(
                            imageVector = if (BuildConfig.DEBUG) Icons.Default.CheckCircle else Icons.Default.Clear,
                            contentDescription = if (BuildConfig.DEBUG) "Enabled" else "Disabled",
                            tint = if (BuildConfig.DEBUG) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // Test options section
            CustomCard(
                title = "Test Options",
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Simulate Offline Mode", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = isOfflineModeEnabled,
                            onCheckedChange = { isOfflineModeEnabled = it }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Rate Limiting", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = isRateLimitingEnabled,
                            onCheckedChange = { isRateLimitingEnabled = it }
                        )
                    }
                }
            }
            
            // Test actions section
            CustomCard(
                title = "Test Actions",
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { edgeCaseTester.testGpsAvailability() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Test GPS Availability")
                    }
                    
                    Button(
                        onClick = { edgeCaseTester.testNetworkConnectivity() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Test Network Connectivity")
                    }
                    
                    Button(
                        onClick = { edgeCaseTester.testLocationPermissions() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Test Location Permissions")
                    }
                    
                    Button(
                        onClick = { edgeCaseTester.testNotificationPermissions() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Test Notification Permissions")
                    }
                    
                    Button(
                        onClick = { edgeCaseTester.testRepeatedAlertTriggers() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Test Repeated Alert Triggers")
                    }
                    
                    Button(
                        onClick = { edgeCaseTester.testOfflineMode() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Test Offline Mode")
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Button(
                        onClick = { edgeCaseTester.runAllTests() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Run All Tests")
                    }
                }
            }
            
            // Test results section
            CustomCard(
                title = "Test Results",
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = testSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(8.dp)
                    )
                    
                    if (testResults.isEmpty()) {
                        StatusIndicator(
                            type = StatusType.INFO,
                            message = "Run tests to see results here",
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Button(
                            onClick = {
                                // Open app settings
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open App Settings")
                        }
                    }
                }
            }
            
            // Debug network section
            CustomCard(
                title = "Network Status",
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    val isNetworkConnected = TestUtils.isNetworkConnected(context)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isNetworkConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = if (isNetworkConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        
                        Text(
                            text = if (isNetworkConnected) "Connected" else "Disconnected",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = {
                            // Toggle airplane mode info
                            val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Network Settings")
                    }
                }
            }
        }
    }
} 