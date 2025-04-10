package com.example.safereach.utils

import android.util.Log
import com.example.safereach.BuildConfig

/**
 * Utility class for standardized logging across the app
 * Logs will only be visible in debug builds for security
 */
object LogUtils {
    private const val DEFAULT_TAG = "SafeReach"
    
    // Log levels
    enum class Level {
        VERBOSE, DEBUG, INFO, WARN, ERROR
    }
    
    /**
     * Log a verbose message
     */
    fun v(tag: String = DEFAULT_TAG, message: String) {
        if (BuildConfig.DEBUG) {
            Log.v(tag, message)
        }
    }
    
    /**
     * Log a debug message
     */
    fun d(tag: String = DEFAULT_TAG, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }
    
    /**
     * Log an info message
     */
    fun i(tag: String = DEFAULT_TAG, message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message)
        }
    }
    
    /**
     * Log a warning message
     */
    fun w(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            if (throwable != null) {
                Log.w(tag, message, throwable)
            } else {
                Log.w(tag, message)
            }
        }
    }
    
    /**
     * Log an error message
     */
    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        }
    }
    
    /**
     * Log a function entry
     */
    fun functionEntry(tag: String, functionName: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, "⏩ Entering $functionName")
        }
    }
    
    /**
     * Log a function exit
     */
    fun functionExit(tag: String, functionName: String, result: String? = null) {
        if (BuildConfig.DEBUG) {
            val message = if (result != null) {
                "⏪ Exiting $functionName with result: $result"
            } else {
                "⏪ Exiting $functionName"
            }
            Log.d(tag, message)
        }
    }
    
    /**
     * Log a network request
     */
    fun networkRequest(tag: String, endpoint: String, params: Map<String, Any>? = null) {
        if (BuildConfig.DEBUG) {
            val paramsStr = params?.entries?.joinToString { "${it.key}=${it.value}" } ?: "none"
            Log.d(tag, "📡 Request to $endpoint, params: $paramsStr")
        }
    }
    
    /**
     * Log a network response
     */
    fun networkResponse(tag: String, endpoint: String, success: Boolean, message: String) {
        if (BuildConfig.DEBUG) {
            val icon = if (success) "✅" else "❌"
            Log.d(tag, "$icon Response from $endpoint: $message")
        }
    }
    
    /**
     * Log a repository action
     */
    fun repository(tag: String, action: String, details: String? = null) {
        if (BuildConfig.DEBUG) {
            val message = if (details != null) {
                "🗄️ Repository $action: $details"
            } else {
                "🗄️ Repository $action"
            }
            Log.d(tag, message)
        }
    }
    
    /**
     * Log a ViewModel action
     */
    fun viewModel(tag: String, action: String, details: String? = null) {
        if (BuildConfig.DEBUG) {
            val message = if (details != null) {
                "🧠 ViewModel $action: $details"
            } else {
                "🧠 ViewModel $action"
            }
            Log.d(tag, message)
        }
    }
    
    /**
     * Log a permission check
     */
    fun permission(tag: String, permission: String, granted: Boolean) {
        if (BuildConfig.DEBUG) {
            val icon = if (granted) "✅" else "❌"
            Log.d(tag, "$icon Permission $permission: ${if (granted) "granted" else "denied"}")
        }
    }
    
    /**
     * Log location updates
     */
    fun location(tag: String, latitude: Double, longitude: Double, accuracy: Float? = null) {
        if (BuildConfig.DEBUG) {
            val message = if (accuracy != null) {
                "📍 Location update: $latitude, $longitude (accuracy: $accuracy)"
            } else {
                "📍 Location update: $latitude, $longitude"
            }
            Log.d(tag, message)
        }
    }
    
    /**
     * Log alert triggers
     */
    fun alert(tag: String, alertType: String, success: Boolean, details: String? = null) {
        if (BuildConfig.DEBUG) {
            val icon = if (success) "🚨" else "❌"
            val message = if (details != null) {
                "$icon Alert $alertType: $details"
            } else {
                "$icon Alert $alertType"
            }
            Log.d(tag, message)
        }
    }
} 