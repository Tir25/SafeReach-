package com.example.safereach.utils

import java.util.concurrent.TimeUnit

object TimeUtils {
    /**
     * Formats milliseconds into a readable MM:SS format
     * @param millis time in milliseconds
     * @return formatted string in MM:SS format
     */
    fun formatCooldownTime(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(millis)
        val seconds = totalSeconds - (minutes * 60)
        
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    /**
     * Formats milliseconds into a readable text description
     * @param millis time in milliseconds
     * @return user-friendly string like "5 minutes 30 seconds"
     */
    fun formatCooldownTimeText(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(millis)
        val seconds = totalSeconds - (minutes * 60)
        
        return when {
            minutes > 0 && seconds > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} and $seconds second${if (seconds > 1) "s" else ""}"
            minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""}"
            else -> "$seconds second${if (seconds > 1) "s" else ""}"
        }
    }
} 