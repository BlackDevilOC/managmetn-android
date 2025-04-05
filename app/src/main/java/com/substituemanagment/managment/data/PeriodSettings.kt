package com.substituemanagment.managment.data

import android.content.Context
import com.google.gson.Gson
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
        return "$startTime - $endTime"
    }
}

object PeriodSettings {
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
        val baseDir = File(context.getExternalFilesDir(null), "substitute_data")
        val processedDir = File(baseDir, "processed")
        val settingsFile = File(processedDir, SETTINGS_FILE_NAME)
        
        return if (settingsFile.exists()) {
            try {
                val gson = Gson()
                val type = object : TypeToken<List<PeriodSetting>>() {}.type
                gson.fromJson(settingsFile.readText(), type)
            } catch (e: Exception) {
                // If there's an error reading the file, use default periods
                DEFAULT_PERIODS
            }
        } else {
            // If file doesn't exist, create it with default periods
            savePeriodsSettings(context, DEFAULT_PERIODS)
            DEFAULT_PERIODS
        }
    }
    
    fun savePeriodsSettings(context: Context, periods: List<PeriodSetting>): Boolean {
        val baseDir = File(context.getExternalFilesDir(null), "substitute_data")
        val processedDir = File(baseDir, "processed")
        if (!processedDir.exists()) {
            processedDir.mkdirs()
        }
        
        val settingsFile = File(processedDir, SETTINGS_FILE_NAME)
        return try {
            val gson = Gson()
            val json = gson.toJson(periods)
            settingsFile.writeText(json)
            true
        } catch (e: IOException) {
            false
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