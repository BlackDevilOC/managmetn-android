package com.substituemanagment.managment.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.IOException
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class PeriodSetting(
    val periodNumber: Int,
    val startTime: String, // Format: HH:mm
    val endTime: String,   // Format: HH:mm
    val isActive: Boolean = true
) {
    fun getStartLocalTime(): LocalTime {
        return LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"))
    }
    
    fun getEndLocalTime(): LocalTime {
        return LocalTime.parse(endTime, DateTimeFormatter.ofPattern("HH:mm"))
    }
    
    fun isCurrentlyActive(): Boolean {
        val now = LocalTime.now()
        val start = getStartLocalTime()
        val end = getEndLocalTime()
        return isActive && now.isAfter(start) && now.isBefore(end)
    }
    
    fun formatTimeRange(): String {
        try {
            val startLocalTime = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"))
            val endLocalTime = LocalTime.parse(endTime, DateTimeFormatter.ofPattern("HH:mm"))
            
            val formatter = DateTimeFormatter.ofPattern("h:mm a")
            return "${startLocalTime.format(formatter)} - ${endLocalTime.format(formatter)}"
        } catch (e: Exception) {
            Log.e("PeriodSetting", "Error formatting time range", e)
            return "$startTime - $endTime" // Fallback to original format
        }
    }
}

object PeriodSettings {
    private const val TAG = "PeriodSettings"
    private const val SETTINGS_FILE_NAME = "period_settings.json"
    private const val EXTERNAL_STORAGE_BASE_PATH = "/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data"
    
    private val DEFAULT_PERIODS = listOf(
        PeriodSetting(1, "08:00", "08:45"),
        PeriodSetting(2, "08:50", "09:35"),
        PeriodSetting(3, "09:40", "10:25"),
        PeriodSetting(4, "10:30", "11:15"),
        PeriodSetting(5, "11:20", "12:05"),
        PeriodSetting(6, "12:10", "12:55"),
        PeriodSetting(7, "13:30", "14:15"),
        PeriodSetting(8, "14:20", "15:05")
    )
    
    fun getPeriodsSettings(context: Context): List<PeriodSetting> {
        // First, try to load from the external storage path
        val processedDir = File("$EXTERNAL_STORAGE_BASE_PATH/processed")
        val externalSettingsFile = File(processedDir, SETTINGS_FILE_NAME)
        
        // Alternative path using Context
        val baseDir = File(context.getExternalFilesDir(null), "substitute_data")
        val contextProcessedDir = File(baseDir, "processed")
        val contextSettingsFile = File(contextProcessedDir, SETTINGS_FILE_NAME)
        
        Log.d(TAG, "Looking for period settings at: ${externalSettingsFile.absolutePath}")
        Log.d(TAG, "Alternative path: ${contextSettingsFile.absolutePath}")
        
        return when {
            // Try the direct external storage path first
            externalSettingsFile.exists() -> {
                try {
                    Log.d(TAG, "Loading from external storage path")
                    val gson = GsonBuilder().setPrettyPrinting().create()
                    val type = object : TypeToken<List<PeriodSetting>>() {}.type
                    gson.fromJson(externalSettingsFile.readText(), type)
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading from external storage path", e)
                    // If there's an error, try the context path
                    loadFromContextPath(contextSettingsFile)
                }
            }
            // If external path doesn't exist, try the context path
            contextSettingsFile.exists() -> {
                loadFromContextPath(contextSettingsFile)
            }
            // If no files exist, create them with default periods
            else -> {
                Log.d(TAG, "No settings file found, using defaults")
                // Create both directories if they don't exist
                processedDir.mkdirs()
                contextProcessedDir.mkdirs()
                
                // Save to both locations for robustness
                savePeriodsSettings(context, DEFAULT_PERIODS)
                DEFAULT_PERIODS
            }
        }
    }
    
    private fun loadFromContextPath(file: File): List<PeriodSetting> {
        return try {
            Log.d(TAG, "Loading from context path")
            val gson = GsonBuilder().setPrettyPrinting().create()
            val type = object : TypeToken<List<PeriodSetting>>() {}.type
            gson.fromJson(file.readText(), type)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading settings file", e)
            DEFAULT_PERIODS
        }
    }
    
    fun savePeriodsSettings(context: Context, periods: List<PeriodSetting>): Boolean {
        // Save to both locations for robustness
        val externalSuccess = saveToExternalPath(periods)
        val contextSuccess = saveToContextPath(context, periods)
        
        return externalSuccess || contextSuccess // Return true if at least one save was successful
    }
    
    private fun saveToExternalPath(periods: List<PeriodSetting>): Boolean {
        val processedDir = File("$EXTERNAL_STORAGE_BASE_PATH/processed")
        if (!processedDir.exists()) {
            processedDir.mkdirs()
        }
        
        val settingsFile = File(processedDir, SETTINGS_FILE_NAME)
        return try {
            val gson = GsonBuilder().setPrettyPrinting().create()
            val json = gson.toJson(periods)
            settingsFile.writeText(json)
            Log.d(TAG, "Successfully saved to external path: ${settingsFile.absolutePath}")
            
            // Create a backup
            createBackup(settingsFile)
            
            true
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save to external path", e)
            false
        }
    }
    
    private fun saveToContextPath(context: Context, periods: List<PeriodSetting>): Boolean {
        val baseDir = File(context.getExternalFilesDir(null), "substitute_data")
        val processedDir = File(baseDir, "processed")
        if (!processedDir.exists()) {
            processedDir.mkdirs()
        }
        
        val settingsFile = File(processedDir, SETTINGS_FILE_NAME)
        return try {
            val gson = GsonBuilder().setPrettyPrinting().create()
            val json = gson.toJson(periods)
            settingsFile.writeText(json)
            Log.d(TAG, "Successfully saved to context path: ${settingsFile.absolutePath}")
            true
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save to context path", e)
            false
        }
    }
    
    private fun createBackup(originalFile: File) {
        try {
            val backupDir = File("$EXTERNAL_STORAGE_BASE_PATH/backups/period_settings")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            
            // Create timestamped backup
            val timestamp = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
            )
            val backupFile = File(backupDir, "period_settings_$timestamp.json")
            
            originalFile.copyTo(backupFile, overwrite = true)
            Log.d(TAG, "Created backup at: ${backupFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create backup", e)
        }
    }
    
    fun getCurrentPeriod(context: Context): Int? {
        val periods = getPeriodsSettings(context)
        val now = LocalTime.now()
        
        for (period in periods) {
            if (period.isActive) {
                val start = period.getStartLocalTime()
                val end = period.getEndLocalTime()
                if (now.isAfter(start) && now.isBefore(end)) {
                    return period.periodNumber
                }
            }
        }
        return null // No current period
    }
    
    fun getCurrentDay(): String {
        val today = java.time.LocalDate.now().dayOfWeek.name.lowercase()
        // Convert to format used in the app (e.g., "monday", "tuesday", etc.)
        return today
    }
} 