package com.example.safereach.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.safereach.BuildConfig
import com.example.safereach.data.local.PermissionManager
import com.example.safereach.presentation.auth.AuthViewModel
import com.example.safereach.presentation.screens.auth.LoginScreen
import com.example.safereach.presentation.screens.auth.RegisterScreen
import com.example.safereach.presentation.screens.debug.DebugScreen
import com.example.safereach.presentation.screens.emergency.EmergencyTriggerScreen
import com.example.safereach.presentation.screens.home.HomeScreen
import com.example.safereach.presentation.screens.settings.SettingsScreen
import com.example.safereach.presentation.viewmodel.PermissionViewModel

/**
 * Navigation routes for SafeReach app
 */
object Routes {
    const val HOME = "home"
    const val EMERGENCY_TRIGGER = "emergency_trigger"
    const val SETTINGS = "settings"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val SPLASH = "splash"
    const val DEBUG = "debug"
}

/**
 * NavHost setup for SafeReach app
 */
@Composable
fun SafeReachNavHost(
    navController: NavHostController,
    startDestination: String = Routes.LOGIN,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = hiltViewModel(),
    permissionManager: PermissionManager,
    permissionViewModel: PermissionViewModel
) {
    // Observer authentication state
    val currentUser by authViewModel.currentUser.collectAsState()
    
    // If user is logged in, navigate to Home
    LaunchedEffect(currentUser) {
        if (currentUser != null && navController.currentDestination?.route == Routes.LOGIN) {
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.LOGIN) { inclusive = true }
            }
        }
    }
    
    // Check if permissions should be refreshed whenever we navigate
    LaunchedEffect(navController.currentBackStackEntry) {
        permissionViewModel.refreshPermissionState()
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Auth screens
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onRegisterClick = { navController.navigate(Routes.REGISTER) }
            )
        }
        
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegistrationSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Main app screens
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToEmergency = { navController.navigate(Routes.EMERGENCY_TRIGGER) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                permissionManager = permissionManager,
                permissionViewModel = permissionViewModel
            )
        }
        
        composable(Routes.EMERGENCY_TRIGGER) {
            EmergencyTriggerScreen(
                onBackPressed = { navController.popBackStack() },
                permissionManager = permissionManager,
                permissionViewModel = permissionViewModel
            )
        }
        
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBackPressed = { navController.popBackStack() },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                // In debug builds, allow navigation to the debug screen
                onNavigateToDebug = if (BuildConfig.DEBUG) {
                    { navController.navigate(Routes.DEBUG) }
                } else null,
                permissionManager = permissionManager,
                permissionViewModel = permissionViewModel
            )
        }
        
        // Debug screen - only included in debug builds
        if (BuildConfig.DEBUG) {
            composable(Routes.DEBUG) {
                DebugScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        
        // Add more routes as needed
    }
} 