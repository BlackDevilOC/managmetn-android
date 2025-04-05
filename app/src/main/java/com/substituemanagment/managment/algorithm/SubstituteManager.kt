package com.substituemanagment.managment.algorithm

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.substituemanagment.managment.algorithm.models.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

const val EXTERNAL_STORAGE_PATH = "/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data"
const val PROCESSED_DIR = "$EXTERNAL_STORAGE_PATH/processed"

// Default file paths
const val DEFAULT_TIMETABLE_PATH = "data/timetable_file.csv"
const val DEFAULT_SUBSTITUTES_PATH = "data/Substitude_file.csv"
const val DEFAULT_TEACHERS_PATH = "data/total_teacher.json"
const val DEFAULT_SCHEDULES_PATH = "data/teacher_schedules.json"
const val DEFAULT_ASSIGNED_TEACHERS_PATH = "data/assigned_substitute.json"

// Processed file paths
const val PROCESSED_TEACHERS_PATH = "$PROCESSED_DIR/total_teacher.json"
const val PROCESSED_SCHEDULES_PATH = "$PROCESSED_DIR/teacher_schedules.json"
const val PROCESSED_CLASS_SCHEDULES_PATH = "$PROCESSED_DIR/class_schedules.json"
const val PROCESSED_DAY_SCHEDULES_PATH = "$PROCESSED_DIR/day_schedules.json"
const val PROCESSED_PERIOD_SCHEDULES_PATH = "$PROCESSED_DIR/period_schedules.json"
const val PROCESSED_ASSIGNED_SUBSTITUTES_PATH = "$EXTERNAL_STORAGE_PATH/assigned_substitute.json"

// Constants
const val MAX_DAILY_WORKLOAD = 6
const val MAX_SUBSTITUTE_ASSIGNMENTS = 3
const val MAX_REGULAR_TEACHER_ASSIGNMENTS = 2
private const val TAG = "SubstituteManager"

class SubstituteManager(private val context: Context) {
    private val schedule: MutableMap<String, MutableMap<Int, List<String>>> = HashMap()
    private val substitutes: MutableMap<String, String> = HashMap()
    private val teacherClasses: MutableMap<String, MutableList<Assignment>> = HashMap()
    private val substituteAssignments: MutableMap<String, MutableList<Assignment>> = HashMap()
    private val teacherWorkload: MutableMap<String, Int> = HashMap()
    private val allAssignments: MutableList<Assignment> = ArrayList()
    private val allTeachers: MutableList<Teacher> = ArrayList()
    private val timetable: MutableList<Any> = ArrayList()
    private val absentTeachers: MutableMap<String, MutableSet<String>> = HashMap()
    
    // Fairness tracking
    private val teacherAssignmentCount: MutableMap<String, Int> = HashMap()
    private val lastAssignedDay: MutableMap<String, String> = HashMap()
    private val substitutionHistory: MutableList<SubstitutionRecord> = mutableListOf()

    // Track classes that couldn't be assigned substitutes
    private val unassignedClasses: MutableList<UnassignedClass> = ArrayList()

    data class SubstitutionRecord(
        val date: String,
        val teacher: String,
        val substitute: String,
        val period: Int,
        val className: String
    )

    // Data class for classes without substitutes
    data class UnassignedClass(
        val teacher: String,
        val day: String, 
        val period: Int, 
        val className: String
    )

    suspend fun loadData(
        timetablePath: String = DEFAULT_TIMETABLE_PATH,
        substitutesPath: String = DEFAULT_SUBSTITUTES_PATH,
        teachersPath: String = DEFAULT_TEACHERS_PATH,
        schedulesPath: String = DEFAULT_SCHEDULES_PATH,
        assignedTeachersPath: String = DEFAULT_ASSIGNED_TEACHERS_PATH
    ) {
        try {
            Log.d(TAG, "Loading data from processed files or assets")
            clearData()
            
            // Load teachers
            val processedTeachersFile = File(PROCESSED_TEACHERS_PATH)
            if (processedTeachersFile.exists()) {
                Log.d(TAG, "Loading teachers from processed file")
                loadTeachersFromJson(processedTeachersFile.readText())
            } else {
                Log.d(TAG, "Loading teachers from assets")
                val teachersContent = context.assets.open(teachersPath).bufferedReader().use { it.readText() }
                loadTeachersFromJson(teachersContent)
            }
            
            // Load schedules
            val processedSchedulesFile = File(PROCESSED_SCHEDULES_PATH)
            if (processedSchedulesFile.exists()) {
                Log.d(TAG, "Loading schedules from processed file")
                loadSchedulesFromJson(processedSchedulesFile.readText())
            } else {
                Log.d(TAG, "Loading schedules from assets")
                val schedulesContent = context.assets.open(schedulesPath).bufferedReader().use { it.readText() }
                loadSchedulesFromJson(schedulesContent)
            }
            
            // Load absent teachers data from assigned_substitute.json (this is critical for preventing absent teachers being assigned)
            loadAbsentTeachers()
            
            // First try to load from assigned_substitute.json (this is the primary source now)
            val assignedSubstitutesFile = File(PROCESSED_ASSIGNED_SUBSTITUTES_PATH)
            if (assignedSubstitutesFile.exists()) {
                Log.d(TAG, "Loading assignments from: $PROCESSED_ASSIGNED_SUBSTITUTES_PATH")
                loadAssignedSubstitutes(assignedSubstitutesFile.readText())
            } else {
                // Fall back to checking the old location if needed
                try {
                    val assignedTeachersFile = File(assignedTeachersPath)
                    if (assignedTeachersFile.exists()) {
                        Log.d(TAG, "Loading previously assigned teachers from legacy file")
                        loadAssignedTeachers(assignedTeachersFile.readText())
                    } else {
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
            }

            // Verify the integrity of our data after loading
            verifyDataIntegrity()
            
            Log.d(TAG, "Loaded ${substitutes.size} substitutes, ${allTeachers.size} teachers, schedules for ${teacherClasses.size} teachers")
            Log.d(TAG, "Loaded ${absentTeachers.flatMap { it.value }.size} absent teachers across all days")

        } catch (error: Exception) {
            Log.e(TAG, "Error loading data: ${error.message}")
            throw Exception("Error loading data: $error")
        }
    }
    
    private fun loadTeachersFromJson(content: String) {
        val teacherType = object : TypeToken<List<Map<String, Any>>>() {}.type
        val teachersData: List<Map<String, Any>> = Gson().fromJson(content, teacherType)
        
        for (teacherData in teachersData) {
            val name = teacherData["name"] as? String ?: continue
            val phone = teacherData["phone"] as? String ?: ""
            val isSubstitute = teacherData["isSubstitute"] as? Boolean ?: false
            val variations = teacherData["variations"] as? List<String> ?: listOf()
            
            val teacher = Teacher(
                name = name.trim(),
                phone = phone,
                isSubstitute = isSubstitute,
                variations = variations
            )
            allTeachers.add(teacher)
            
            if (isSubstitute) {
                substitutes[name.trim().lowercase()] = phone
            }
        }
    }
    
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
                    day = day.trim().lowercase(),
                    period = period,
                    className = className,
                    originalTeacher = originalTeacher.trim().lowercase(),
                    substitute = substitute.trim().lowercase()
                )
            }
            
            if (assignments.isNotEmpty()) {
                teacherClasses[teacherName.trim().lowercase()] = assignments.toMutableList()
            }
        }
    }
    
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
                    val date = assignment["date"] as? String ?: SimpleDateFormat("yyyy-MM-dd").format(Date())
                    
                    if (originalTeacher.isNotBlank() && substitute.isNotBlank()) {
                        val assignmentObj = Assignment(
                            day = "saturday",
                            period = period,
                            className = className,
                            originalTeacher = originalTeacher.trim().lowercase(),
                            substitute = substitute.trim().lowercase()
                        )
                        
                        substituteAssignments
                            .getOrPut(substitute.lowercase()) { ArrayList() }
                            .add(assignmentObj)
                        
                        teacherWorkload[substitute.lowercase()] = (teacherWorkload[substitute.lowercase()] ?: 0) + 1
                        teacherAssignmentCount[substitute.lowercase()] = (teacherAssignmentCount[substitute.lowercase()] ?: 0) + 1
                        lastAssignedDay[substitute.lowercase()] = date
                    }
                }
            }
        }
    }
    
    private fun loadAssignedSubstitutes(content: String) {
        try {
            val assignedType = object : TypeToken<Map<String, Any>>() {}.type
            val assignedData: Map<String, Any> = Gson().fromJson(content, assignedType)
            
            if (assignedData.containsKey("assignments")) {
                val assignments = assignedData["assignments"] as? List<*> ?: emptyList<Any>()
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                
                for (assignment in assignments) {
                    if (assignment is Map<*, *>) {
                        val originalTeacher = (assignment["originalTeacher"] as? String)?.lowercase() ?: continue
                        val period = (assignment["period"] as? Double)?.toInt() 
                            ?: (assignment["period"] as? Int) ?: continue
                        val className = assignment["className"] as? String ?: continue
                        val substitute = (assignment["substitute"] as? String)?.lowercase() ?: continue
                        val substitutePhone = assignment["substitutePhone"] as? String ?: ""
                        
                        // Record the original teacher as absent (for Saturday by default)
                        absentTeachers.getOrPut("saturday") { mutableSetOf() }.add(originalTeacher)
                        
                        // Create an assignment object
                        val assignmentObj = Assignment(
                            day = "saturday", // Default to Saturday for existing assignments
                            period = period,
                            className = className,
                            originalTeacher = originalTeacher,
                            substitute = substitute
                        )
                        
                        // Add to our assignments list
                        allAssignments.add(assignmentObj)
                        
                        // Update assignments and workload tracking
                        substituteAssignments
                            .getOrPut(substitute) { ArrayList() }
                            .add(assignmentObj)
                        
                        teacherWorkload[substitute] = (teacherWorkload[substitute] ?: 0) + 1
                        teacherAssignmentCount[substitute] = (teacherAssignmentCount[substitute] ?: 0) + 1
                        lastAssignedDay[substitute] = currentDate
                    }
                }
                
                Log.d(TAG, "Loaded ${allAssignments.size} assignments from assigned_substitute.json")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing assigned_substitute.json: ${e.message}")
        }
    }
    
    private fun clearData() {
        schedule.clear()
        substitutes.clear()
        teacherClasses.clear()
        substituteAssignments.clear()
        teacherWorkload.clear()
        allAssignments.clear()
        allTeachers.clear()
        timetable.clear()
        absentTeachers.clear()
        teacherAssignmentCount.clear()
        lastAssignedDay.clear()
        substitutionHistory.clear()
        unassignedClasses.clear()
    }

    fun assignSubstitutes(teacherName: String, day: String): List<SubstituteAssignment> {
        val normalizedTeacherName = normalizeName(teacherName)
        val normalizedDay = normalizeDay(day)
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        
        Log.d(TAG, "Assigning substitutes for $normalizedTeacherName on $normalizedDay")
        
        if (allTeachers.isEmpty()) {
            Log.e(TAG, "Teacher list is empty, cannot assign substitutes")
            return emptyList()
        }

        if (!teacherClasses.containsKey(normalizedTeacherName)) {
            Log.w(TAG, "Teacher $normalizedTeacherName not found in schedule")
            return emptyList()
        }

        // Check if teacher is already marked as absent
        if (isTeacherAlreadyAbsent(normalizedTeacherName, normalizedDay)) {
            Log.d(TAG, "Teacher $normalizedTeacherName already marked as absent for $normalizedDay")
        } else {
            // If not, mark them as absent and update all data structures
            markTeacherAbsent(normalizedTeacherName, normalizedDay)
        }

        // Get classes for this teacher on this day
        val classes = teacherClasses[normalizedTeacherName]?.filter { it.day == normalizedDay } ?: listOf()
        if (classes.isEmpty()) {
            Log.w(TAG, "No classes found for $normalizedTeacherName on $normalizedDay")
            return emptyList()
        }

        // Clear any existing unassigned classes for this teacher and day
        unassignedClasses.removeAll { it.teacher == normalizedTeacherName && it.day == normalizedDay }
        
        val assignments = mutableListOf<SubstituteAssignment>()
        val classesByPeriod = classes.groupBy { it.period }

        classesByPeriod.forEach { (period, classList) ->
            // Check if this period was already assigned for this teacher
            val existingAssignments = getExistingAssignmentsForTeacherAndPeriod(normalizedTeacherName, normalizedDay, period)
            if (existingAssignments.isNotEmpty()) {
                Log.d(TAG, "Period $period already has assignments for $normalizedTeacherName. Reusing existing assignments.")
                
                // Add existing assignments to our return list
                existingAssignments.forEach { existing ->
                    assignments.add(existing)
                }
            } else {
                // Try to find available substitutes (will automatically exclude absent teachers)
                val availableSubs = findAvailableSubstitutes(normalizedDay, period, normalizedTeacherName)
                
                if (availableSubs.isNotEmpty()) {
                    // Select the best substitute
                    val bestSubstitute = selectOptimalSubstitute(availableSubs, normalizedDay, period)
                    val subName = bestSubstitute.first
                    val subPhone = bestSubstitute.second

                    classList.forEach { clazz ->
                        val assignment = SubstituteAssignment(
                            originalTeacher = normalizedTeacherName,
                            period = period,
                            className = clazz.className,
                            substitute = subName,
                            substitutePhone = subPhone
                        )
                        
                        assignments.add(assignment)
                        updateAssignmentRecords(subName, normalizedDay, period, clazz.className, normalizedTeacherName)
                        updateTeacherWorkload(subName)
                        recordSubstitution(currentDate, normalizedTeacherName, subName, period, clazz.className)
                    }
                } else {
                    Log.w(TAG, "No available substitutes for period $period. Trying backup allocation.")
                    
                    // Try backup allocation with relaxed constraints
                    val backupAssignments = findAndAssignBackupSubstitutes(normalizedTeacherName, normalizedDay, period, classList)
                    if (backupAssignments.isNotEmpty()) {
                        assignments.addAll(backupAssignments)
                    } else {
                        Log.e(TAG, "Failed to find any substitutes for period $period even with relaxed constraints.")
                        
                        // Record these classes as unassigned
                        classList.forEach { clazz ->
                            unassignedClasses.add(
                                UnassignedClass(
                                    teacher = normalizedTeacherName,
                                    day = normalizedDay,
                                    period = period,
                                    className = clazz.className
                                )
                            )
                            Log.w(TAG, "Added to unassigned classes: ${clazz.className} for teacher $normalizedTeacherName period $period")
                        }
                    }
                }
            }
        }

        // Verify assignments before saving - fix any issues detected
        // ALWAYS run verification, not just at the end
        val verifiedAssignments = verifyAndCorrectAssignments(assignments, normalizedDay)
        
        // Save only if we have valid assignments or unassigned classes
        if (verifiedAssignments.isNotEmpty() || unassignedClasses.isNotEmpty()) {
            saveAssignments(verifiedAssignments)
            Log.i(TAG, "Successfully processed assignments for $normalizedTeacherName on $normalizedDay")
            
            if (unassignedClasses.isNotEmpty()) {
                Log.w(TAG, "${unassignedClasses.size} classes could not be assigned a substitute")
            }
        } else {
            Log.w(TAG, "No valid assignments could be made for $normalizedTeacherName on $normalizedDay")
        }
        
        return verifiedAssignments
    }

    /**
     * Mark a teacher as absent and update all related data structures
     */
    private fun markTeacherAbsent(teacherName: String, day: String) {
        val normalizedName = teacherName.lowercase()
        
        // Add to the absentTeachers map
        absentTeachers.getOrPut(day) { mutableSetOf() }.add(normalizedName)
        
        // Log the change
        Log.d(TAG, "Marked teacher $normalizedName as absent for $day")
        
        // Verify this teacher isn't already assigned as a substitute
        val existingAssignments = substituteAssignments[normalizedName]?.filter { it.day == day }
        if (!existingAssignments.isNullOrEmpty()) {
            Log.w(TAG, "Teacher $normalizedName is marked as absent but has ${existingAssignments.size} substitute assignments on $day")
            
            // These will be fixed during the verification pass
        }
    }

    /**
     * Checks if a teacher is already marked as absent for a specific day
     */
    private fun isTeacherAlreadyAbsent(teacherName: String, day: String): Boolean {
        val normalizedName = teacherName.lowercase()
        return absentTeachers[day]?.contains(normalizedName) == true
    }

    /**
     * Gets existing substitute assignments for a specific teacher, day, and period
     */
    private fun getExistingAssignmentsForTeacherAndPeriod(
        teacherName: String, 
        day: String, 
        period: Int
    ): List<SubstituteAssignment> {
        val result = mutableListOf<SubstituteAssignment>()
        
        // Look through all assignments
        for (assignment in allAssignments) {
            if (assignment.originalTeacher.lowercase() == teacherName.lowercase() && 
                assignment.day.lowercase() == day.lowercase() && 
                assignment.period == period) {
                
                // Find the substitute's phone number
                val substitutePhone = allTeachers
                    .find { it.name.lowercase() == assignment.substitute.lowercase() }
                    ?.phone ?: ""
                
                result.add(
                    SubstituteAssignment(
                        originalTeacher = assignment.originalTeacher,
                        period = assignment.period,
                        className = assignment.className,
                        substitute = assignment.substitute,
                        substitutePhone = substitutePhone
                    )
                )
            }
        }
        
        return result
    }

    /**
     * Checks if a teacher is teaching during a specific period
     */
    private fun isTeachingDuringPeriod(teacherName: String, day: String, period: Int): Boolean {
        return teacherClasses[teacherName]?.any { 
            it.day.lowercase() == day.lowercase() && it.period == period 
        } ?: false
    }

    /**
     * Finds backup substitutes when normal allocation fails
     * Uses relaxed constraints to ensure classes are covered
     */
    private fun findAndAssignBackupSubstitutes(
        teacherName: String,
        day: String,
        period: Int,
        classList: List<Assignment>
    ): List<SubstituteAssignment> {
        val result = mutableListOf<SubstituteAssignment>()
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        
        // Get all absent teachers for this day
        val absentTeachersForDay = absentTeachers[day] ?: emptySet()
        
        // Double check for absent teachers who might not be in the map
        val teachersWithSubstitutes = allAssignments
            .filter { it.day.lowercase() == day.lowercase() }
            .map { it.originalTeacher.lowercase() }
            .toSet()
        
        // Combine our known absent teachers with those who have substitutes
        val allAbsentTeachers = absentTeachersForDay.union(teachersWithSubstitutes)
        
        // First try: Find teachers who teach fewer classes (even if at workload limit)
        var backupTeachers = allTeachers
            .filter { teacher ->
                val name = teacher.name.lowercase()
                // ENHANCED: Exclude absent teachers and those already teaching this period
                name != teacherName.lowercase() &&
                !allAbsentTeachers.contains(name) &&
                !isTeachingDuringPeriod(name, day, period) &&
                !hasExistingAssignment(day, period, name)
            }
            .sortedBy { teacher -> teacherWorkload[teacher.name.lowercase()] ?: 0 }
        
        if (backupTeachers.isNotEmpty()) {
            val backupTeacher = backupTeachers.first()
            
            // Create assignments
            for (clazz in classList) {
                val assignment = SubstituteAssignment(
                    originalTeacher = teacherName,
                    period = period,
                    className = clazz.className,
                    substitute = backupTeacher.name,
                    substitutePhone = backupTeacher.phone
                )
                
                result.add(assignment)
                updateAssignmentRecords(
                    backupTeacher.name, day, period, 
                    clazz.className, teacherName
                )
                updateTeacherWorkload(backupTeacher.name)
                recordSubstitution(
                    currentDate, teacherName, backupTeacher.name, 
                    period, clazz.className
                )
            }
            
            Log.d(TAG, "Backup substitute ${backupTeacher.name} assigned for period $period")
        } else {
            // Extreme fallback: Find ANY teacher, even if teaching, prioritizing those with fewer classes
            backupTeachers = allTeachers
                .filter { teacher -> 
                    val name = teacher.name.lowercase()
                    // ENHANCED: Don't assign absent teachers even in extreme cases
                    name != teacherName.lowercase() && 
                    !allAbsentTeachers.contains(name)
                }
                .sortedBy { teacher -> teacherWorkload[teacher.name.lowercase()] ?: 0 }
            
            if (backupTeachers.isNotEmpty()) {
                val emergencyTeacher = backupTeachers.first()
                
                // Add warning log about this emergency assignment
                Log.w(TAG, "EMERGENCY ASSIGNMENT: ${emergencyTeacher.name} assigned for period $period despite constraints")
                
                // Create assignments
                for (clazz in classList) {
                    val assignment = SubstituteAssignment(
                        originalTeacher = teacherName,
                        period = period,
                        className = clazz.className,
                        substitute = emergencyTeacher.name,
                        substitutePhone = emergencyTeacher.phone
                    )
                    
                    result.add(assignment)
                    updateAssignmentRecords(
                        emergencyTeacher.name, day, period, 
                        clazz.className, teacherName
                    )
                    updateTeacherWorkload(emergencyTeacher.name)
                    recordSubstitution(
                        currentDate, teacherName, emergencyTeacher.name, 
                        period, clazz.className
                    )
                }
            } else {
                Log.e(TAG, "CRITICAL: No substitute available for period $period even with relaxed constraints")
            }
        }
        
        return result
    }

    /**
     * Verifies and corrects assignments before saving
     * Returns the corrected list of assignments
     */
    private fun verifyAndCorrectAssignments(
        assignments: List<SubstituteAssignment>,
        day: String
    ): List<SubstituteAssignment> {
        if (assignments.isEmpty()) {
            return emptyList()
        }
        
        // Create a mutable copy to work with
        val workingAssignments = assignments.toMutableList()
        
        // Check 1: No absent teachers as substitutes
        checkAndFixAbsentSubstitutes(workingAssignments, day)
        
        // Check 2: No duplicate assignments for the same substitute in the same period
        checkAndFixDuplicateAssignments(workingAssignments, day)
        
        // Check 3: No substitute assigned to a period they're already teaching
        checkAndFixTeachingConflicts(workingAssignments, day)
        
        // Final validation - log any remaining issues but return the assignments
        val validationResults = validateFinalAssignments(workingAssignments, day)
        if (!validationResults.allValid) {
            Log.w(TAG, "Some assignments still have issues: ${validationResults.issues.size} problems remain")
            validationResults.issues.forEach { Log.w(TAG, it) }
        }
        
        return workingAssignments
    }

    /**
     * Finds and fixes cases where absent teachers are assigned as substitutes
     */
    private fun checkAndFixAbsentSubstitutes(
        assignments: MutableList<SubstituteAssignment>,
        day: String
    ) {
        val absentTeachersForDay = absentTeachers[day] ?: emptySet()
        val problemAssignments = mutableListOf<Int>()
        
        // Find problematic assignments
        assignments.forEachIndexed { index, assignment ->
            val substituteLower = assignment.substitute.lowercase()
            if (absentTeachersForDay.contains(substituteLower)) {
                Log.w(TAG, "Substitute ${assignment.substitute} is marked as absent but assigned to ${assignment.originalTeacher}")
                problemAssignments.add(index)
            }
        }
        
        // Fix each issue (iterate in reverse to safely remove items)
        for (index in problemAssignments.sortedDescending()) {
            val assignment = assignments[index]
            
            // Try to find a replacement substitute
            val availableSubs = findAvailableSubstitutes(day, assignment.period, assignment.originalTeacher)
            
            if (availableSubs.isNotEmpty()) {
                // Choose the best substitute and replace
                val replacement = selectOptimalSubstitute(availableSubs, day, assignment.period)
                
                assignments[index] = SubstituteAssignment(
                    originalTeacher = assignment.originalTeacher,
                    period = assignment.period,
                    className = assignment.className,
                    substitute = replacement.first,
                    substitutePhone = replacement.second
                )
                
                Log.d(TAG, "Replaced absent substitute ${assignment.substitute} with ${replacement.first}")
            } else {
                // If no replacement found, try the backup method
                val backupAssignments = findAndAssignBackupSubstitutes(
                    assignment.originalTeacher, 
                    day, 
                    assignment.period, 
                    listOf(
                        Assignment(
                            day = day, 
                            period = assignment.period, 
                            className = assignment.className, 
                            originalTeacher = assignment.originalTeacher
                        )
                    )
                )
                
                if (backupAssignments.isNotEmpty()) {
                    assignments[index] = backupAssignments.first()
                } else {
                    // If still no substitute, just remove this assignment
                    assignments.removeAt(index)
                    Log.w(TAG, "Removed assignment for ${assignment.originalTeacher} period ${assignment.period} due to no available substitutes")
                }
            }
        }
    }

    /**
     * Finds and fixes duplicate assignments for the same substitute in the same period
     */
    private fun checkAndFixDuplicateAssignments(
        assignments: MutableList<SubstituteAssignment>,
        day: String
    ) {
        // Group by period and substitute
        val duplicateGroups = assignments.groupBy { "${it.period}:${it.substitute.lowercase()}" }
        
        // Find duplicates (where a substitute is assigned to multiple classes in the same period)
        val duplicates = duplicateGroups.filter { it.value.size > 1 }
        
        // Process duplicates
        duplicates.forEach { (key, duplicateAssignments) ->
            Log.w(TAG, "Found duplicate assignments: $key has ${duplicateAssignments.size} assignments")
            
            // Keep the first one, fix the rest
            val (period, _) = key.split(":")
            val periodInt = period.toInt()
            
            // Process duplicates starting from the second one
            for (i in 1 until duplicateAssignments.size) {
                val duplicate = duplicateAssignments[i]
                
                // Try to find an alternative substitute
                val availableSubs = findAvailableSubstitutes(day, periodInt, duplicate.originalTeacher)
                    .filter { it.first.lowercase() != duplicate.substitute.lowercase() } // Exclude current substitute
                
                if (availableSubs.isNotEmpty()) {
                    // Choose a new substitute
                    val newSubstitute = selectOptimalSubstitute(availableSubs, day, periodInt)
                    
                    // Find and update the assignment
                    val index = assignments.indexOfFirst { 
                        it.originalTeacher == duplicate.originalTeacher && 
                        it.period == duplicate.period && 
                        it.className == duplicate.className && 
                        it.substitute == duplicate.substitute 
                    }
                    
                    if (index >= 0) {
                        assignments[index] = SubstituteAssignment(
                            originalTeacher = duplicate.originalTeacher,
                            period = duplicate.period,
                            className = duplicate.className,
                            substitute = newSubstitute.first,
                            substitutePhone = newSubstitute.second
                        )
                        
                        Log.d(TAG, "Reassigned duplicate: ${duplicate.substitute} to ${newSubstitute.first} for period ${duplicate.period}")
                    }
                } else {
                    // Try backup method
                    val backupAssignments = findAndAssignBackupSubstitutes(
                        duplicate.originalTeacher, 
                        day, 
                        periodInt, 
                        listOf(
                            Assignment(
                                day = day, 
                                period = periodInt, 
                                className = duplicate.className, 
                                originalTeacher = duplicate.originalTeacher
                            )
                        )
                    )
                    
                    if (backupAssignments.isNotEmpty()) {
                        // Find and update the assignment
                        val index = assignments.indexOfFirst { 
                            it.originalTeacher == duplicate.originalTeacher && 
                            it.period == duplicate.period && 
                            it.className == duplicate.className && 
                            it.substitute == duplicate.substitute 
                        }
                        
                        if (index >= 0) {
                            assignments[index] = backupAssignments.first()
                        }
                    }
                }
            }
        }
    }

    /**
     * Finds and fixes cases where a substitute is assigned during a period they're already teaching
     */
    private fun checkAndFixTeachingConflicts(
        assignments: MutableList<SubstituteAssignment>,
        day: String
    ) {
        val conflictAssignments = mutableListOf<Int>()
        
        // Find conflicts
        assignments.forEachIndexed { index, assignment ->
            val substituteLower = assignment.substitute.lowercase()
            
            // Check if this substitute is teaching during this period
            if (isTeachingDuringPeriod(substituteLower, day, assignment.period)) {
                Log.w(TAG, "Substitute ${assignment.substitute} is already teaching during period ${assignment.period}")
                conflictAssignments.add(index)
            }
        }
        
        // Fix conflicts (iterate in reverse to safely remove items)
        for (index in conflictAssignments.sortedDescending()) {
            val assignment = assignments[index]
            
            // Try to find a replacement who's not teaching
            val availableSubs = findAvailableSubstitutes(day, assignment.period, assignment.originalTeacher)
            
            if (availableSubs.isNotEmpty()) {
                // Choose the best substitute and replace
                val replacement = selectOptimalSubstitute(availableSubs, day, assignment.period)
                
                assignments[index] = SubstituteAssignment(
                    originalTeacher = assignment.originalTeacher,
                    period = assignment.period,
                    className = assignment.className,
                    substitute = replacement.first,
                    substitutePhone = replacement.second
                )
                
                Log.d(TAG, "Replaced conflicting substitute ${assignment.substitute} with ${replacement.first}")
            } else {
                // If no replacement found, try relaxed constraints
                val backupAssignments = findAndAssignBackupSubstitutes(
                    assignment.originalTeacher, 
                    day, 
                    assignment.period, 
                    listOf(
                        Assignment(
                            day = day, 
                            period = assignment.period, 
                            className = assignment.className, 
                            originalTeacher = assignment.originalTeacher
                        )
                    )
                )
                
                if (backupAssignments.isNotEmpty()) {
                    assignments[index] = backupAssignments.first()
                } else {
                    // If still no substitute, just remove this assignment
                    assignments.removeAt(index)
                    Log.w(TAG, "Removed assignment for ${assignment.originalTeacher} period ${assignment.period} due to no available substitutes")
                }
            }
        }
    }

    /**
     * Final validation of assignments, logging any remaining issues
     */
    private fun validateFinalAssignments(
        assignments: List<SubstituteAssignment>,
        day: String
    ): ValidationResult {
        val issues = mutableListOf<String>()
        var allValid = true
        
        // Check 1: No absent teachers as substitutes
        assignments.forEach { assignment ->
            val substituteLower = assignment.substitute.lowercase()
            if (absentTeachers[day]?.contains(substituteLower) == true) {
                issues.add("Substitute ${assignment.substitute} is still marked as absent")
                allValid = false
            }
        }
        
        // Check 2: No duplicate assignments for same substitute in same period
        val seenPeriodSubstitutes = mutableMapOf<Pair<Int, String>, MutableList<String>>()
        
        assignments.forEach { assignment ->
            val key = assignment.period to assignment.substitute.lowercase()
            seenPeriodSubstitutes.getOrPut(key) { mutableListOf() }.add(assignment.className)
        }
        
        seenPeriodSubstitutes.forEach { (key, classes) ->
            if (classes.size > 1) {
                val (period, substitute) = key
                issues.add("Substitute $substitute still assigned to multiple classes (${classes.joinToString()}) in period $period")
                allValid = false
            }
        }
        
        // Check 3: No substitute teaching during their assigned period
        assignments.forEach { assignment ->
            val substituteLower = assignment.substitute.lowercase()
            if (isTeachingDuringPeriod(substituteLower, day, assignment.period)) {
                issues.add("Substitute ${assignment.substitute} is still teaching during period ${assignment.period}")
                allValid = false
            }
        }
        
        return ValidationResult(allValid, issues)
    }

    data class ValidationResult(
        val allValid: Boolean,
        val issues: List<String>
    )

    private fun selectOptimalSubstitute(
        availableSubs: List<Pair<String, String>>,
        day: String,
        period: Int
    ): Pair<String, String> {
        return availableSubs
            .sortedWith(compareBy(
                { teacherAssignmentCount[it.first.lowercase()] ?: 0 },
                { lastAssignedDay[it.first.lowercase()] },
                { if (substitutes.containsKey(it.first.lowercase())) 0 else 1 },
                { teacherWorkload[it.first.lowercase()] ?: 0 }
            ))
            .first()
    }

    private fun updateAssignmentRecords(
        subName: String,
        day: String,
        period: Int,
        className: String,
        originalTeacher: String
    ) {
        substituteAssignments
            .getOrPut(subName.lowercase()) { mutableListOf() }
            .add(
                Assignment(
                    day = day,
                    period = period,
                    className = className,
                    originalTeacher = originalTeacher,
                    substitute = subName
                )
            )
    }

    private fun updateTeacherWorkload(teacherName: String) {
        val normalizedName = teacherName.lowercase()
        teacherWorkload[normalizedName] = (teacherWorkload[normalizedName] ?: 0) + 1
        teacherAssignmentCount[normalizedName] = (teacherAssignmentCount[normalizedName] ?: 0) + 1
    }

    private fun recordSubstitution(date: String, teacher: String, substitute: String, period: Int, className: String) {
        substitutionHistory.add(
            SubstitutionRecord(
                date = date,
                teacher = teacher,
                substitute = substitute,
                period = period,
                className = className
            )
        )
    }

    private fun findAvailableSubstitutes(day: String, period: Int, absentTeacher: String): List<Pair<String, String>> {
        // Get all teachers who are teaching during this period
        val teachersTeachingThisPeriod = getTeachersTeachingInPeriod(day, period)
        
        // Get all absent teachers for this day
        val absentTeachersForDay = absentTeachers[day] ?: emptySet()
        
        // ENHANCE: Double check that our absent teachers set is comprehensive
        // This will catch any teachers who have been marked as needing subs in any way
        val teachersWithSubstitutes = allAssignments
            .filter { it.day.lowercase() == day.lowercase() }
            .map { it.originalTeacher.lowercase() }
            .toSet()
        
        // Combine our known absent teachers with those who have substitutes
        val allAbsentTeachers = absentTeachersForDay.union(teachersWithSubstitutes)
        
        // Log if we find inconsistencies
        if (teachersWithSubstitutes.minus(absentTeachersForDay).isNotEmpty()) {
            Log.w(TAG, "Found ${teachersWithSubstitutes.minus(absentTeachersForDay).size} teachers with substitutes not marked as absent")
        }
        
        return allTeachers
            .asSequence()
            .filter { teacher ->
                val nameLower = teacher.name.lowercase()
                
                when {
                    // Don't assign the absent teacher to themselves
                    nameLower == absentTeacher.lowercase() -> false
                    
                    // ENHANCED: Don't assign teachers who are marked as absent or have substitutes
                    allAbsentTeachers.contains(nameLower) -> false
                    
                    // Don't assign teachers who are already teaching during this period
                    teachersTeachingThisPeriod.contains(nameLower) -> false
                    
                    // Don't assign teachers who have reached their workload limit
                    isOverworked(teacher, nameLower) -> false
                    
                    // Don't assign teachers who already have an assignment for this period
                    hasExistingAssignment(day, period, nameLower) -> false
                    
                    // Otherwise, teacher is available
                    else -> true
                }
            }
            .map { teacher -> teacher.name to teacher.phone }
            .toList()
    }

    /**
     * Gets a list of teachers who are already teaching during a specific day and period
     */
    private fun getTeachersTeachingInPeriod(day: String, period: Int): Set<String> {
        val teachingTeachers = mutableSetOf<String>()
        
        // Check all teacher schedules for this day and period
        for ((teacherName, assignments) in teacherClasses) {
            // Skip if this is an absent teacher
            if (absentTeachers[day]?.contains(teacherName) == true) {
                continue
            }
            
            // Check if teacher has a class during this period
            if (assignments.any { it.day == day && it.period == period }) {
                teachingTeachers.add(teacherName)
            }
        }
        
        // Also add teachers who are already assigned as substitutes for this period
        for ((subName, subAssignments) in substituteAssignments) {
            if (subAssignments.any { it.day == day && it.period == period }) {
                teachingTeachers.add(subName)
            }
        }
        
        return teachingTeachers
    }

    /**
     * Checks if a teacher has reached their maximum workload
     */
    private fun isOverworked(teacher: Teacher, normalizedName: String): Boolean {
        val maxAssignments = if (teacher.isSubstitute) {
            MAX_SUBSTITUTE_ASSIGNMENTS
        } else {
            MAX_REGULAR_TEACHER_ASSIGNMENTS
        }
        return (teacherWorkload[normalizedName] ?: 0) >= maxAssignments
    }

    /**
     * Checks if a teacher already has a substitute assignment during this period
     */
    private fun hasExistingAssignment(day: String, period: Int, teacherName: String): Boolean {
        return substituteAssignments[teacherName]?.any { it.day == day && it.period == period } == true
    }

    /**
     * Gets all periods that a teacher is scheduled to teach on a specific day
     */
    fun getTeacherPeriodsForDay(teacherName: String, day: String): List<Int> {
        val normalizedTeacherName = normalizeName(teacherName)
        val normalizedDay = normalizeDay(day)
        
        return teacherClasses[normalizedTeacherName]
            ?.filter { it.day == normalizedDay }
            ?.map { it.period }
            ?: emptyList()
    }

    /**
     * Gets all teachers who are available (not teaching) during a specific period
     */
    fun getAvailableTeachersForPeriod(day: String, period: Int): List<Teacher> {
        val teachersTeachingThisPeriod = getTeachersTeachingInPeriod(day, period)
        val absentTeachersForDay = absentTeachers[day] ?: emptySet()
        
        return allTeachers.filter { teacher ->
            val nameLower = teacher.name.lowercase()
            !teachersTeachingThisPeriod.contains(nameLower) && 
            !absentTeachersForDay.contains(nameLower) &&
            !isOverworked(teacher, nameLower) &&
            !hasExistingAssignment(day, period, nameLower)
        }
    }

    private fun saveAssignments(assignments: List<SubstituteAssignment>) {
        try {
            // Read existing assignments if file exists
            val existingAssignments = mutableListOf<SubstituteAssignment>()
            val existingUnassigned = mutableListOf<UnassignedClass>()
            
            val assignedSubstitutesFile = File(PROCESSED_ASSIGNED_SUBSTITUTES_PATH)
            if (assignedSubstitutesFile.exists()) {
                try {
                    val content = assignedSubstitutesFile.readText()
                    val assignedType = object : TypeToken<Map<String, Any>>() {}.type
                    val assignedData: Map<String, Any> = Gson().fromJson(content, assignedType)
                    
                    if (assignedData.containsKey("assignments")) {
                        val previousAssignments = assignedData["assignments"] as? List<*> ?: emptyList<Any>()
                        
                        for (assignment in previousAssignments) {
                            if (assignment is Map<*, *>) {
                                val originalTeacher = assignment["originalTeacher"] as? String ?: continue
                                val period = (assignment["period"] as? Double)?.toInt() 
                                    ?: (assignment["period"] as? Int) ?: continue
                                val className = assignment["className"] as? String ?: continue
                                val substitute = assignment["substitute"] as? String ?: continue
                                val substitutePhone = assignment["substitutePhone"] as? String ?: ""
                                
                                existingAssignments.add(
                                    SubstituteAssignment(
                                        originalTeacher = originalTeacher,
                                        period = period,
                                        className = className,
                                        substitute = substitute,
                                        substitutePhone = substitutePhone
                                    )
                                )
                            }
                        }
                    }
                    
                    // Also load existing unassigned classes
                    if (assignedData.containsKey("unassignedClasses")) {
                        val previousUnassigned = assignedData["unassignedClasses"] as? List<*> ?: emptyList<Any>()
                        
                        for (item in previousUnassigned) {
                            if (item is Map<*, *>) {
                                val teacher = item["teacher"] as? String ?: continue
                                val day = item["day"] as? String ?: continue
                                val period = (item["period"] as? Double)?.toInt() 
                                    ?: (item["period"] as? Int) ?: continue
                                val className = item["className"] as? String ?: continue
                                
                                existingUnassigned.add(
                                    UnassignedClass(
                                        teacher = teacher,
                                        day = day,
                                        period = period,
                                        className = className
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading existing assignments: ${e.message}")
                    // Continue with empty list if there was an error
                }
            }
            
            // Add new assignments
            existingAssignments.addAll(assignments)
            
            // Merge existing unassigned with current unassigned
            // First remove duplicates that might have been fixed
            val currentUnassignedMap = unassignedClasses.associateBy { "${it.teacher}:${it.day}:${it.period}:${it.className}" }
            val existingUnassignedMap = existingUnassigned.associateBy { "${it.teacher}:${it.day}:${it.period}:${it.className}" }
            
            // Combine maps, with current values taking precedence
            val combinedUnassignedMap = existingUnassignedMap.toMutableMap()
            combinedUnassignedMap.putAll(currentUnassignedMap)
            
            // Create final unassigned list
            val combinedUnassigned = combinedUnassignedMap.values.toList()
            
            // Create output structure
            val outputData = mapOf(
                "assignments" to existingAssignments,
                "unassignedClasses" to combinedUnassigned,
                "warnings" to emptyList<String>()
            )
            
            // Ensure directory exists
            val baseDir = File(EXTERNAL_STORAGE_PATH)
            if (!baseDir.exists()) {
                baseDir.mkdirs()
            }
            
            // Write the combined assignments to file
            val json = GsonBuilder().setPrettyPrinting().create().toJson(outputData)
            File(PROCESSED_ASSIGNED_SUBSTITUTES_PATH).writeText(json)
            
            // Update all assignments list
            allAssignments.clear()
            for (assignment in existingAssignments) {
                allAssignments.add(
                    Assignment(
                        day = "saturday", // Default day for now
                        period = assignment.period,
                        className = assignment.className,
                        originalTeacher = assignment.originalTeacher.lowercase(),
                        substitute = assignment.substitute.lowercase()
                    )
                )
            }
            
            Log.d(TAG, "Saved ${existingAssignments.size} assignments (${assignments.size} new) and " +
                    "${combinedUnassigned.size} unassigned classes to $PROCESSED_ASSIGNED_SUBSTITUTES_PATH")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving assignments: ${e.message}")
        }
    }

    fun getSubstituteAssignments(): Map<String, Any> {
        return mapOf(
            "assignments" to allAssignments,
            "warnings" to verifyAssignments().flatMap { it.issues }
        )
    }

    fun clearAssignments() {
        substituteAssignments.clear()
        teacherWorkload.clear()
        allAssignments.clear()
        unassignedClasses.clear()
        Log.d(TAG, "Cleared all assignments and unassigned classes")
    }

    fun verifyAssignments(): List<VerificationReport> {
        return listOf(
            verifyNoDuplicateAssignments(),
            verifyWorkloadLimits(),
            verifyNoConflicts(),
            verifyNoAbsentTeachersAsSubstitutes()
        )
    }

    fun validateAssignments(): List<VerificationReport> {
        return verifyAssignments()
    }

    private fun verifyNoAbsentTeachersAsSubstitutes(): VerificationReport {
        val issues = mutableListOf<String>()
        var invalid = 0
        
        allAssignments.forEach { assignment ->
            val subLower = assignment.substitute.lowercase()
            val day = assignment.day.lowercase()
            if (absentTeachers[day]?.contains(subLower) == true) {
                issues.add("Substitute '${assignment.substitute}' is absent on $day but assigned to ${assignment.originalTeacher}")
                invalid++
            }
        }
        
        return VerificationReport(
            success = issues.isEmpty(),
            issues = issues,
            validAssignments = allAssignments.size - invalid,
            invalidAssignments = invalid
        )
    }

    private fun verifyNoDuplicateAssignments(): VerificationReport {
        val issues = mutableListOf<String>()
        val assignmentSet = HashSet<String>()
        var duplicates = 0
        
        for (assignment in allAssignments) {
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
            validAssignments = allAssignments.size - duplicates,
            invalidAssignments = duplicates
        )
    }

    private fun verifyWorkloadLimits(): VerificationReport {
        val issues = mutableListOf<String>()
        var invalidAssignments = 0
        
        for ((teacherName, workload) in teacherWorkload) {
            val isSubstitute = substitutes.containsKey(teacherName)
            val maxWorkload = if (isSubstitute) MAX_SUBSTITUTE_ASSIGNMENTS else MAX_REGULAR_TEACHER_ASSIGNMENTS
            
            if (workload > maxWorkload) {
                issues.add("$teacherName has $workload assignments, exceeding the maximum of $maxWorkload")
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

    private fun verifyNoConflicts(): VerificationReport {
        val issues = mutableListOf<String>()
        var conflicts = 0
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

    fun getRandomTeacher(): Teacher? {
        if (allTeachers.isEmpty()) return null
        val regularTeachers = allTeachers.filter { !it.isSubstitute }
        if (regularTeachers.isEmpty()) return null
        return regularTeachers.random()
    }

    private fun normalizeDay(day: String): String {
        return day.trim().lowercase()
    }

    private fun normalizeName(name: String): String {
        return name.trim().lowercase()
    }

    /**
     * Loads absent teachers data from existing assignments
     */
    private fun loadAbsentTeachers() {
        try {
            val assignedSubstitutesFile = File(PROCESSED_ASSIGNED_SUBSTITUTES_PATH)
            if (!assignedSubstitutesFile.exists()) {
                Log.d(TAG, "No assigned_substitute.json file found for loading absent teachers")
                return
            }
            
            val content = assignedSubstitutesFile.readText()
            val assignedType = object : TypeToken<Map<String, Any>>() {}.type
            val assignedData: Map<String, Any> = Gson().fromJson(content, assignedType)
            
            if (assignedData.containsKey("assignments")) {
                val assignments = assignedData["assignments"] as? List<*> ?: emptyList<Any>()
                
                // Extract all original teachers who are absent (need substitutes)
                for (assignment in assignments) {
                    if (assignment is Map<*, *>) {
                        val originalTeacher = (assignment["originalTeacher"] as? String)?.lowercase() ?: continue
                        
                        // Mark teacher as absent (for Saturday by default)
                        absentTeachers.getOrPut("saturday") { mutableSetOf() }.add(originalTeacher)
                    }
                }
                
                Log.d(TAG, "Loaded ${absentTeachers.getOrDefault("saturday", emptySet()).size} absent teachers from assigned_substitute.json")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading absent teachers: ${e.message}")
        }
    }

    /**
     * Verifies that our loaded data is consistent and properly populated
     */
    private fun verifyDataIntegrity() {
        // Ensure all teachers in absentTeachers actually exist in allTeachers
        for ((day, teachers) in absentTeachers) {
            val nonExistentTeachers = teachers.filter { absentTeacher ->
                !allTeachers.any { it.name.lowercase() == absentTeacher.lowercase() }
            }
            
            if (nonExistentTeachers.isNotEmpty()) {
                Log.w(TAG, "Found ${nonExistentTeachers.size} non-existent teachers marked as absent on $day: ${nonExistentTeachers.joinToString()}")
            }
        }
        
        // Ensure we don't have teaching assignments for absent teachers
        for ((day, absentTeachersForDay) in absentTeachers) {
            for (teacherName in absentTeachersForDay) {
                val assignments = substituteAssignments[teacherName]
                if (assignments != null && assignments.any { it.day == day }) {
                    Log.w(TAG, "Inconsistency detected: $teacherName is marked as absent on $day but also has substitute assignments")
                }
            }
        }
        
        // Log summary of our data
        Log.d(TAG, "Data integrity check complete.")
        Log.d(TAG, "Total teachers: ${allTeachers.size}")
        Log.d(TAG, "Total substitutes: ${substitutes.size}")
        Log.d(TAG, "Total assignments: ${allAssignments.size}")
        for ((day, teachers) in absentTeachers) {
            Log.d(TAG, "Absent teachers on $day: ${teachers.size}")
        }
    }
}