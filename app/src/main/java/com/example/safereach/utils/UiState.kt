package com.example.safereach.utils

/**
 * Generic sealed class for UI state management
 */
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val exception: Throwable? = null) : UiState<Nothing>()
} 