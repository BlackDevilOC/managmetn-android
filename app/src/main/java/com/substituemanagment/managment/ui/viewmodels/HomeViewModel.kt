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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileReader
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
    val nextClassInfo: ClassInfo? = null,
    val isLoading: Boolean = false
)

// Simple timetable data structure for JSON parsing
data class TimetableEntry(
    val day: String = "",
    val period: Int = 0,
    val teacher: String = "",
    val subject: String = "",
    val room: String = "",
    val grade: String = ""
)

class HomeViewModel(private val context: Context) : ViewModel() {
    private val TAG = "HomeViewModel"
    private val teacherDataManager = TeacherDataManager(context)
    private val baseDir = File("/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data")
    private val processedDir = File(baseDir, "processed")
    private val timetableFile = File(processedDir, "timetable.json")
    
    private var timetableData: List<TimetableEntry> = emptyList()
    
    var state by mutableStateOf(HomeScreenState())
        private set
    
    init {
        loadTimetableData()
        refreshData()
        // Start periodic refresh
        viewModelScope.launch {
            while (true) {
                delay(60000) // Refresh every minute
                refreshData()
            }
        }
    }
    
    private fun loadTimetableData() {
        try {
            if (timetableFile.exists()) {
                val type = object : TypeToken<List<TimetableEntry>>() {}.type
                FileReader(timetableFile).use { reader ->
                    timetableData = Gson().fromJson(reader, type) ?: emptyList()
                }
                Log.d(TAG, "Loaded ${timetableData.size} timetable entries")
            } else {
                Log.w(TAG, "Timetable file not found at: ${timetableFile.absolutePath}")
                // Create a fallback timetable if file doesn't exist
                createFallbackTimetable()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading timetable data", e)
            // Create a fallback timetable if there's an error
            createFallbackTimetable()
        }
    }
    
    private fun createFallbackTimetable() {
        // Create sample data if the file doesn't exist or there's an error loading it
        val days = listOf("monday", "tuesday", "wednesday", "thursday", "friday")
        val subjects = listOf("Mathematics", "Physics", "Chemistry", "Biology", "English", "History", "Computer Science", "Physical Education")
        val teachers = listOf("Ms. Johnson", "Mr. Thomas", "Dr. Rodriguez", "Ms. Chen", "Ms. Williams", "Dr. Ahmed", "Mr. Patel", "Coach Davis")
        val rooms = listOf("Room 101", "Lab 201", "Lab 202", "Lab 203", "Room 104", "Room 105", "Computer Lab", "Gymnasium")
        val grades = listOf("Grade 9", "Grade 10", "Grade 11", "Grade 12")
        
        timetableData = buildList {
            days.forEach { day ->
                for (period in 1..8) {
                    val subjectIndex = (period - 1) % subjects.size
                    add(
                        TimetableEntry(
                            day = day,
                            period = period,
                            teacher = teachers[subjectIndex],
                            subject = subjects[subjectIndex],
                            room = rooms[subjectIndex],
                            grade = grades[(period - 1) % grades.size]
                        )
                    )
                }
            }
        }
        Log.d(TAG, "Created fallback timetable with ${timetableData.size} entries")
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
            
            // Get class information based on period
            val classInfo = getCurrentClassInfo(periodInfo.first, currentDayOfWeek.lowercase())
            val nextClassInfo = getCurrentClassInfo(periodInfo.third, currentDayOfWeek.lowercase())
            
            // Count total classes for today
            val todayClassesCount = timetableData.count { it.day.equals(currentDayOfWeek.lowercase(), ignoreCase = true) }
            
            state = state.copy(
                absentTeachersCount = absentTeachers.size,
                substitutionsCount = substitutionsCount,
                classesCount = todayClassesCount.takeIf { it > 0 } ?: 15, // Use fallback value if no data
                currentPeriod = periodInfo.first,
                currentPeriodTimeRange = periodInfo.second,
                nextPeriod = periodInfo.third,
                nextPeriodTimeRange = periodInfo.fourth,
                currentDayOfWeek = currentDayOfWeek,
                currentDate = currentDate,
                currentClassInfo = classInfo,
                nextClassInfo = nextClassInfo,
                isLoading = false
            )
        }
    }
    
    private fun getCurrentClassInfo(periodNumber: Int?, dayOfWeek: String): ClassInfo? {
        if (periodNumber == null) return null
        
        // Look up the timetable entry for the current day and period
        val entry = timetableData.find { 
            it.day.equals(dayOfWeek, ignoreCase = true) && it.period == periodNumber 
        }
        
        if (entry != null) {
            // Check if the teacher is absent and has a substitute assigned
            val isTeacherAbsent = teacherDataManager.isTeacherAbsent(entry.teacher)
            val hasSubstitute = if (isTeacherAbsent) teacherDataManager.isTeacherAssigned(entry.teacher) else false
            
            // Find substitute teacher name if one is assigned
            val substituteTeacher = if (hasSubstitute) {
                // This is a simplified version - in a real app, you'd look up the actual substitute teacher name
                // from your substitute assignments database
                "Substitute Teacher"
            } else null
            
            return ClassInfo(
                subject = entry.subject,
                grade = entry.grade,
                teacherName = entry.teacher,
                room = entry.room,
                isSubstituted = hasSubstitute,
                substituteTeacher = substituteTeacher
            )
        } else {
            // Fallback to default data if no timetable entry is found
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