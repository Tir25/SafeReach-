package com.example.safereach.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for SafeReach app, currently only contains offline alerts
 */
@Database(
    entities = [OfflineAlert::class],
    version = 1,
    exportSchema = false
)
abstract class SafeReachDatabase : RoomDatabase() {
    
    /**
     * DAO for accessing offline alerts
     */
    abstract fun alertDao(): AlertDao
    
    companion object {
        private const val DATABASE_NAME = "safereach-db"
        
        @Volatile
        private var INSTANCE: SafeReachDatabase? = null
        
        /**
         * Get the database instance, creating it if it doesn't exist
         */
        fun getInstance(context: Context): SafeReachDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SafeReachDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration()
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
} 