package com.substituemanagment.managment.data.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.substituemanagment.managment.data.AbsentTeacherData
import com.substituemanagment.managment.data.TeacherData
import com.substituemanagment.managment.data.models.SubstituteAssignment
import com.substituemanagment.managment.data.models.Teacher
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit

/**
 * ReportManager is responsible for generating periodic reports and backups
 * of teacher attendance and substitute assignments.
 * Reports are generated in CSV format and organized by year/month
 */
class ReportManager(private val context: Context) {
    private val TAG = "ReportManager"
    private val gson = Gson()
    
    // Base directory for all application data
    private val baseDir = File(context.getExternalFilesDir(null), "substitute_data")
    
    // Directory for reports organized by year/month
    private val reportsDir = File(baseDir, "reports")
    
    // Directory for error logs
    private val errorLogsDir = File(baseDir, "error_logs")
    
    // Default backup interval: 30 minutes
    private val backupIntervalMinutes = 30L
    
    // Shared preferences for tracking teacher data
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "ReportManagerPrefs", Context.MODE_PRIVATE
    )
    
    private var timer: Timer? = null
    private var isRunning = false
    
    // Keep track of last known teacher count
    private var lastKnownTeacherCount = prefs.getInt("last_known_teacher_count", 0)
    
    init {
        // Create necessary directories if they don't exist
        listOf(baseDir, reportsDir, errorLogsDir).forEach { dir ->
            if (!dir.exists()) {
                val created = dir.mkdirs()
                Log.d(TAG, "Creating directory ${dir.absolutePath}: $created")
            }
        }
    }
    
    /**
     * Start the report generation and backup service
     */
    fun startReportService() {
        if (isRunning) return
        
        isRunning = true
        timer = Timer()
        
        // Schedule the task to run periodically
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                try {
                    generateReports()
                } catch (e: Exception) {
                    // Log any errors that occur during report generation
                    Log.e(TAG, "Error in scheduled report generation", e)
                    logError("Scheduled report generation error: ${e.message}")
                }
            }
        }, 0, TimeUnit.MINUTES.toMillis(backupIntervalMinutes))
        
        Log.d(TAG, "Report service started with interval of $backupIntervalMinutes minutes")
    }
    
    /**
     * Stop the report generation and backup service
     */
    fun stopReportService() {
        timer?.cancel()
        timer = null
        isRunning = false
        Log.d(TAG, "Report service stopped")
    }
    
    /**
     * Generate reports immediately (can be called manually)
     */
    fun generateReports() {
        try {
            Log.d(TAG, "Generating reports...")
            
            // Check for data consistency and new teachers before generating reports
            checkForNewTeachers()
            
            // Create year/month directory structure
            val now = LocalDateTime.now()
            val year = now.year.toString()
            val month = now.month.toString()
            
            val yearDir = File(reportsDir, year)
            if (!yearDir.exists()) yearDir.mkdirs()
            
            // Generate attendance report
            val attendanceSuccess = generateAttendanceReport(yearDir, month, now)
            
            // Generate substitute assignments report
            val substituteSuccess = generateSubstituteReport(yearDir, month, now)
            
            if (attendanceSuccess || substituteSuccess) {
                Log.d(TAG, "Reports generation completed successfully")
            } else {
                Log.w(TAG, "No reports were generated - no data available")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating reports", e)
            logError("Report generation error: ${e.message}")
            
            // Try to recover by ensuring directories exist
            try {
                listOf(baseDir, reportsDir, errorLogsDir).forEach { dir ->
                    if (!dir.exists()) dir.mkdirs()
                }
            } catch (dirEx: Exception) {
                Log.e(TAG, "Failed to recover by creating directories", dirEx)
            }
        }
    }
    
    /**
     * Check if new teachers have been added to the system
     * and log this information
     */
    private fun checkForNewTeachers() {
        try {
            val allTeachers = getAllTeachers()
            val currentTeacherCount = allTeachers.size
            
            if (currentTeacherCount > lastKnownTeacherCount && lastKnownTeacherCount > 0) {
                // New teachers have been added
                val newTeacherCount = currentTeacherCount - lastKnownTeacherCount
                Log.i(TAG, "Detected $newTeacherCount new teachers added to the system")
                
                // Create a special report for new teachers
                generateNewTeachersReport(allTeachers, newTeacherCount)
                
                // Update the last known count
                prefs.edit().putInt("last_known_teacher_count", currentTeacherCount).apply()
                lastKnownTeacherCount = currentTeacherCount
            } else if (lastKnownTeacherCount == 0 && currentTeacherCount > 0) {
                // First time seeing teachers, just record the count
                prefs.edit().putInt("last_known_teacher_count", currentTeacherCount).apply()
                lastKnownTeacherCount = currentTeacherCount
                Log.i(TAG, "Initial teacher count recorded: $currentTeacherCount")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for new teachers", e)
            logError("Error checking for new teachers: ${e.message}")
        }
    }
    
    /**
     * Generate a special report when new teachers are added to the system
     */
    private fun generateNewTeachersReport(allTeachers: List<TeacherData>, newTeacherCount: Int) {
        try {
            val now = LocalDateTime.now()
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val fileName = "new_teachers_${now.format(dateFormatter)}.csv"
            
            // Create in the reports/year directory
            val yearDir = File(reportsDir, now.year.toString())
            if (!yearDir.exists()) yearDir.mkdirs()
            
            val reportFile = File(yearDir, fileName)
            
            // Create CSV content
            val csvBuilder = StringBuilder()
            csvBuilder.append("Teacher Name,Phone,Date Added\n")
            
            // Sort teachers by likely add date (we don't have actual add date stored)
            // and take the newest ones based on count
            allTeachers.takeLast(newTeacherCount).forEach { teacher ->
                csvBuilder.append("\"${teacher.name}\",")
                csvBuilder.append("\"${teacher.phone}\",")
                csvBuilder.append("\"${now.format(dateFormatter)}\"\n")
            }
            
            // Write to file
            FileOutputStream(reportFile).use { outputStream ->
                outputStream.write(csvBuilder.toString().toByteArray())
            }
            
            Log.d(TAG, "New teachers report generated: ${reportFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error generating new teachers report", e)
            logError("Error generating new teachers report: ${e.message}")
        }
    }
    
    /**
     * Generate a CSV report of teacher attendance for the current month
     * @return true if report was generated, false otherwise
     */
    private fun generateAttendanceReport(yearDir: File, month: String, now: LocalDateTime): Boolean {
        try {
            val absentTeachers = getAbsentTeachers()
            if (absentTeachers.isEmpty()) {
                Log.d(TAG, "No absent teachers data to report")
                return false
            }
            
            // Create file with timestamp
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val fileName = "teacher_attendance_${month}_${now.format(dateFormatter)}.csv"
            val reportFile = File(yearDir, fileName)
            
            // Create CSV content
            val csvBuilder = StringBuilder()
            csvBuilder.append("Teacher ID,Teacher Name,Absence Date,Substitute Assigned\n")
            
            absentTeachers.forEach { teacher ->
                try {
                    // Parse the timestamp to get just the date
                    val timestamp = java.time.Instant.parse(teacher.timestamp)
                    val date = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault())
                    val formattedDate = date.format(dateFormatter)
                    
                    csvBuilder.append("${teacher.id},")
                    csvBuilder.append("\"${teacher.name}\",")
                    csvBuilder.append("$formattedDate,")
                    csvBuilder.append("${teacher.assignedSubstitute}\n")
                } catch (e: Exception) {
                    // Handle individual teacher parsing errors
                    Log.e(TAG, "Error processing teacher record: ${teacher.name}", e)
                    csvBuilder.append("${teacher.id},")
                    csvBuilder.append("\"${teacher.name}\",")
                    csvBuilder.append("ERROR,") // Indicate there was a date parsing error
                    csvBuilder.append("${teacher.assignedSubstitute}\n")
                }
            }
            
            // Write to file
            FileOutputStream(reportFile).use { outputStream ->
                outputStream.write(csvBuilder.toString().toByteArray())
            }
            
            Log.d(TAG, "Teacher attendance report generated: ${reportFile.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error generating attendance report", e)
            logError("Error generating attendance report: ${e.message}")
            return false
        }
    }
    
    /**
     * Generate a CSV report of substitute assignments for the current month
     * @return true if report was generated, false otherwise
     */
    private fun generateSubstituteReport(yearDir: File, month: String, now: LocalDateTime): Boolean {
        try {
            val substituteAssignments = getSubstituteAssignments()
            if (substituteAssignments.isEmpty()) {
                Log.d(TAG, "No substitute assignments data to report")
                return false
            }
            
            // Create file with timestamp
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val fileName = "substitute_assignments_${month}_${now.format(dateFormatter)}.csv"
            val reportFile = File(yearDir, fileName)
            
            // Create CSV content
            val csvBuilder = StringBuilder()
            csvBuilder.append("Assignment ID,Date,Absent Teacher ID,Substitute Teacher ID,Class,Period,Subject\n")
            
            substituteAssignments.forEach { assignment ->
                try {
                    csvBuilder.append("${assignment.id},")
                    csvBuilder.append("${assignment.date},")
                    csvBuilder.append("${assignment.absentTeacherId},")
                    csvBuilder.append("${assignment.substituteTeacherId},")
                    csvBuilder.append("\"${assignment.className}\",")
                    csvBuilder.append("${assignment.period},")
                    csvBuilder.append("\"${assignment.subject}\"\n")
                } catch (e: Exception) {
                    // Handle individual assignment parsing errors
                    Log.e(TAG, "Error processing assignment record: ${assignment.id}", e)
                    csvBuilder.append("${assignment.id},")
                    csvBuilder.append("ERROR,") // Indicate there was an error
                    csvBuilder.append("${assignment.absentTeacherId},")
                    csvBuilder.append("${assignment.substituteTeacherId},")
                    csvBuilder.append("ERROR,")
                    csvBuilder.append("${assignment.period},")
                    csvBuilder.append("ERROR\n")
                }
            }
            
            // Write to file
            FileOutputStream(reportFile).use { outputStream ->
                outputStream.write(csvBuilder.toString().toByteArray())
            }
            
            Log.d(TAG, "Substitute assignments report generated: ${reportFile.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error generating substitute report", e)
            logError("Error generating substitute report: ${e.message}")
            return false
        }
    }
    
    /**
     * Log an error to a dedicated error log file
     */
    private fun logError(errorMessage: String) {
        try {
            val now = LocalDateTime.now()
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val fileName = "error_log_${now.format(dateFormatter)}.txt"
            val errorFile = File(errorLogsDir, fileName)
            
            // Append to file
            errorFile.appendText("[${now}] $errorMessage\n")
        } catch (e: Exception) {
            // Last resort logging if even our error logging fails
            Log.e(TAG, "Failed to write to error log: ${e.message}")
        }
    }
    
    /**
     * Get all teachers in the system
     */
    private fun getAllTeachers(): List<TeacherData> {
        val totalTeachersFile = File(baseDir, "processed/total_teacher.json")
        return try {
            if (!totalTeachersFile.exists()) {
                Log.w(TAG, "Total teachers file not found at: ${totalTeachersFile.absolutePath}")
                return emptyList()
            }
            val type = object : TypeToken<List<TeacherData>>() {}.type
            FileReader(totalTeachersFile).use { reader ->
                gson.fromJson(reader, type)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading total teachers", e)
            logError("Error reading total teachers: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Get the current list of absent teachers
     */
    private fun getAbsentTeachers(): Set<AbsentTeacherData> {
        val absentTeachersFile = File(baseDir, "absent_teachers.json")
        return try {
            if (!absentTeachersFile.exists()) {
                return emptySet()
            }
            val type = object : TypeToken<Set<AbsentTeacherData>>() {}.type
            FileReader(absentTeachersFile).use { reader ->
                gson.fromJson(reader, type)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading absent teachers", e)
            logError("Error reading absent teachers: ${e.message}")
            emptySet()
        }
    }
    
    /**
     * Get the current list of substitute assignments
     */
    private fun getSubstituteAssignments(): List<SubstituteAssignment> {
        val substitutesFile = File(baseDir, "substitute_assignments.json")
        return try {
            if (!substitutesFile.exists()) {
                return emptyList()
            }
            val type = object : TypeToken<List<SubstituteAssignment>>() {}.type
            FileReader(substitutesFile).use { reader ->
                gson.fromJson(reader, type)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading substitute assignments", e)
            logError("Error reading substitute assignments: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Check if reports exist for a specific year/month
     */
    fun reportsExistForMonth(year: Int, month: String): Boolean {
        val yearDir = File(reportsDir, year.toString())
        if (!yearDir.exists()) return false
        
        // Check if any files contain the month name
        return yearDir.listFiles()?.any { 
            it.name.contains(month) && (
                it.name.startsWith("teacher_attendance") || 
                it.name.startsWith("substitute_assignments")
            )
        } ?: false
    }
} 