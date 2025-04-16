package com.substituemanagment.managment.data.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.substituemanagment.managment.data.AbsentTeacherData
import com.substituemanagment.managment.data.TeacherData
import com.substituemanagment.managment.data.models.SubstituteAssignment
import com.substituemanagment.managment.data.models.Teacher
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
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
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    
    // Base directory for all application data
    private val baseDir = File(context.getExternalFilesDir(null), "substitute_data")
    
    // Directory for reports organized by year/month
    private val reportsDir = File(baseDir, "reports")
    
    // Directory for error logs
    private val errorLogsDir = File(baseDir, "error_logs")
    
    // Directory for JSON backups
    private val jsonBackupsDir = File(baseDir, "json_backups")
    
    // Directory for monthly attendance sheets
    private val attendanceSheetsDir = File(baseDir, "attendance_sheets")
    
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
        listOf(baseDir, reportsDir, errorLogsDir, jsonBackupsDir, attendanceSheetsDir).forEach { dir ->
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
            
            // Create JSON backup directory for this month
            val jsonMonthDir = File(jsonBackupsDir, "$year-$month")
            if (!jsonMonthDir.exists()) jsonMonthDir.mkdirs()
            
            // Create attendance sheets directory
            val attendanceYearMonthDir = File(attendanceSheetsDir, "$year/$month")
            if (!attendanceYearMonthDir.exists()) attendanceYearMonthDir.mkdirs()
            
            // Generate attendance report
            val attendanceSuccess = generateAttendanceReport(yearDir, month, now)
            
            // Generate substitute assignments report
            val substituteSuccess = generateSubstituteReport(yearDir, month, now)
            
            // Generate monthly calendar-style attendance sheet
            val calendarSuccess = generateMonthlyAttendanceSheet(attendanceYearMonthDir, now)
            
            // Generate monthly substitute assignment sheet
            val substituteSheetSuccess = generateMonthlySubstituteSheet(attendanceYearMonthDir, now)
            
            // Create JSON backups
            val jsonBackupSuccess = createJsonBackups(jsonMonthDir, now)
            
            if (attendanceSuccess || substituteSuccess || jsonBackupSuccess || calendarSuccess || substituteSheetSuccess) {
                Log.d(TAG, "Reports generation completed successfully")
            } else {
                Log.w(TAG, "No reports were generated - no data available")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating reports", e)
            logError("Report generation error: ${e.message}")
            
            // Try to recover by ensuring directories exist
            try {
                listOf(baseDir, reportsDir, errorLogsDir, jsonBackupsDir, attendanceSheetsDir).forEach { dir ->
                    if (!dir.exists()) dir.mkdirs()
                }
            } catch (dirEx: Exception) {
                Log.e(TAG, "Failed to recover by creating directories", dirEx)
            }
        }
    }
    
    /**
     * Generate a monthly calendar-style attendance sheet showing daily attendance for each teacher
     */
    private fun generateMonthlyAttendanceSheet(targetDir: File, now: LocalDateTime): Boolean {
        try {
            val allTeachers = getAllTeachers()
            val absentTeachers = getAbsentTeachers().toList()
            
            if (allTeachers.isEmpty()) {
                Log.d(TAG, "No teacher data available for monthly attendance sheet")
                return false
            }
            
            // Get the year and month
            val year = now.year
            val month = now.month
            val yearMonth = YearMonth.of(year, month)
            val daysInMonth = yearMonth.lengthOfMonth()
            
            // Create attendance file
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val fileName = "attendance_calendar_${month}_${now.format(dateFormatter)}.csv"
            val attendanceFile = File(targetDir, fileName)
            
            // Build CSV content
            val csvBuilder = StringBuilder()
            
            // Header row with dates
            csvBuilder.append("TeacherName")
            for (day in 1..daysInMonth) {
                csvBuilder.append(",$day")
            }
            csvBuilder.append("\n")
            
            // Map of teacherName -> Map of day -> attendance status
            val teacherAttendance = mutableMapOf<String, MutableMap<Int, Char>>()
            
            // Initialize all teachers as present for all days
            allTeachers.forEach { teacher ->
                val dayStatusMap = mutableMapOf<Int, Char>()
                for (day in 1..daysInMonth) {
                    dayStatusMap[day] = 'p' // Default to present
                }
                teacherAttendance[teacher.name] = dayStatusMap
            }
            
            // Mark absent days based on absent teacher records
            absentTeachers.forEach { absentRecord ->
                try {
                    val timestamp = java.time.Instant.parse(absentRecord.timestamp)
                    val date = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault())
                    
                    // Only process records for the current month
                    if (date.year == year && date.month == month) {
                        val day = date.dayOfMonth
                        val teacherName = absentRecord.name
                        
                        // Mark as absent
                        teacherAttendance[teacherName]?.put(day, 'a')
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing absent record date: ${absentRecord.name}", e)
                }
            }
            
            // Add each teacher's attendance to the CSV
            teacherAttendance.forEach { (teacherName, dayStatus) ->
                csvBuilder.append(teacherName)
                for (day in 1..daysInMonth) {
                    val status = dayStatus[day] ?: 'p'
                    csvBuilder.append(",$status")
                }
                csvBuilder.append("\n")
            }
            
            // Write to file
            FileOutputStream(attendanceFile).use { outputStream ->
                outputStream.write(csvBuilder.toString().toByteArray())
            }
            
            Log.d(TAG, "Monthly attendance calendar sheet generated: ${attendanceFile.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error generating monthly attendance sheet", e)
            logError("Error generating monthly attendance sheet: ${e.message}")
            return false
        }
    }
    
    /**
     * Generate a monthly sheet showing substitute assignments for each day
     */
    private fun generateMonthlySubstituteSheet(targetDir: File, now: LocalDateTime): Boolean {
        try {
            val substituteAssignments = getSubstituteAssignments()
            val allTeachers = getAllTeachers()
            
            if (substituteAssignments.isEmpty()) {
                Log.d(TAG, "No substitute assignments available for monthly sheet")
                return false
            }
            
            // Get the year and month
            val year = now.year
            val month = now.month
            val yearMonth = YearMonth.of(year, month)
            val daysInMonth = yearMonth.lengthOfMonth()
            
            // Create file
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val fileName = "substitute_assignments_calendar_${month}_${now.format(dateFormatter)}.csv"
            val assignmentFile = File(targetDir, fileName)
            
            // Create a map of teacher names for quick lookups
            val teacherMap = allTeachers.associateBy { it.name }
            
            // Create a structure to hold assignments by day
            val assignmentsByDay = mutableMapOf<Int, MutableList<SubstituteAssignment>>()
            for (day in 1..daysInMonth) {
                assignmentsByDay[day] = mutableListOf()
            }
            
            // Group assignments by day
            substituteAssignments.forEach { assignment ->
                try {
                    val date = LocalDate.parse(assignment.date, DateTimeFormatter.ISO_DATE)
                    if (date.year == year && date.month == month) {
                        val day = date.dayOfMonth
                        assignmentsByDay[day]?.add(assignment)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing assignment date: ${assignment.date}", e)
                }
            }
            
            // Build CSV content
            val csvBuilder = StringBuilder()
            csvBuilder.append("Day,Absent Teacher,Substitute Teacher,Class,Period,Subject\n")
            
            // Add data for each day
            for (day in 1..daysInMonth) {
                val assignments = assignmentsByDay[day] ?: emptyList()
                
                if (assignments.isEmpty()) {
                    // No assignments for this day
                    csvBuilder.append("$day,No absences recorded,,,\n")
                } else {
                    // Add each assignment for this day
                    assignments.forEach { assignment ->
                        val absentTeacherName = teacherMap[assignment.absentTeacherId]?.name ?: assignment.absentTeacherId
                        val substituteTeacherName = teacherMap[assignment.substituteTeacherId]?.name ?: assignment.substituteTeacherId
                        
                        csvBuilder.append("$day,")
                        csvBuilder.append("\"$absentTeacherName\",")
                        csvBuilder.append("\"$substituteTeacherName\",")
                        csvBuilder.append("\"${assignment.className}\",")
                        csvBuilder.append("${assignment.period},")
                        csvBuilder.append("\"${assignment.subject}\"\n")
                    }
                }
            }
            
            // Write to file
            FileOutputStream(assignmentFile).use { outputStream ->
                outputStream.write(csvBuilder.toString().toByteArray())
            }
            
            Log.d(TAG, "Monthly substitute assignments sheet generated: ${assignmentFile.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error generating monthly substitute sheet", e)
            logError("Error generating monthly substitute sheet: ${e.message}")
            return false
        }
    }
    
    /**
     * Create JSON backups of all important data
     */
    private fun createJsonBackups(jsonMonthDir: File, now: LocalDateTime): Boolean {
        try {
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val todayStr = now.format(dateFormatter)
            var success = false
            
            // 1. Backup substitute assignments
            val substituteAssignments = getSubstituteAssignments()
            if (substituteAssignments.isNotEmpty()) {
                val substituteBackupFile = File(jsonMonthDir, "substitute_assignments_$todayStr.json")
                FileWriter(substituteBackupFile).use { writer ->
                    gson.toJson(substituteAssignments, writer)
                }
                Log.d(TAG, "JSON backup created for substitute assignments: ${substituteBackupFile.absolutePath}")
                success = true
            }
            
            // 2. Backup absent teachers
            val absentTeachers = getAbsentTeachers()
            if (absentTeachers.isNotEmpty()) {
                val absentTeachersBackupFile = File(jsonMonthDir, "absent_teachers_$todayStr.json")
                FileWriter(absentTeachersBackupFile).use { writer ->
                    gson.toJson(absentTeachers, writer)
                }
                Log.d(TAG, "JSON backup created for absent teachers: ${absentTeachersBackupFile.absolutePath}")
                success = true
            }
            
            // 3. Backup total teachers
            val allTeachers = getAllTeachers()
            if (allTeachers.isNotEmpty()) {
                val teachersBackupFile = File(jsonMonthDir, "total_teachers_$todayStr.json")
                FileWriter(teachersBackupFile).use { writer ->
                    gson.toJson(allTeachers, writer)
                }
                Log.d(TAG, "JSON backup created for total teachers: ${teachersBackupFile.absolutePath}")
                success = true
            }
            
            return success
        } catch (e: Exception) {
            Log.e(TAG, "Error creating JSON backups", e)
            logError("JSON backup error: ${e.message}")
            return false
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
            
            // Also create a JSON backup of new teachers
            val jsonBackupDir = File(jsonBackupsDir, now.year.toString())
            if (!jsonBackupDir.exists()) jsonBackupDir.mkdirs()
            
            val jsonFile = File(jsonBackupDir, "new_teachers_${now.format(dateFormatter)}.json")
            FileWriter(jsonFile).use { writer ->
                gson.toJson(allTeachers.takeLast(newTeacherCount), writer)
            }
            
            Log.d(TAG, "New teachers report generated: ${reportFile.absolutePath}")
            Log.d(TAG, "New teachers JSON backup created: ${jsonFile.absolutePath}")
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
            val allTeachers = getAllTeachers()
            
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
            csvBuilder.append("Teacher ID,Teacher Name,Teacher Phone,Absence Date,Substitute Assigned,Registered In System\n")
            
            // Create a map of teacher name to TeacherData for quick lookup
            val teacherMap = allTeachers.associateBy { it.name }
            
            absentTeachers.forEach { absentTeacher ->
                try {
                    // Parse the timestamp to get just the date
                    val timestamp = java.time.Instant.parse(absentTeacher.timestamp)
                    val date = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault())
                    val formattedDate = date.format(dateFormatter)
                    
                    // Look up the teacher in the total teachers list to get additional data
                    val teacherData = teacherMap[absentTeacher.name]
                    val phone = teacherData?.phone ?: "Not available"
                    val isRegistered = teacherData != null
                    
                    csvBuilder.append("${absentTeacher.id},")
                    csvBuilder.append("\"${absentTeacher.name}\",")
                    csvBuilder.append("\"$phone\",")
                    csvBuilder.append("$formattedDate,")
                    csvBuilder.append("${absentTeacher.assignedSubstitute},")
                    csvBuilder.append("$isRegistered\n")
                } catch (e: Exception) {
                    // Handle individual teacher parsing errors
                    Log.e(TAG, "Error processing teacher record: ${absentTeacher.name}", e)
                    csvBuilder.append("${absentTeacher.id},")
                    csvBuilder.append("\"${absentTeacher.name}\",")
                    csvBuilder.append("\"Unknown\",")
                    csvBuilder.append("ERROR,") // Indicate there was a date parsing error
                    csvBuilder.append("${absentTeacher.assignedSubstitute},")
                    csvBuilder.append("UNKNOWN\n")
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
            val allTeachers = getAllTeachers()
            
            if (substituteAssignments.isEmpty()) {
                Log.d(TAG, "No substitute assignments data to report")
                return false
            }
            
            // Create file with timestamp
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val fileName = "substitute_assignments_${month}_${now.format(dateFormatter)}.csv"
            val reportFile = File(yearDir, fileName)
            
            // Create a map of teacher IDs to names and phones for quick lookup
            val teacherMap = allTeachers.associateBy(
                { it.name }, 
                { TeacherLookup(it.name, it.phone) }
            )
            
            // Create CSV content
            val csvBuilder = StringBuilder()
            csvBuilder.append("Assignment ID,Date,Absent Teacher ID,Absent Teacher Name,Substitute Teacher ID,Substitute Teacher Name,Class,Period,Subject\n")
            
            substituteAssignments.forEach { assignment ->
                try {
                    // Try to get teacher names from the lookup map
                    val absentTeacherName = teacherMap[assignment.absentTeacherId]?.name ?: assignment.absentTeacherId
                    val substituteTeacherName = teacherMap[assignment.substituteTeacherId]?.name ?: assignment.substituteTeacherId
                    
                    csvBuilder.append("${assignment.id},")
                    csvBuilder.append("${assignment.date},")
                    csvBuilder.append("${assignment.absentTeacherId},")
                    csvBuilder.append("\"$absentTeacherName\",")
                    csvBuilder.append("${assignment.substituteTeacherId},")
                    csvBuilder.append("\"$substituteTeacherName\",")
                    csvBuilder.append("\"${assignment.className}\",")
                    csvBuilder.append("${assignment.period},")
                    csvBuilder.append("\"${assignment.subject}\"\n")
                } catch (e: Exception) {
                    // Handle individual assignment parsing errors
                    Log.e(TAG, "Error processing assignment record: ${assignment.id}", e)
                    csvBuilder.append("${assignment.id},")
                    csvBuilder.append("ERROR,") // Indicate there was an error
                    csvBuilder.append("${assignment.absentTeacherId},")
                    csvBuilder.append("\"ERROR\",")
                    csvBuilder.append("${assignment.substituteTeacherId},")
                    csvBuilder.append("\"ERROR\",")
                    csvBuilder.append("\"${assignment.className}\",")
                    csvBuilder.append("${assignment.period},")
                    csvBuilder.append("\"${assignment.subject}\"\n")
                }
            }
            
            // Write to file
            FileOutputStream(reportFile).use { outputStream ->
                outputStream.write(csvBuilder.toString().toByteArray())
            }
            
            // Also create a detailed JSON backup with structured information
            createDetailedSubstituteBackup(substituteAssignments, allTeachers, now)
            
            Log.d(TAG, "Substitute assignments report generated: ${reportFile.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error generating substitute report", e)
            logError("Error generating substitute report: ${e.message}")
            return false
        }
    }
    
    /**
     * Create a detailed JSON backup of substitute assignments with teacher information
     */
    private fun createDetailedSubstituteBackup(
        assignments: List<SubstituteAssignment>,
        teachers: List<TeacherData>,
        now: LocalDateTime
    ) {
        try {
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val backupFileName = "detailed_substitute_assignments_${now.format(dateFormatter)}.json"
            
            // Create in the json_backups/year-month directory
            val yearMonth = "${now.year}-${now.month}"
            val backupDir = File(jsonBackupsDir, yearMonth)
            if (!backupDir.exists()) backupDir.mkdirs()
            
            val backupFile = File(backupDir, backupFileName)
            
            // Create a map for quick teacher lookup
            val teacherMap = teachers.associateBy { it.name }
            
            // Create detailed records with teacher information
            val detailedAssignments = assignments.map { assignment ->
                val absentTeacher = teacherMap[assignment.absentTeacherId]
                val substituteTeacher = teacherMap[assignment.substituteTeacherId]
                
                DetailedSubstituteAssignment(
                    id = assignment.id,
                    date = assignment.date,
                    absentTeacherId = assignment.absentTeacherId,
                    absentTeacherName = absentTeacher?.name ?: "Unknown",
                    absentTeacherPhone = absentTeacher?.phone ?: "Unknown",
                    substituteTeacherId = assignment.substituteTeacherId,
                    substituteTeacherName = substituteTeacher?.name ?: "Unknown",
                    substituteTeacherPhone = substituteTeacher?.phone ?: "Unknown",
                    className = assignment.className,
                    period = assignment.period,
                    subject = assignment.subject
                )
            }
            
            // Write to JSON file
            FileWriter(backupFile).use { writer ->
                gson.toJson(detailedAssignments, writer)
            }
            
            Log.d(TAG, "Detailed substitute assignments JSON backup created: ${backupFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating detailed substitute assignments backup", e)
            logError("Error creating detailed substitute assignments backup: ${e.message}")
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
    
    /**
     * Helper class for teacher lookups
     */
    private data class TeacherLookup(
        val name: String,
        val phone: String
    )
    
    /**
     * Detailed substitute assignment class for JSON export
     */
    private data class DetailedSubstituteAssignment(
        val id: String,
        val date: String,
        val absentTeacherId: String,
        val absentTeacherName: String,
        val absentTeacherPhone: String,
        val substituteTeacherId: String,
        val substituteTeacherName: String,
        val substituteTeacherPhone: String,
        val className: String,
        val period: Int,
        val subject: String
    )
} 