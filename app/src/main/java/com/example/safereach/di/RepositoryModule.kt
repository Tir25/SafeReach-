package com.example.safereach.di

import com.example.safereach.data.repository.AlertRepositoryImpl
import com.example.safereach.domain.repository.AlertRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing repository implementations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    /**
     * Binds the AlertRepositoryImpl implementation to the AlertRepository interface
     */
    @Binds
    @Singleton
    abstract fun bindAlertRepository(
        alertRepositoryImpl: AlertRepositoryImpl
    ): AlertRepository
} 