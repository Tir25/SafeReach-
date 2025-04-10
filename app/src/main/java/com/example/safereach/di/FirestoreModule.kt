package com.example.safereach.di

import com.example.safereach.data.repository.FirestoreUserRepository
import com.example.safereach.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing Firestore repositories
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FirestoreModule {
    
    @Binds
    @Singleton
    abstract fun bindUserRepository(
        firestoreUserRepository: FirestoreUserRepository
    ): UserRepository
} 