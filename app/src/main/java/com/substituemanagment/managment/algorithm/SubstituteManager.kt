package com.substituemanagment.managment.algorithm

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.substituemanagment.managment.algorithm.models.*
import com.substituemanagment.managment.algorithm.utils.CsvUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

/**
 * File paths in external storage
 */
const val EXTERNAL_STORAGE_PATH = "/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data"
const val PROCESSED_DIR = "$EXTERNAL_STORAGE_PATH/processed"

// Default file paths (for backwards compatibility)
const val DEFAULT_TIMETABLE_PATH = "data/timetable_file.csv"
const val DEFAULT_SUBSTITUTES_PATH = "data/Substitude_file.csv"
const val DEFAULT_TEACHERS_PATH = "data/total_teacher.json"
const val DEFAULT_SCHEDULES_PATH = "data/teacher_schedules.json"
const val DEFAULT_ASSIGNED_TEACHERS_PATH = "data/assigned_teacher.json"

// Processed file paths
const val PROCESSED_TEACHERS_PATH = "$PROCESSED_DIR/total_teacher.json"
const val PROCESSED_SCHEDULES_PATH = "$PROCESSED_DIR/teacher_schedules.json"
const val PROCESSED_CLASS_SCHEDULES_PATH = "$PROCESSED_DIR/class_schedules.json"
const val PROCESSED_DAY_SCHEDULES_PATH = "$PROCESSED_DIR/day_schedules.json"
const val PROCESSED_PERIOD_SCHEDULES_PATH = "$PROCESSED_DIR/period_schedules.json"

/**
 * Constants
 */
const val MAX_DAILY_WORKLOAD = 6
private const val TAG = "SubstituteManager"

/**
 * Main class for managing substitute teacher assignments.
 * Modified for Android integration.
 */
class SubstituteManager(private val context: Context) {
    private val schedule: MutableMap<String, MutableMap<Int, List<String>>> = HashMap()
    private val substitutes: MutableMap<String, String> = HashMap()
    private val teacherClasses: MutableMap<String, MutableList<Assignment>> = HashMap()
    private val substituteAssignments: MutableMap<String, MutableList<Assignment>> = HashMap()
    private val teacherWorkload: MutableMap<String, Int> = HashMap()
    private val MAX_SUBSTITUTE_ASSIGNMENTS = 3
    private val MAX_REGULAR_TEACHER_ASSIGNMENTS = 2
    private val allAssignments: MutableList<Assignment> = ArrayList()
    private val allTeachers: MutableList<Teacher> = ArrayList() // Store all teachers for easy lookup
    private val timetable: MutableList<Any> = ArrayList() // Store timetable data

    /**
     * Loads data from processed files if available, falls back to assets if not.
     */
    suspend fun loadData(
        timetablePath: String = DEFAULT_TIMETABLE_PATH,
        substitutesPath: String = DEFAULT_SUBSTITUTES_PATH,
        teachersPath: String = DEFAULT_TEACHERS_PATH,
        schedulesPath: String = DEFAULT_SCHEDULES_PATH,
        assignedTeachersPath: String = DEFAULT_ASSIGNED_TEACHERS_PATH
    ) {
        try {
            Log.d(TAG, "Loading data from processed files or assets")
            
            // Clear existing data
            clearData()
            
            // First try to load teachers from processed file
            val processedTeachersFile = File(PROCESSED_TEACHERS_PATH)
            if (processedTeachersFile.exists()) {
                Log.d(TAG, "Loading teachers from processed file: ${processedTeachersFile.absolutePath}")
                val teachersContent = processedTeachersFile.readText()
                loadTeachersFromJson(teachersContent)
            } else {
                // Fall back to assets
                Log.d(TAG, "Loading teachers from assets: $teachersPath")
                val teachersContent = context.assets.open(teachersPath).bufferedReader().use { it.readText() }
                loadTeachersFromJson(teachersContent)
            }
            
            // Try to load teacher schedules from processed file
            val processedSchedulesFile = File(PROCESSED_SCHEDULES_PATH)
            if (processedSchedulesFile.exists()) {
                Log.d(TAG, "Loading schedules from processed file: ${processedSchedulesFile.absolutePath}")
                val schedulesContent = processedSchedulesFile.readText()
                loadSchedulesFromJson(schedulesContent)
            } else {
                // Fall back to assets
                Log.d(TAG, "Loading schedules from assets: $schedulesPath")
                val schedulesContent = context.assets.open(schedulesPath).bufferedReader().use { it.readText() }
                loadSchedulesFromJson(schedulesContent)
            }
            
            // Load previously assigned teachers if available
            try {
                val assignedTeachersFile = File(assignedTeachersPath)
                if (assignedTeachersFile.exists()) {
                    Log.d(TAG, "Loading previously assigned teachers from: ${assignedTeachersFile.absolutePath}")
                    val assignedContent = assignedTeachersFile.readText()
                    loadAssignedTeachers(assignedContent)
                } else {
                    // Try from assets
                    try {
                        val assignedContent = context.assets.open(assignedTeachersPath).bufferedReader().use { it.readText() }
                        loadAssignedTeachers(assignedContent)
                    } catch (e: Exception) {
                        Log.w(TAG, "No previous assignments found in assets: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error loading previous assignments: ${e.message}")
            }

            Log.d(TAG, "Loaded ${substitutes.size} substitutes")
            Log.d(TAG, "Loaded ${allTeachers.size} teachers")
            Log.d(TAG, "Loaded schedules for ${teacherClasses.size} teachers")

        } catch (error: Exception) {
            Log.e(TAG, "Error loading data: ${error.message}")
            throw Exception("Error loading data: $error")
        }
    }
    
    /**
     * Loads teachers from JSON content
     */
    private fun loadTeachersFromJson(content: String) {
        val teacherType = object : TypeToken<List<Map<String, Any>>>() {}.type
        val teachersData: List<Map<String, Any>> = Gson().fromJson(content, teacherType)
        
        for (teacherData in teachersData) {
            val name = teacherData["name"] as? String ?: continue
            val phone = teacherData["phone"] as? String ?: ""
            val isSubstitute = teacherData["isSubstitute"] as? Boolean ?: false
            val variations = teacherData["variations"] as? List<String> ?: listOf()
            
            // Add to all teachers
            val teacher = Teacher(
                name = name,
                phone = phone,
                isSubstitute = isSubstitute,
                variations = variations
            )
            allTeachers.add(teacher)
            
            // Register as substitute if needed
            if (isSubstitute) {
                substitutes[name.lowercase()] = phone
            }
        }
    }
    
    /**
     * Loads schedules from JSON content
     */
    private fun loadSchedulesFromJson(content: String) {
        val schedulesType = object : TypeToken<Map<String, List<Map<String, Any>>>>() {}.type
        val schedules: Map<String, List<Map<String, Any>>> = Gson().fromJson(content, schedulesType)
        
        for ((teacherName, assignmentsData) in schedules) {
            val assignments = assignmentsData.mapNotNull { assignmentData ->
                val day = assignmentData["day"] as? String ?: return@mapNotNull null
                val period = (assignmentData["period"] as? Double)?.toInt() 
                    ?: (assignmentData["period"] as? Int)
                    ?: return@mapNotNull null
                val className = assignmentData["className"] as? String ?: return@mapNotNull null
                val originalTeacher = assignmentData["originalTeacher"] as? String ?: teacherName
                val substitute = assignmentData["substitute"] as? String ?: ""
                
                Assignment(
                    day = day.lowercase(),
                    period = period,
                    className = className,
                    originalTeacher = originalTeacher.lowercase(),
                    substitute = substitute.lowercase()
                )
            }
            
            if (assignments.isNotEmpty()) {
                teacherClasses[teacherName.lowercase()] = assignments.toMutableList()
            }
        }
    }
    
    /**
     * Loads previously assigned teachers
     */
    private fun loadAssignedTeachers(content: String) {
        val assignedType = object : TypeToken<Map<String, Any>>() {}.type
        val assigned: Map<String, Any> = Gson().fromJson(content, assignedType)
        
        if (assigned.containsKey("assignments")) {
            val assignments = assigned["assignments"] as? List<*> ?: emptyList<Any>()
            for (assignment in assignments) {
                if (assignment is Map<*, *>) {
                    val originalTeacher = assignment["originalTeacher"] as? String ?: ""
                    val period = (assignment["period"] as? Double)?.toInt() ?: 0
                    val className = assignment["className"] as? String ?: ""
                    val substitute = assignment["substitute"] as? String ?: ""
                    val substitutePhone = assignment["substitutePhone"] as? String ?: ""
                    
                    if (originalTeacher.isNotBlank() && substitute.isNotBlank()) {
                        val assignmentObj = Assignment(
                            day = "saturday", // Default to Saturday for existing assignments
                            period = period,
                            className = className,
                            originalTeacher = originalTeacher.lowercase(),
                            substitute = substitute.lowercase()
                        )
                        
                        // Update assignments
                        if (!substituteAssignments.containsKey(substitute.lowercase())) {
                            substituteAssignments[substitute.lowercase()] = ArrayList()
                        }
                        substituteAssignments[substitute.lowercase()]?.add(assignmentObj)
                        
                        // Update workload
                        teacherWorkload[substitute.lowercase()] = (teacherWorkload[substitute.lowercase()] ?: 0) + 1
                    }
                }
            }
        }
    }
    
    /**
     * Clears all data in memory
     */
    private fun clearData() {
        schedule.clear()
        substitutes.clear()
        teacherClasses.clear()
        substituteAssignments.clear()
        teacherWorkload.clear()
        allAssignments.clear()
        allTeachers.clear()
        timetable.clear()
    }

    /**
     * Helper method to fix common CSV format issues.
     *
     * @param content The CSV content to fix
     * @return The fixed CSV content
     */
    private fun fixCSVContent(content: String): String {
        val lines = content.split("\n")
        val fixedLines = lines.map { line ->
            // Remove extra quotes if they're unbalanced
            val quoteCount = line.count { it == '"' }
            var fixedLine = if (quoteCount % 2 != 0) {
                line.replace("\"", "")
            } else {
                line
            }

            // Ensure each line ends with the right number of commas
            val expectedColumns = if (line.startsWith("Day,Period")) 17 else 3 // For timetable or substitutes
            val commaCount = fixedLine.count { it == ',' }

            if (commaCount > expectedColumns - 1) {
                // Too many commas, trim excess
                val parts = fixedLine.split(",").take(expectedColumns)
                fixedLine = parts.joinToString(",")
            } else if (commaCount < expectedColumns - 1 && fixedLine.isNotBlank()) {
                // Too few commas, add missing ones
                val missingCommas = expectedColumns - 1 - commaCount
                fixedLine = fixedLine + ",".repeat(missingCommas)
            }

            fixedLine
        }

        return fixedLines.joinToString("\n")
    }

    /**
     * Parses the timetable CSV content.
     *
     * @param content The CSV content to parse
     */
    private fun parseTimetable(content: String) {
        val rows = content.split("\n").filter { it.isNotBlank() }
        
        // Skip header row
        for (i in 1 until rows.size) {
            val cols = rows[i].split(",")
            if (cols.isEmpty() || cols.size < 2) continue

            val day = normalizeDay(cols[0])
            val periodStr = cols[1].trim()
            val period = periodStr.toIntOrNull() ?: continue

            val teachers = cols.subList(2, cols.size)
                .map { if (it.isNotBlank() && it.trim().lowercase() != "empty") normalizeName(it) else null }
                .filterNotNull()

            if (!schedule.containsKey(day)) schedule[day] = HashMap()
            schedule[day]?.set(period, teachers)

            teachers.forEachIndexed { idx, teacher ->
                val classes = listOf("10A", "10B", "10C", "9A", "9B", "9C", "8A", "8B", "8C", "7A", "7B", "7C", "6A", "6B", "6C")
                if (idx < classes.size) {
                    val className = classes[idx]
                    if (!teacherClasses.containsKey(teacher)) teacherClasses[teacher] = ArrayList()
                    teacherClasses[teacher]?.add(
                        Assignment(
                            day = day,
                            period = period,
                            className = className,
                            originalTeacher = teacher,
                            substitute = ""
                        )
                    )
                }
            }
        }
    }

    /**
     * Parses the substitutes CSV content.
     *
     * @param content The CSV content to parse
     */
    private fun parseSubstitutes(content: String) {
        val rows = content.split("\n").filter { it.isNotBlank() }
        
        for (row in rows) {
            val cols = row.split(",")
            if (cols.size < 2) continue
            
            val name = normalizeName(cols[0])
            val phone = cols[1].trim()
            
            if (name.isNotBlank() && phone.isNotBlank()) {
                substitutes[name] = phone
            }
        }
    }

    /**
     * Normalizes the day name.
     *
     * @param day The day name to normalize
     * @return The normalized day name
     */
    private fun normalizeDay(day: String): String {
        return day.trim().lowercase()
    }

    /**
     * Normalizes the teacher name.
     *
     * @param name The teacher name to normalize
     * @return The normalized teacher name
     */
    private fun normalizeName(name: String): String {
        return name.trim().lowercase()
    }

    /**
     * Assigns substitutes for an absent teacher on a specific day.
     *
     * @param teacherName The name of the absent teacher
     * @param day The day of the week
     * @return A list of substitute assignments
     */
    fun assignSubstitutes(teacherName: String, day: String): List<SubstituteAssignment> {
        val normalizedTeacherName = normalizeName(teacherName)
        val normalizedDay = normalizeDay(day)
        
        Log.d(TAG, "Assigning substitutes for: $normalizedTeacherName on $normalizedDay")
        
        // Get the classes for the absent teacher
        val classes = teacherClasses[normalizedTeacherName]?.filter { it.day == normalizedDay } ?: listOf()
        
        if (classes.isEmpty()) {
            Log.w(TAG, "No classes found for teacher: $normalizedTeacherName on $normalizedDay")
            return listOf()
        }
        
        val assignments = mutableListOf<SubstituteAssignment>()
        
        // Assign substitutes for each class
        for (clazz in classes) {
            val period = clazz.period
            val className = clazz.className
            
            // Find available substitutes for this period
            val availableSubs = findAvailableSubstitutes(normalizedDay, period, normalizedTeacherName)
            
            if (availableSubs.isNotEmpty()) {
                // Choose the best substitute
                val sub = chooseBestSubstitute(availableSubs, normalizedDay, period)
                
                // Create the assignment
                val subName = sub.first
                val subPhone = sub.second
                
                val assignment = SubstituteAssignment(
                    originalTeacher = normalizedTeacherName,
                    period = period,
                    className = className,
                    substitute = subName,
                    substitutePhone = subPhone
                )
                
                // Add to our assignments list
                assignments.add(assignment)
                
                // Update the substitute's assignments
                if (!substituteAssignments.containsKey(subName)) {
                    substituteAssignments[subName] = mutableListOf()
                }
                
                substituteAssignments[subName]?.add(
                    Assignment(
                        day = normalizedDay,
                        period = period,
                        className = className,
                        originalTeacher = normalizedTeacherName,
                        substitute = subName
                    )
                )
                
                // Update the workload
                teacherWorkload[subName] = (teacherWorkload[subName] ?: 0) + 1
                
                Log.d(TAG, "Assigned $subName to substitute for $normalizedTeacherName in period $period")
            } else {
                Log.w(TAG, "No available substitutes for period $period")
            }
        }
        
        // Save the assignments
        saveAssignments(assignments)
        
        return assignments
    }

    /**
     * Finds available substitutes for a specific day and period.
     *
     * @param day The day of the week
     * @param period The period number
     * @param absentTeacher The name of the absent teacher
     * @return A list of available substitutes (name, phone)
     */
    private fun findAvailableSubstitutes(day: String, period: Int, absentTeacher: String): List<Pair<String, String>> {
        val availableSubs = mutableListOf<Pair<String, String>>()
        
        // Get all teachers who have classes during this period
        val teachersWithClasses = HashSet<String>()
        schedule[day]?.get(period)?.forEach { teachersWithClasses.add(it) }
        
        // Check each substitute
        for ((subName, subPhone) in substitutes) {
            // Skip if the substitute is already assigned during this period
            if (substituteAssignments.containsKey(subName)) {
                val subAssignments = substituteAssignments[subName] ?: listOf()
                if (subAssignments.any { it.day == day && it.period == period }) {
                    continue
                }
            }
            
            // Skip if the substitute is already teaching during this period
            if (teachersWithClasses.contains(subName)) {
                continue
            }
            
            // Skip if the substitute has reached their maximum assignments
            val workload = teacherWorkload[subName] ?: 0
            if (workload >= MAX_SUBSTITUTE_ASSIGNMENTS) {
                continue
            }
            
            // This substitute is available
            availableSubs.add(Pair(subName, subPhone))
        }
        
        // If we don't have enough substitutes, check if regular teachers can substitute
        if (availableSubs.size < 3) {
            for (teacher in allTeachers) {
                val teacherName = teacher.name.lowercase()
                
                // Skip if this is the absent teacher or already a substitute
                if (teacherName == absentTeacher || substitutes.containsKey(teacherName)) {
                    continue
                }
                
                // Skip if the teacher is already teaching during this period
                if (teachersWithClasses.contains(teacherName)) {
                    continue
                }
                
                // Skip if the teacher has reached their maximum assignments
                val workload = teacherWorkload[teacherName] ?: 0
                if (workload >= MAX_REGULAR_TEACHER_ASSIGNMENTS) {
                    continue
                }
                
                // This teacher is available
                availableSubs.add(Pair(teacherName, teacher.phone))
            }
        }
        
        return availableSubs
    }

    /**
     * Chooses the best substitute from a list of available substitutes.
     *
     * @param availableSubs The list of available substitutes
     * @param day The day of the week
     * @param period The period number
     * @return The chosen substitute (name, phone)
     */
    private fun chooseBestSubstitute(
        availableSubs: List<Pair<String, String>>,
        day: String,
        period: Int
    ): Pair<String, String> {
        // Sort substitutes by workload (ascending)
        val sortedSubs = availableSubs.sortedBy { teacherWorkload[it.first] ?: 0 }
        
        // Choose the substitute with the lowest workload
        return sortedSubs.first()
    }

    /**
     * Saves the substitute assignments.
     *
     * @param assignments The list of assignments to save
     */
    private fun saveAssignments(assignments: List<SubstituteAssignment>) {
        // Add to our list of all assignments
        for (assignment in assignments) {
            allAssignments.add(
                Assignment(
                    day = "saturday", // Always Saturday for testing
                    period = assignment.period,
                    className = assignment.className,
                    originalTeacher = assignment.originalTeacher,
                    substitute = assignment.substitute
                )
            )
        }
        
        // Save to external storage
        try {
            val assignmentsMap = mapOf(
                "assignments" to assignments,
                "timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            )
            
            val json = Gson().toJson(assignmentsMap)
            val baseDir = File(EXTERNAL_STORAGE_PATH)
            if (!baseDir.exists()) {
                baseDir.mkdirs()
            }
            
            val file = File(baseDir, "assigned_teachers.json")
            file.writeText(json)
            
            Log.d(TAG, "Saved ${assignments.size} assignments to ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving assignments: ${e.message}")
        }
    }

    /**
     * Gets all current substitute assignments.
     *
     * @return A map with assignments and warnings
     */
    fun getSubstituteAssignments(): Map<String, Any> {
        val result = HashMap<String, Any>()
        result["assignments"] = allAssignments
        result["warnings"] = listOf<String>() // No warnings for now
        return result
    }

    /**
     * Clears all substitute assignments.
     */
    fun clearAssignments() {
        substituteAssignments.clear()
        teacherWorkload.clear()
        allAssignments.clear()
        Log.d(TAG, "Cleared all assignments")
    }

    /**
     * Verifies the validity of all substitute assignments.
     *
     * @return A list of verification reports
     */
    fun verifyAssignments(): List<VerificationReport> {
        val reports = mutableListOf<VerificationReport>()
        
        // Check for duplicate assignments
        val duplicateReport = verifyNoDuplicateAssignments()
        reports.add(duplicateReport)
        
        // Check for workload limits
        val workloadReport = verifyWorkloadLimits()
        reports.add(workloadReport)
        
        // Check for period conflicts
        val conflictReport = verifyNoConflicts()
        reports.add(conflictReport)
        
        return reports
    }

    /**
     * Verifies that there are no duplicate assignments.
     *
     * @return A verification report
     */
    private fun verifyNoDuplicateAssignments(): VerificationReport {
        val issues = mutableListOf<String>()
        val assignments = allAssignments
        
        // Check for duplicate assignments (same teacher, period, and day)
        val assignmentSet = HashSet<String>()
        var duplicates = 0
        
        for (assignment in assignments) {
            val key = "${assignment.day}:${assignment.period}:${assignment.originalTeacher}"
            if (assignmentSet.contains(key)) {
                issues.add("Duplicate assignment: ${assignment.originalTeacher} on ${assignment.day} period ${assignment.period}")
                duplicates++
            } else {
                assignmentSet.add(key)
            }
        }
        
        return VerificationReport(
            success = duplicates == 0,
            issues = issues,
            validAssignments = assignments.size - duplicates,
            invalidAssignments = duplicates
        )
    }

    /**
     * Verifies that no substitute exceeds their workload limit.
     *
     * @return A verification report
     */
    private fun verifyWorkloadLimits(): VerificationReport {
        val issues = mutableListOf<String>()
        var invalidAssignments = 0
        
        // Check substitute workload
        for ((subName, workload) in teacherWorkload) {
            val isSubstitute = substitutes.containsKey(subName)
            val maxWorkload = if (isSubstitute) MAX_SUBSTITUTE_ASSIGNMENTS else MAX_REGULAR_TEACHER_ASSIGNMENTS
            
            if (workload > maxWorkload) {
                issues.add("$subName has $workload assignments, exceeding the maximum of $maxWorkload")
                invalidAssignments += (workload - maxWorkload)
            }
        }
        
        return VerificationReport(
            success = issues.isEmpty(),
            issues = issues,
            validAssignments = allAssignments.size - invalidAssignments,
            invalidAssignments = invalidAssignments
        )
    }

    /**
     * Verifies that no substitute has conflicting assignments.
     *
     * @return A verification report
     */
    private fun verifyNoConflicts(): VerificationReport {
        val issues = mutableListOf<String>()
        var conflicts = 0
        
        // Group assignments by day and period
        val dayPeriodMap = HashMap<String, MutableSet<String>>()
        
        for (assignment in allAssignments) {
            val key = "${assignment.day}:${assignment.period}"
            if (!dayPeriodMap.containsKey(key)) {
                dayPeriodMap[key] = HashSet()
            }
            
            val subName = assignment.substitute
            if (dayPeriodMap[key]?.contains(subName) == true) {
                issues.add("$subName has multiple assignments on ${assignment.day} period ${assignment.period}")
                conflicts++
            } else {
                dayPeriodMap[key]?.add(subName)
            }
        }
        
        return VerificationReport(
            success = conflicts == 0,
            issues = issues,
            validAssignments = allAssignments.size - conflicts,
            invalidAssignments = conflicts
        )
    }

    /**
     * Gets a random teacher from the list of all teachers.
     * 
     * @return A random teacher
     */
    fun getRandomTeacher(): Teacher? {
        if (allTeachers.isEmpty()) return null
        val regularTeachers = allTeachers.filter { !it.isSubstitute }
        if (regularTeachers.isEmpty()) return null
        return regularTeachers.random()
    }
} 