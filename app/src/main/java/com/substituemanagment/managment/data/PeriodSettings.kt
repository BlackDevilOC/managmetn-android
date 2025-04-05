package com.substituemanagment.managment.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
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
        return "$startTime - $endTime"
    }
}

object PeriodSettings {
    private const val TAG = "PeriodSettings"
    private const val SETTINGS_FILE_NAME = "period_settings.json"
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
        // Use the standard data directory where other timetable files are stored
        val baseDir = File("/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data")
        val processedDir = File(baseDir, "processed")
        val settingsFile = File(processedDir, SETTINGS_FILE_NAME)
        
        Log.d(TAG, "Looking for period settings file at: ${settingsFile.absolutePath}")
        
        return if (settingsFile.exists()) {
            try {
                val gson = Gson()
                val type = object : TypeToken<List<PeriodSetting>>() {}.type
                val periods = gson.fromJson<List<PeriodSetting>>(FileReader(settingsFile), type)
                Log.d(TAG, "Successfully loaded ${periods.size} periods from settings file")
                periods
            } catch (e: Exception) {
                Log.e(TAG, "Error reading period settings file", e)
                // If there's an error reading the file, use default periods
                savePeriodsSettings(context, DEFAULT_PERIODS) // Try to recreate the file
                DEFAULT_PERIODS
            }
        } else {
            Log.d(TAG, "Period settings file not found. Creating with default values.")
            // If file doesn't exist, create it with default periods
            savePeriodsSettings(context, DEFAULT_PERIODS)
            DEFAULT_PERIODS
        }
    }
    
    fun savePeriodsSettings(context: Context, periods: List<PeriodSetting>): Boolean {
        val baseDir = File("/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data")
        val processedDir = File(baseDir, "processed")
        
        try {
            // Ensure directories exist
            if (!baseDir.exists()) {
                Log.d(TAG, "Creating base directory: ${baseDir.absolutePath}")
                baseDir.mkdirs()
            }
            
            if (!processedDir.exists()) {
                Log.d(TAG, "Creating processed directory: ${processedDir.absolutePath}")
                processedDir.mkdirs()
            }
            
            val settingsFile = File(processedDir, SETTINGS_FILE_NAME)
            
            val gson = Gson()
            val json = gson.toJson(periods)
            
            FileOutputStream(settingsFile).use { outputStream ->
                outputStream.write(json.toByteArray())
            }
            
            Log.d(TAG, "Successfully saved ${periods.size} periods to ${settingsFile.absolutePath}")
            return true
        } catch (e: IOException) {
            Log.e(TAG, "Error saving period settings", e)
            return false
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