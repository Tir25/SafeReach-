package com.example.safereach.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Sealed class to represent the result of an operation
 */
sealed class ResultWrapper<out T> {
    data class Success<out T>(val data: T) : ResultWrapper<T>()
    data class Error(val exception: Throwable, val message: String? = null) : ResultWrapper<Nothing>()
    object Loading : ResultWrapper<Nothing>()
    object Empty : ResultWrapper<Nothing>()
    
    /**
     * Check if the result is successful
     */
    fun isSuccess(): Boolean = this is Success
    
    /**
     * Check if the result is an error
     */
    fun isError(): Boolean = this is Error
    
    /**
     * Check if the result is loading
     */
    fun isLoading(): Boolean = this is Loading
    
    /**
     * Check if the result is empty
     */
    fun isEmpty(): Boolean = this is Empty
    
    /**
     * Get the data if the result is successful, otherwise null
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
    
    /**
     * Get the error message if the result is an error, otherwise null
     */
    fun getErrorMessage(): String? = when (this) {
        is Error -> message ?: exception.message
        else -> null
    }
}

/**
 * Transform a Flow<T> to Flow<ResultWrapper<T>> with proper error handling
 */
fun <T> Flow<T>.asResult(): Flow<ResultWrapper<T>> {
    return this
        .map<T, ResultWrapper<T>> { ResultWrapper.Success(it) }
        .onStart { emit(ResultWrapper.Loading) }
        .catch { emit(ResultWrapper.Error(it)) }
}

/**
 * Try to execute a suspend function with error handling
 * @param block The suspend function to execute
 * @return ResultWrapper<T> with the result of the operation
 */
suspend fun <T> safeCall(block: suspend () -> T): ResultWrapper<T> {
    return try {
        val result = block()
        if (result is Collection<*> && result.isEmpty()) {
            ResultWrapper.Empty
        } else {
            ResultWrapper.Success(result)
        }
    } catch (e: Exception) {
        LogUtils.e("SafeCall", "Error in safe call", e)
        ResultWrapper.Error(e)
    }
}

/**
 * Extension function to convert Result<T> to ResultWrapper<T>
 */
fun <T> Result<T>.toResultWrapper(): ResultWrapper<T> {
    return fold(
        onSuccess = { ResultWrapper.Success(it) },
        onFailure = { ResultWrapper.Error(it) }
    )
}

/**
 * Extension function to convert ResultWrapper<T> to Result<T>
 */
fun <T> ResultWrapper<T>.toResult(): Result<T> {
    return when (this) {
        is ResultWrapper.Success -> Result.success(data)
        is ResultWrapper.Error -> Result.failure(exception)
        is ResultWrapper.Loading, is ResultWrapper.Empty -> Result.failure(
            IllegalStateException("Cannot convert Loading or Empty to Result")
        )
    }
} 