package com.substituemanagment.managment.algorithm

import android.content.Context
import android.util.Log
import com.google.gson.Gson
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
const val DEFAULT_ASSIGNED_TEACHERS_PATH = "data/assigned_teacher.json"

// Processed file paths
const val PROCESSED_TEACHERS_PATH = "$PROCESSED_DIR/total_teacher.json"
const val PROCESSED_SCHEDULES_PATH = "$PROCESSED_DIR/teacher_schedules.json"
const val PROCESSED_CLASS_SCHEDULES_PATH = "$PROCESSED_DIR/class_schedules.json"
const val PROCESSED_DAY_SCHEDULES_PATH = "$PROCESSED_DIR/day_schedules.json"
const val PROCESSED_PERIOD_SCHEDULES_PATH = "$PROCESSED_DIR/period_schedules.json"

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

    data class SubstitutionRecord(
        val date: String,
        val teacher: String,
        val substitute: String,
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
            
            // Load assigned teachers
            try {
                val assignedTeachersFile = File(assignedTeachersPath)
                if (assignedTeachersFile.exists()) {
                    Log.d(TAG, "Loading previously assigned teachers")
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

            Log.d(TAG, "Loaded ${substitutes.size} substitutes, ${allTeachers.size} teachers, schedules for ${teacherClasses.size} teachers")

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
    }

    fun assignSubstitutes(teacherName: String, day: String): List<SubstituteAssignment> {
        val normalizedTeacherName = normalizeName(teacherName)
        val normalizedDay = normalizeDay(day)
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        if (normalizedTeacherName.isBlank()) {
            Log.w(TAG, "Blank teacher name provided")
            return emptyList()
        }

        if (!teacherClasses.containsKey(normalizedTeacherName)) {
            Log.w(TAG, "Teacher $normalizedTeacherName not found in schedule")
            return emptyList()
        }

        absentTeachers.getOrPut(normalizedDay) { mutableSetOf() }.add(normalizedTeacherName)

        val classes = teacherClasses[normalizedTeacherName]?.filter { it.day == normalizedDay } ?: listOf()
        if (classes.isEmpty()) {
            Log.w(TAG, "No classes found for $normalizedTeacherName on $normalizedDay")
            return emptyList()
        }

        val assignments = mutableListOf<SubstituteAssignment>()
        val classesByPeriod = classes.groupBy { it.period }

        classesByPeriod.forEach { (period, classList) ->
            val availableSubs = findAvailableSubstitutes(normalizedDay, period, normalizedTeacherName)
            
            if (availableSubs.isNotEmpty()) {
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
                Log.w(TAG, "No available substitutes for period $period")
            }
        }

        saveAssignments(assignments)
        return assignments
    }

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

    private fun recordSubstitution(
        date: String,
        teacher: String,
        substitute: String,
        period: Int,
        className: String
    ) {
        substitutionHistory.add(
            SubstitutionRecord(
                date = date,
                teacher = teacher,
                substitute = substitute,
                period = period,
                className = className
            )
        )
        lastAssignedDay[substitute.lowercase()] = date
    }

    private fun findAvailableSubstitutes(day: String, period: Int, absentTeacher: String): List<Pair<String, String>> {
        return allTeachers
            .asSequence()
            .filter { teacher ->
                val nameLower = teacher.name.lowercase()
                
                when {
                    nameLower == absentTeacher.lowercase() -> false
                    absentTeachers[day]?.contains(nameLower) == true -> false
                    isTeacherScheduled(day, period, nameLower) -> false
                    isOverworked(teacher, nameLower) -> false
                    hasExistingAssignment(day, period, nameLower) -> false
                    else -> true
                }
            }
            .map { teacher -> teacher.name to teacher.phone }
            .toList()
    }

    private fun isTeacherScheduled(day: String, period: Int, teacherName: String): Boolean {
        return schedule[day]?.get(period)?.any { it.lowercase() == teacherName } == true
    }

    private fun isOverworked(teacher: Teacher, normalizedName: String): Boolean {
        val maxAssignments = if (teacher.isSubstitute) {
            MAX_SUBSTITUTE_ASSIGNMENTS
        } else {
            MAX_REGULAR_TEACHER_ASSIGNMENTS
        }
        return (teacherWorkload[normalizedName] ?: 0) >= maxAssignments
    }

    private fun hasExistingAssignment(day: String, period: Int, teacherName: String): Boolean {
        return substituteAssignments[teacherName]?.any { it.day == day && it.period == period } == true
    }

    private fun saveAssignments(assignments: List<SubstituteAssignment>) {
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
            
            Log.d(TAG, "Saved ${assignments.size} assignments")
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
        Log.d(TAG, "Cleared all assignments")
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
}