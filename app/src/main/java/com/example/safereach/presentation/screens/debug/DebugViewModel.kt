package com.example.safereach.presentation.screens.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safereach.utils.LogUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import javax.inject.Inject

/**
 * ViewModel for the Debug Screen
 * Used for testing and debugging features
 */
@HiltViewModel
class DebugViewModel @Inject constructor() : ViewModel() {
    
    companion object {
        private const val TAG = "DebugViewModel"
    }
    
    // State for holding recent log entries
    private val _logEntries = MutableStateFlow<List<String>>(emptyList())
    val logEntries: StateFlow<List<String>> = _logEntries.asStateFlow()
    
    // State for debug options
    private val _offlineModeEnabled = MutableStateFlow(false)
    val offlineModeEnabled: StateFlow<Boolean> = _offlineModeEnabled.asStateFlow()
    
    private val _rateLimitingEnabled = MutableStateFlow(true)
    val rateLimitingEnabled: StateFlow<Boolean> = _rateLimitingEnabled.asStateFlow()
    
    init {
        LogUtils.i(TAG, "DebugViewModel initialized")
    }
    
    /**
     * Loads recent log entries using logcat
     */
    fun loadRecentLogs() {
        viewModelScope.launch {
            try {
                // Use logcat command to get recent logs
                val process = Runtime.getRuntime().exec("logcat -d -t 100 SafeReach:D *:S")
                val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
                
                val logs = mutableListOf<String>()
                var line: String?
                
                while (bufferedReader.readLine().also { line = it } != null) {
                    line?.let {
                        if (it.isNotEmpty()) {
                            logs.add(it)
                        }
                    }
                }
                
                _logEntries.value = logs
                LogUtils.d(TAG, "Loaded ${logs.size} log entries")
            } catch (e: IOException) {
                LogUtils.e(TAG, "Error loading logs", e)
                _logEntries.value = listOf("Error loading logs: ${e.message}")
            }
        }
    }
    
    /**
     * Clears all app logs
     */
    fun clearLogs() {
        viewModelScope.launch {
            try {
                Runtime.getRuntime().exec("logcat -c")
                _logEntries.value = listOf("Logs cleared successfully")
                LogUtils.i(TAG, "Logs cleared")
            } catch (e: IOException) {
                LogUtils.e(TAG, "Error clearing logs", e)
                _logEntries.value = listOf("Error clearing logs: ${e.message}")
            }
        }
    }
    
    /**
     * Toggles offline mode simulation
     */
    fun setOfflineMode(enabled: Boolean) {
        _offlineModeEnabled.value = enabled
        LogUtils.i(TAG, "Offline mode ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Toggles rate limiting
     */
    fun setRateLimiting(enabled: Boolean) {
        _rateLimitingEnabled.value = enabled
        LogUtils.i(TAG, "Rate limiting ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Gets debug-related build info
     */
    fun getBuildInfo(): Map<String, String> {
        return mapOf(
            "Debug Mode" to com.example.safereach.BuildConfig.DEBUG.toString(),
            "Build Type" to com.example.safereach.BuildConfig.BUILD_TYPE,
            "Version Name" to com.example.safereach.BuildConfig.VERSION_NAME,
            "Version Code" to com.example.safereach.BuildConfig.VERSION_CODE.toString(),
            "Application ID" to com.example.safereach.BuildConfig.APPLICATION_ID
        )
    }
} 