package com.example.safereach.di

import android.content.Context
import com.example.safereach.data.local.database.AlertDao
import com.example.safereach.data.local.database.SafeReachDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * Provides the Room database instance
     */
    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): SafeReachDatabase {
        return SafeReachDatabase.getInstance(context)
    }
    
    /**
     * Provides the DAO for offline alerts
     */
    @Singleton
    @Provides
    fun provideAlertDao(database: SafeReachDatabase): AlertDao {
        return database.alertDao()
    }
} 