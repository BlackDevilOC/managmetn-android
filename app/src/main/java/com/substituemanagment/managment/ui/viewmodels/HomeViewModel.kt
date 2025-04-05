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
import java.time.DayOfWeek
import java.time.LocalDate

data class ClassInfo(
    val subject: String = "Mathematics",
    val grade: String = "Grade 9",
    val teacherName: String = "Ms. Johnson",
    val room: String = "Room 101",
    val isSubstituted: Boolean = false,
    val substituteTeacher: String? = null
)

data class HomeScreenState(
    val absentTeachersCount: Int = 0,
    val substitutionsCount: Int = 0,
    val classesCount: Int = 15, // Default value or could be calculated
    val currentPeriod: Int? = null,
    val currentPeriodTimeRange: String = "",
    val nextPeriod: Int? = null,
    val nextPeriodTimeRange: String = "",
    val currentDayOfWeek: String = "",
    val currentDate: String = "",
    val currentClassInfo: ClassInfo? = null,
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
            
            // Get current day and date
            val today = LocalDate.now()
            val currentDayOfWeek = today.dayOfWeek.toString().lowercase().capitalize()
            val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
            val currentDate = today.format(dateFormatter)
            
            // Get current period information
            val periodInfo = getCurrentPeriodInfo()
            
            // Get current class information based on period
            val classInfo = getCurrentClassInfo(periodInfo.first, currentDayOfWeek.lowercase())
            
            state = state.copy(
                absentTeachersCount = absentTeachers.size,
                substitutionsCount = substitutionsCount,
                currentPeriod = periodInfo.first,
                currentPeriodTimeRange = periodInfo.second,
                nextPeriod = periodInfo.third,
                nextPeriodTimeRange = periodInfo.fourth,
                currentDayOfWeek = currentDayOfWeek,
                currentDate = currentDate,
                currentClassInfo = classInfo,
                isLoading = false
            )
        }
    }
    
    private fun getCurrentClassInfo(periodNumber: Int?, dayOfWeek: String): ClassInfo? {
        if (periodNumber == null) return null
        
        // Get class information from timetable data for the current period
        // This is a simplified example - in a real app you would query your timetable database
        
        // Simulate different classes for different periods to demonstrate dynamic content
        val subjectsByPeriod = mapOf(
            1 to "Mathematics",
            2 to "Physics",
            3 to "Chemistry",
            4 to "Biology",
            5 to "English",
            6 to "History",
            7 to "Computer Science",
            8 to "Physical Education"
        )
        
        val teachersBySubject = mapOf(
            "Mathematics" to "Ms. Johnson",
            "Physics" to "Mr. Thomas",
            "Chemistry" to "Dr. Rodriguez",
            "Biology" to "Ms. Chen",
            "English" to "Ms. Williams",
            "History" to "Dr. Ahmed",
            "Computer Science" to "Mr. Patel",
            "Physical Education" to "Coach Davis"
        )
        
        val roomsByPeriod = mapOf(
            1 to "Room 101",
            2 to "Lab 201",
            3 to "Lab 202",
            4 to "Lab 203",
            5 to "Room 104",
            6 to "Room 105",
            7 to "Computer Lab",
            8 to "Gymnasium"
        )
        
        val subject = subjectsByPeriod[periodNumber] ?: "Unknown Subject"
        val teacherName = teachersBySubject[subject] ?: "Unknown Teacher"
        val room = roomsByPeriod[periodNumber] ?: "Room Unknown"
        
        // Check if the teacher is absent and has a substitute assigned
        val isTeacherAbsent = teacherDataManager.isTeacherAbsent(teacherName)
        val hasSubstitute = if (isTeacherAbsent) teacherDataManager.isTeacherAssigned(teacherName) else false
        
        return ClassInfo(
            subject = subject,
            grade = "Grade ${9 + (periodNumber % 3)}", // Just a demo variation
            teacherName = teacherName,
            room = room,
            isSubstituted = hasSubstitute,
            substituteTeacher = if (hasSubstitute) "Mr. Smith" else null
        )
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
        
        // Find next period - only consider active periods
        val nextPeriod = if (currentPeriod != null) {
            periods.filter { it.isActive && it.periodNumber > currentPeriod.periodNumber }
                .minByOrNull { it.periodNumber }
        } else {
            // If no current period, find the next upcoming period today
            periods.filter { 
                it.isActive && now.isBefore(it.getStartLocalTime()) 
            }.minByOrNull { it.getStartLocalTime() }
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

// Extension function to capitalize the first letter of a string
private fun String.capitalize(): String {
    return this.replaceFirstChar { it.uppercase() }
} 