package com.example.safereach.presentation.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safereach.domain.repository.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * Base ViewModel class that provides common functionality for all ViewModels
 */
abstract class BaseViewModel : ViewModel() {
    
    /**
     * Helper method to handle Flow<Result<T>> and convert it to StateFlow<UiState<T>>
     */
    protected fun <T> Flow<Result<T>>.asUiStateFlow(): Flow<UiState<T>> {
        return this
            .onStart { emit(Result.Loading) }
            .map { result ->
                when (result) {
                    is Result.Success -> UiState.Success(result.data)
                    is Result.Error -> UiState.Error(result.message)
                    Result.Loading -> UiState.Loading
                }
            }
            .catch { e ->
                emit(UiState.Error(e.message ?: "Unknown error occurred"))
            }
    }
    
    /**
     * Helper method to create a MutableStateFlow with Loading state
     */
    protected fun <T> createMutableStateFlow(): MutableStateFlow<UiState<T>> {
        return MutableStateFlow(UiState.Loading)
    }
    
    /**
     * Helper method to launch a coroutine in viewModelScope with error handling
     */
    protected fun launchWithErrorHandling(
        onError: (Exception) -> Unit = {},
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
} 