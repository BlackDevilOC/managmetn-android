package com.substituemanagment.managment.ui.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.substituemanagment.managment.data.PeriodSettings
import com.substituemanagment.managment.data.TeacherDataManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class HomeScreenState(
    val absentTeachersCount: Int = 0,
    val substitutionsCount: Int = 0,
    val classesCount: Int = 15, // Default value or could be calculated
    val currentPeriod: Int? = null,
    val currentPeriodTimeRange: String = "",
    val nextPeriod: Int? = null,
    val nextPeriodTimeRange: String = "",
    val isLoading: Boolean = false
)

class HomeViewModel(private val context: Context) : ViewModel() {
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
            
            state = state.copy(
                absentTeachersCount = absentTeachers.size,
                substitutionsCount = substitutionsCount,
                currentPeriod = periodInfo.first,
                currentPeriodTimeRange = periodInfo.second,
                nextPeriod = periodInfo.third,
                nextPeriodTimeRange = periodInfo.fourth,
                isLoading = false
            )
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
            currentPeriod?.formatTimeRange() ?: "",
            nextPeriod?.periodNumber,
            nextPeriod?.formatTimeRange() ?: ""
        )
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