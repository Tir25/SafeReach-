package com.example.safereach.presentation.base

/**
 * Represents UI state for screens
 */
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
    
    /**
     * Helper functions to check the current state
     */
    val isLoading get() = this is Loading
    val isSuccess get() = this is Success
    val isError get() = this is Error
    
    /**
     * Get the data if the state is Success, otherwise return null
     */
    fun getSuccessDataOrNull(): T? = if (this is Success) data else null
} 