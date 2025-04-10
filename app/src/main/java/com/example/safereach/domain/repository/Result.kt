package com.example.safereach.domain.repository

/**
 * A generic class that holds a value with its loading status.
 * @param <T> Type of the data contained in the result
 */
sealed class Result<out T> {
    /**
     * Represents successful operations with data
     */
    data class Success<out T>(val data: T) : Result<T>()
    
    /**
     * Represents failed operations with optional error message and exception
     */
    data class Error(
        val message: String, 
        val exception: Exception? = null
    ) : Result<Nothing>()
    
    /**
     * Represents loading state
     */
    data object Loading : Result<Nothing>()
    
    /**
     * Utility method to check if the result is a success
     */
    fun isSuccess(): Boolean = this is Success
    
    /**
     * Utility method to check if the result is an error
     */
    fun isError(): Boolean = this is Error
    
    /**
     * Utility method to check if the result is loading
     */
    fun isLoading(): Boolean = this is Loading
    
    /**
     * Get the data if available, otherwise null
     */
    fun getOrNull(): T? = if (this is Success) data else null
    
    /**
     * Get the data if available, otherwise default value
     */
    fun getOrDefault(defaultValue: @UnsafeVariance T): T = 
        if (this is Success) data else defaultValue
    
    /**
     * Map a Result<T> to Result<R> based on a transform function
     */
    fun <R> map(transform: (T) -> R): Result<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
            is Loading -> Loading
        }
    }
} 