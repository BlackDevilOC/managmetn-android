package com.substituemanagment.managment.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.substituemanagment.managment.data.PeriodSettings
import com.substituemanagment.managment.data.TeacherDataManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// For teacher schedule display
data class TeacherScheduleEntry(
    val teacherName: String,
    val className: String,
    val period: Int
)

data class HomeScreenState(
    val absentTeachersCount: Int = 0,
    val substitutionsCount: Int = 0,
    val classesCount: Int = 15, // Default value or could be calculated
    val currentPeriod: Int? = null,
    val currentPeriodTimeRange: String = "",
    val nextPeriod: Int? = null,
    val nextPeriodTimeRange: String = "",
    val currentSchedules: List<TeacherScheduleEntry> = emptyList(),
    val isLoading: Boolean = false
)

class HomeViewModel(private val context: Context) : ViewModel() {
    private val TAG = "HomeViewModel"
    private val teacherDataManager = TeacherDataManager(context)
    
    var state by mutableStateOf(HomeScreenState())
        private set
    
    init {
        refreshData()
        // Start periodic refresh
        viewModelScope.launch {
            while (true) {
                delay(60000) // Refresh every minute
                refreshData()
            }
        }
    }
    
    fun refreshData() {
        state = state.copy(isLoading = true)
        
        viewModelScope.launch {
            // Get absent teachers count
            val absentTeachers = teacherDataManager.getAbsentTeachers()
            val substitutionsCount = absentTeachers.count { it.assignedSubstitute }
            
            // Get current period information
            val periodInfo = getCurrentPeriodInfo()
            
            // Get current teacher schedules if we have a current period
            val currentSchedules = if (periodInfo.first != null) {
                fetchCurrentPeriodSchedules(periodInfo.first)
            } else {
                emptyList()
            }
            
            state = state.copy(
                absentTeachersCount = absentTeachers.size,
                substitutionsCount = substitutionsCount,
                currentPeriod = periodInfo.first,
                currentPeriodTimeRange = periodInfo.second,
                nextPeriod = periodInfo.third,
                nextPeriodTimeRange = periodInfo.fourth,
                currentSchedules = currentSchedules,
                isLoading = false
            )
        }
    }
    
    private suspend fun fetchCurrentPeriodSchedules(currentPeriod: Int): List<TeacherScheduleEntry> {
        return withContext(Dispatchers.IO) {
            try {
                val today = PeriodSettings.getCurrentDay()
                val baseDir = File("/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data")
                val processedDir = File(baseDir, "processed")
                val daySchedulesFile = File(processedDir, "day_schedules.json")
                
                if (!daySchedulesFile.exists()) {
                    Log.e(TAG, "Day schedules file not found at: ${daySchedulesFile.absolutePath}")
                    return@withContext emptyList<TeacherScheduleEntry>()
                }
                
                // Read the JSON file
                val jsonContent = daySchedulesFile.readText()
                val gson = Gson()
                
                // Define the type for the JSON structure
                val type = object : TypeToken<Map<String, Map<String, List<Map<String, Any>>>>>() {}.type
                val daySchedules: Map<String, Map<String, List<Map<String, Any>>>> = gson.fromJson(jsonContent, type)
                
                // Get today's schedule
                val todaySchedule = daySchedules[today] ?: return@withContext emptyList<TeacherScheduleEntry>()
                
                // Get current period's schedule
                val periodSchedule = todaySchedule[currentPeriod.toString()] ?: return@withContext emptyList<TeacherScheduleEntry>()
                
                // Convert to our model
                return@withContext periodSchedule.map { entry ->
                    TeacherScheduleEntry(
                        teacherName = entry["teacherName"] as String,
                        className = entry["className"] as String,
                        period = (entry["period"] as Number).toInt()
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching current period schedules", e)
                return@withContext emptyList<TeacherScheduleEntry>()
            }
        }
    }
    
    private fun getCurrentPeriodInfo(): Quadruple<Int?, String, Int?, String> {
        val periods = PeriodSettings.getPeriodsSettings(context)
        val now = LocalTime.now()
        
        // Find current period
        val currentPeriod = periods.find { period ->
            period.isActive && 
            now.isAfter(period.getStartLocalTime()) && 
            now.isBefore(period.getEndLocalTime())
        }
        
        // Find next period
        val nextPeriod = if (currentPeriod != null) {
            periods.filter { it.periodNumber > currentPeriod.periodNumber && it.isActive }
                .minByOrNull { it.periodNumber }
        } else {
            // If no current period, find the next upcoming period
            periods.filter { it.isActive && now.isBefore(it.getStartLocalTime()) }
                .minByOrNull { it.getStartLocalTime() }
        }
        
        return Quadruple(
            currentPeriod?.periodNumber,
            formatTimeRange(currentPeriod?.startTime, currentPeriod?.endTime),
            nextPeriod?.periodNumber,
            formatTimeRange(nextPeriod?.startTime, nextPeriod?.endTime)
        )
    }
    
    // Format time range in 12-hour format
    private fun formatTimeRange(startTime: String?, endTime: String?): String {
        if (startTime == null || endTime == null) return ""
        
        try {
            val startLocalTime = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"))
            val endLocalTime = LocalTime.parse(endTime, DateTimeFormatter.ofPattern("HH:mm"))
            
            val formatter = DateTimeFormatter.ofPattern("h:mm a")
            val formattedStart = startLocalTime.format(formatter)
            val formattedEnd = endLocalTime.format(formatter)
            
            return "$formattedStart - $formattedEnd"
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting time range", e)
            return "$startTime - $endTime"
        }
    }
    
    // Helper class for returning four values
    data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )
    
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
} 