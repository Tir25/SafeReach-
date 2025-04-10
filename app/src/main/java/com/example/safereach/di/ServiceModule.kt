package com.example.safereach.di

import com.example.safereach.data.service.FakeEmergencyService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides service-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    
    /**
     * Provides a singleton instance of FakeEmergencyService
     */
    @Provides
    @Singleton
    fun provideFakeEmergencyService(): FakeEmergencyService {
        return FakeEmergencyService()
    }
} 