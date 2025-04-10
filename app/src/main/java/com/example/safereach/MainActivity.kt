package com.example.safereach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.safereach.data.local.PermissionManager
import com.example.safereach.presentation.navigation.SafeReachNavHost
import com.example.safereach.presentation.viewmodel.PermissionViewModel
import com.example.safereach.ui.theme.SafeReachTheme
import com.example.safereach.utils.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var permissionManager: PermissionManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Check permissions at startup, but don't prompt immediately
        PermissionUtils.hasLocationPermissions(this)
        PermissionUtils.hasNotificationPermissions(this)
        
        setContent {
            MainAppContent(permissionManager)
        }
    }
}

@Composable
fun MainAppContent(
    permissionManager: PermissionManager,
    permissionViewModel: PermissionViewModel = hiltViewModel()
) {
    SafeReachTheme {
        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        
        // Get permission state from view model
        val locationPermissionState by permissionViewModel.locationPermissionState.collectAsState()
        
        // Surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
            ) { paddingValues ->
                SafeReachNavHost(
                    navController = navController,
                    modifier = Modifier.padding(paddingValues),
                    permissionManager = permissionManager,
                    permissionViewModel = permissionViewModel
                )
            }
        }
    }
}