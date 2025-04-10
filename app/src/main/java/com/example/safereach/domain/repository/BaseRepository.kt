package com.example.safereach.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Base repository interface for implementing the repository pattern
 */
interface BaseRepository<T, in Params> {
    /**
     * Gets a data from the repository as a Flow
     */
    fun getAsFlow(params: Params? = null): Flow<Result<T>>
    
    /**
     * Gets a data from the repository
     */
    suspend fun get(params: Params? = null): Result<T>
    
    /**
     * Creates or updates data in the repository
     */
    suspend fun save(data: T): Result<T>
    
    /**
     * Deletes data from the repository
     */
    suspend fun delete(data: T): Result<Boolean>
} 