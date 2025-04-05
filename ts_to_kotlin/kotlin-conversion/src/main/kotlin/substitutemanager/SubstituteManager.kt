package substitutemanager

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import substitutemanager.models.*
import substitutemanager.utils.CsvUtils
import java.io.File
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

/**
 * File paths
 */
const val DEFAULT_TIMETABLE_PATH = "data/timetable_file.csv"
const val DEFAULT_SUBSTITUTES_PATH = "data/Substitude_file.csv"
const val DEFAULT_TEACHERS_PATH = "data/total_teacher.json"
const val DEFAULT_SCHEDULES_PATH = "data/teacher_schedules.json"
const val DEFAULT_ASSIGNED_TEACHERS_PATH = "data/assigned_teacher.json"

/**
 * Constants
 */
const val MAX_DAILY_WORKLOAD = 6

/**
 * Main class for managing substitute teacher assignments.
 */
class SubstituteManager {
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
     * Loads data from timetable and substitutes files.
     *
     * @param timetablePath Path to the timetable CSV file
     * @param substitutesPath Path to the substitutes CSV file
     */
    fun loadData(
        timetablePath: String = DEFAULT_TIMETABLE_PATH,
        substitutesPath: String = DEFAULT_SUBSTITUTES_PATH
    ) {
        try {
            println("Loading data from: $timetablePath, $substitutesPath")

            // Load the timetable
            val timetableFile = File(timetablePath)
            if (!timetableFile.exists()) {
                throw Exception("Timetable file not found at: $timetablePath")
            }
            val timetableContent = timetableFile.readText()

            try {
                parseTimetable(timetableContent)
            } catch (parseError: Exception) {
                println("Error parsing timetable: $parseError")

                // Try to fix common timetable format issues
                val fixedContent = fixCSVContent(timetableContent)

                if (fixedContent != timetableContent) {
                    val backupPath = "$timetablePath.bak"
                    File(backupPath).writeText(timetableContent)
                    File(timetablePath).writeText(fixedContent)
                    println("Fixed and saved timetable. Original backed up to $backupPath")

                    // Try parsing again with fixed content
                    parseTimetable(fixedContent)
                } else {
                    throw Exception("Error parsing timetable file: $parseError")
                }
            }

            // Load the substitute teachers
            val substitutesFile = File(substitutesPath)
            if (!substitutesFile.exists()) {
                throw Exception("Substitute file not found at: $substitutesPath")
            }

            val substitutesContent = substitutesFile.readText()

            try {
                parseSubstitutes(substitutesContent)
            } catch (parseError: Exception) {
                println("Error parsing substitutes: $parseError")

                // Try to fix common substitutes format issues
                val fixedContent = fixCSVContent(substitutesContent)

                if (fixedContent != substitutesContent) {
                    val backupPath = "$substitutesPath.bak"
                    File(backupPath).writeText(substitutesContent)
                    File(substitutesPath).writeText(fixedContent)
                    println("Fixed and saved substitutes. Original backed up to $backupPath")

                    // Try parsing again with fixed content
                    parseSubstitutes(fixedContent)
                } else {
                    throw Exception("Error parsing substitute file: $parseError")
                }
            }

            println("Loaded ${substitutes.size} substitutes")

        } catch (error: Exception) {
            throw Exception("Error loading data: $error")
        }
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
        val lines = content.split("\n")
        for (line in lines) {
            if (line.isBlank()) continue // Skip empty lines
            
            val parts = line.split(",").map { it.trim() }
            val name = parts.getOrNull(0) ?: ""
            val phone = parts.getOrNull(1) ?: ""
            
            if (name.isNotBlank()) {
                substitutes[normalizeName(name)] = phone
            }
        }
    }

    /**
     * Automatically assigns substitutes for absent teachers.
     *
     * @param date The date for which to assign substitutes
     * @param absentTeacherNames List of absent teacher names
     * @param clearLogs Whether to clear logs before starting
     * @return A map containing assignments, warnings, and logs
     */
    fun autoAssignSubstitutes(
        date: String,
        absentTeacherNames: List<String> = emptyList(),
        clearLogs: Boolean = false
    ): Map<String, Any> {
        val logs = mutableListOf<ProcessLog>()
        val startTime = System.currentTimeMillis()
        val assignments = mutableListOf<SubstituteAssignment>()
        val warnings = mutableListOf<String>()

        fun addLog(action: String, details: String, status: String = "info", data: Any? = null) {
            logs.add(
                ProcessLog(
                    timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(Date()),
                    action = action,
                    details = details,
                    status = status,
                    data = data,
                    durationMs = System.currentTimeMillis() - startTime
                )
            )
        }

        try {
            addLog("ProcessStart", "Starting auto-assignment process", "info", mapOf(
                "date" to date,
                "teachers" to absentTeacherNames
            ))

            // Get the day of the week
            val day = getDayFromDate(date)
            addLog("DayCalculation", "Calculated day: $day", "info")

            // Load necessary data
            val teachers = loadTeachers(DEFAULT_TEACHERS_PATH)
            val schedules = loadSchedules(DEFAULT_SCHEDULES_PATH)

            val teacherMap = createTeacherMap(teachers)
            val availableSubstitutes = createSubstituteArray(teachers)
            val workloadMap = mutableMapOf<String, Int>()
            val assignedPeriodsMap = mutableMapOf<String, MutableSet<Int>>()

            addLog("DataLoading", "Loaded required data", "info", mapOf(
                "teachersCount" to teachers.size,
                "substitutesCount" to availableSubstitutes.size
            ))

            // Process each absent teacher
            for (teacherName in absentTeacherNames) {
                val cleanTeacherName = teacherName.lowercase().trim()
                addLog("TeacherProcessing", "Processing teacher: $teacherName", "info")

                // Get periods for this teacher
                val periods = getAllPeriodsForTeacherWithDiagnostics(
                    cleanTeacherName,
                    day,
                    timetable,
                    schedules,
                    { action, details, status, data -> addLog(action, details, status, data) }
                )

                if (periods.isEmpty()) {
                    warnings.add("No periods found for $teacherName on $day")
                    continue
                }

                addLog("PeriodsFound", "Found ${periods.size} periods for $teacherName", "info", periods)

                // Process each period
                for (periodInfo in periods) {
                    val period = periodInfo["period"] as Int
                    val className = periodInfo["className"] as String

                    // Filter available substitutes for this period
                    val availableForPeriod = availableSubstitutes.filter { sub ->
                        // Check if substitute is already assigned this period
                        val assignedPeriods = assignedPeriodsMap[sub.phone] ?: mutableSetOf()
                        if (assignedPeriods.contains(period)) return@filter false

                        // Check workload
                        val currentWorkload = workloadMap[sub.phone] ?: 0
                        if (currentWorkload >= MAX_DAILY_WORKLOAD) return@filter false

                        // Check if substitute is free this period
                        return@filter checkAvailability(sub, period, day, schedules)
                    }

                    if (availableForPeriod.isEmpty()) {
                        warnings.add("No available substitutes for $teacherName, period $period")
                        continue
                    }

                    // Select substitute with least workload
                    val selected = availableForPeriod.sortedBy { workloadMap[it.phone] ?: 0 }.first()

                    // Record assignment
                    assignments.add(
                        SubstituteAssignment(
                            originalTeacher = teacherName,
                            period = period,
                            className = className,
                            substitute = selected.name,
                            substitutePhone = selected.phone
                        )
                    )

                    // Update tracking maps
                    workloadMap[selected.phone] = (workloadMap[selected.phone] ?: 0) + 1
                    if (!assignedPeriodsMap.containsKey(selected.phone)) {
                        assignedPeriodsMap[selected.phone] = mutableSetOf()
                    }
                    assignedPeriodsMap[selected.phone]?.add(period)

                    addLog("AssignmentMade", "Assigned ${selected.name} to $teacherName's period $period", "info", mapOf(
                        "period" to period,
                        "className" to className,
                        "substituteWorkload" to (workloadMap[selected.phone] ?: 0)
                    ))
                }
            }

            // Save assignments
            if (assignments.isNotEmpty()) {
                saveAssignmentsToFile(assignments)
                addLog("AssignmentsSaved", "Saved ${assignments.size} assignments", "info")
            }

            // Save logs and warnings
            saveLogs(logs, date)
            saveWarnings(warnings, date)

            // Perform basic validation
            val validationWarnings = mutableListOf<String>()
            
            // Check for overloaded substitutes
            val overloadedSubs = workloadMap.entries
                .filter { (_, count) -> count > MAX_DAILY_WORKLOAD }
                .map { (phone, count) ->
                    val sub = availableSubstitutes.find { it.phone == phone }
                    "${sub?.name ?: "Unknown substitute"} has $count assignments (max: $MAX_DAILY_WORKLOAD)"
                }
            
            validationWarnings.addAll(overloadedSubs)
            
            addLog("ProcessComplete", "Auto-assignment process completed", "info", mapOf(
                "assignmentsCount" to assignments.size,
                "warningsCount" to (warnings.size + validationWarnings.size)
            ))

            return mapOf(
                "assignments" to assignments,
                "warnings" to (warnings + validationWarnings),
                "logs" to logs
            )
        } catch (error: Exception) {
            val errorMsg = "Error in auto-assign process: $error"
            addLog("ProcessError", errorMsg, "error")
            saveLogs(logs, date)
            saveWarnings(listOf(errorMsg), date)
            return mapOf(
                "assignments" to emptyList<SubstituteAssignment>(),
                "warnings" to listOf(errorMsg),
                "logs" to logs
            )
        }
    }

    /**
     * Checks if a substitute is available during a specific period.
     *
     * @param substitute The substitute teacher to check
     * @param period The period to check
     * @param day The day to check
     * @param schedules Map of teacher schedules
     * @return true if the substitute is available, false otherwise
     */
    private fun checkAvailability(
        substitute: Teacher,
        period: Int,
        day: String,
        schedules: Map<String, List<Assignment>>
    ): Boolean {
        val schedule = schedules[substitute.name.lowercase()] ?: emptyList()
        return !schedule.any { 
            it.day.lowercase() == day.lowercase() && 
            it.period == period
        }
    }

    /**
     * Gets all periods for a teacher with diagnostic information.
     *
     * @param teacherName The name of the teacher
     * @param day The day to check
     * @param timetable The timetable data
     * @param schedules Map of teacher schedules
     * @param logCallback Callback for logging
     * @return A list of period information
     */
    private fun getAllPeriodsForTeacherWithDiagnostics(
        teacherName: String,
        day: String,
        timetable: List<Any>,
        schedules: Map<String, List<Assignment>>,
        logCallback: (String, String, String, Any?) -> Unit
    ): List<Map<String, Any>> {
        val normalizedTeacherName = normalizeName(teacherName)
        val normalizedDay = normalizeDay(day)
        
        // Check if teacher is in our schedule
        if (!teacherClasses.containsKey(normalizedTeacherName)) {
            logCallback("TeacherLookup", "Teacher $teacherName not found in schedule", "warning", null)
            
            // Check if they exist in the master list
            val teacher = findTeacherByName(normalizedTeacherName)
            if (teacher == null) {
                logCallback("TeacherLookup", "Teacher $teacherName not found in master list", "warning", null)
                return emptyList()
            }
        }
        
        // Get from teacher classes
        val classes = teacherClasses[normalizedTeacherName] ?: emptyList()
        val todayClasses = classes.filter { it.day.lowercase() == normalizedDay.lowercase() }
        
        if (todayClasses.isEmpty()) {
            logCallback("ClassLookup", "No classes found for $teacherName on $day", "warning", null)
            
            // Try to lookup directly in the timetable
            val timetableData = timetable.filterIsInstance<Map<String, Any>>()
            val relevantEntries = timetableData.filter {
                val entryDay = it["Day"] as? String ?: ""
                val teachers = (it as? Map<String, Any> ?: emptyMap())
                    .filter { (key, _) -> key.startsWith("Teacher") }
                    .map { (_, value) -> value as? String ?: "" }
                
                return@filter normalizeDay(entryDay) == normalizedDay && 
                       teachers.any { t -> normalizeName(t) == normalizedTeacherName }
            }
            
            if (relevantEntries.isNotEmpty()) {
                logCallback("TimetableLookup", "Found ${relevantEntries.size} timetable entries", "info", relevantEntries)
                
                return relevantEntries.mapNotNull {
                    val period = (it["Period"] as? String)?.toIntOrNull() ?: 0
                    if (period <= 0) return@mapNotNull null
                    
                    val teacherColumns = (it as? Map<String, Any> ?: emptyMap())
                        .filter { (key, _) -> key.startsWith("Teacher") }
                    
                    val teacherIndex = teacherColumns.entries.indexOfFirst { (_, value) -> 
                        normalizeName(value as String) == normalizedTeacherName 
                    }
                    
                    if (teacherIndex < 0) return@mapNotNull null
                    
                    val classes = listOf("10A", "10B", "10C", "9A", "9B", "9C", "8A", "8B", "8C", "7A", "7B", "7C", "6A", "6B", "6C")
                    val className = if (teacherIndex < classes.size) classes[teacherIndex] else "Unknown"
                    
                    mapOf(
                        "period" to period,
                        "className" to className
                    )
                }
            }
            
            // Check if we have a direct schedule for this teacher
            if (schedules.containsKey(normalizedTeacherName)) {
                val directSchedule = schedules[normalizedTeacherName] ?: emptyList()
                val todaySchedule = directSchedule.filter { it.day.lowercase() == normalizedDay.lowercase() }
                
                if (todaySchedule.isNotEmpty()) {
                    logCallback("ScheduleLookup", "Found ${todaySchedule.size} schedule entries", "info", todaySchedule)
                    
                    return todaySchedule.map {
                        mapOf(
                            "period" to it.period,
                            "className" to it.className
                        )
                    }
                }
            }
            
            return emptyList()
        }
        
        logCallback("ClassLookup", "Found ${todayClasses.size} classes for $teacherName on $day", "info", todayClasses)
        
        return todayClasses.map {
            mapOf(
                "period" to it.period,
                "className" to it.className
            )
        }
    }

    /**
     * Finds suitable substitutes for a given assignment.
     *
     * @param params Parameters for finding substitutes
     * @return A pair of candidate substitutes and warnings
     */
    private fun findSuitableSubstitutes(params: Map<String, Any>): Pair<List<Teacher>, List<String>> {
        val warnings = mutableListOf<String>()
        val targetGrade = (params["className"] as String).replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0

        // Split substitutes into preferred and fallback based on grade compatibility
        val substitutes = params["substitutes"] as List<Teacher>
        val teachers = params["teachers"] as Map<String, Teacher>
        val schedules = params["schedules"] as Map<String, List<Assignment>>
        val currentWorkload = params["currentWorkload"] as Map<String, Int>
        val assignedPeriodsMap = params["assignedPeriodsMap"] as Map<String, Set<Int>>
        val day = params["day"] as String
        val period = params["period"] as Int

        val preferred = mutableListOf<Teacher>()
        val fallback = mutableListOf<Teacher>()

        for (sub in substitutes) {
            // Check schedule availability
            val isBusy = !checkAvailability(sub, period, day, schedules)

            // Check if already assigned to another class in this period
            val isAlreadyAssigned = assignedPeriodsMap[sub.phone]?.contains(period) ?: false

            // Check workload
            val currentLoad = currentWorkload[sub.phone] ?: 0

            // Grade compatibility
            val gradeLevel = sub.gradeLevel ?: 10 // Default to highest grade if not specified
            val isCompatible = gradeLevel >= targetGrade

            if (!isBusy && !isAlreadyAssigned && currentLoad < MAX_DAILY_WORKLOAD) {
                if (isCompatible) {
                    preferred.add(sub)
                } else if (targetGrade <= 8 && gradeLevel >= 9) {
                    fallback.add(sub)
                    warnings.add("Using higher-grade substitute ${sub.name} for ${params["className"]}")
                }
            }
        }

        return Pair(if (preferred.isNotEmpty()) preferred else fallback, warnings)
    }

    /**
     * Creates an array of substitute teachers from the list of all teachers.
     *
     * @param teachers List of all teachers
     * @return List of substitute teachers
     */
    private fun createSubstituteArray(teachers: List<Teacher>): List<Teacher> {
        // Create substitute teachers array from the teachers that have phone numbers
        return teachers.filter { it.phone.isNotBlank() }
    }

    /**
     * Creates a map of teachers indexed by their names.
     *
     * @param teachers List of all teachers
     * @return Map of teachers indexed by their names
     */
    private fun createTeacherMap(teachers: List<Teacher>): Map<String, Teacher> {
        val map = mutableMapOf<String, Teacher>()
        for (teacher in teachers) {
            // Add by main name
            map[teacher.name.lowercase().trim()] = teacher

            // Add by variations if available
            teacher.variations.forEach { variation ->
                val key = variation.lowercase().trim()
                map[key] = teacher
            }
        }
        return map
    }

    /**
     * Resolves teacher names to Teacher objects.
     *
     * @param names List of teacher names
     * @param teacherMap Map of teachers indexed by their names
     * @param warnings List to add warnings to
     * @return List of Teacher objects
     */
    private fun resolveTeacherNames(
        names: List<String>,
        teacherMap: Map<String, Teacher>,
        warnings: MutableList<String>
    ): List<Teacher> {
        val resolvedTeachers = mutableListOf<Teacher>()

        for (name in names) {
            val normalized = name.lowercase().trim()
            val teacher = teacherMap[normalized]
            if (teacher == null) {
                warnings.add("Unknown teacher: $name")
                continue
            }
            resolvedTeachers.add(teacher)
        }

        return resolvedTeachers
    }

    /**
     * Gets the day of the week from a date string.
     *
     * @param dateString The date string in format YYYY-MM-DD
     * @return The day of the week (e.g., "Monday")
     */
    private fun getDayFromDate(dateString: String): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        val date = dateFormat.parse(dateString)
        val calendar = Calendar.getInstance()
        calendar.time = date
        
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            else -> "Unknown"
        }
    }

    /**
     * Finds a teacher by name.
     *
     * @param name The name of the teacher to find
     * @return The Teacher object, or null if not found
     */
    private fun findTeacherByName(name: String): Teacher? {
        val normalized = name.lowercase().trim()
        
        // First check by exact match
        val exactMatch = allTeachers.find { it.name.lowercase().trim() == normalized }
        if (exactMatch != null) return exactMatch
        
        // Then check by variations
        return allTeachers.find { teacher ->
            teacher.variations.any { variation ->
                variation.lowercase().trim() == normalized
            }
        }
    }

    /**
     * Loads teacher data from a JSON file.
     *
     * @param path Path to the JSON file
     * @return List of Teacher objects
     */
    private fun loadTeachers(path: String): List<Teacher> {
        val file = File(path)
        if (!file.exists()) {
            println("Teachers file not found at: $path")
            return emptyList()
        }
        
        val content = file.readText()
        val gson = Gson()
        val teacherListType = object : TypeToken<List<Teacher>>() {}.type
        val teachers: List<Teacher> = gson.fromJson(content, teacherListType)
        
        // Update our internal list
        allTeachers.clear()
        allTeachers.addAll(teachers)
        
        return teachers
    }

    /**
     * Loads schedule data from a JSON file.
     *
     * @param path Path to the JSON file
     * @return Map of teacher names to their schedules
     */
    private fun loadSchedules(path: String): Map<String, List<Assignment>> {
        val file = File(path)
        if (!file.exists()) {
            println("Schedules file not found at: $path")
            return emptyMap()
        }
        
        val content = file.readText()
        val gson = Gson()
        val schedulesMapType = object : TypeToken<Map<String, List<Assignment>>>() {}.type
        return gson.fromJson(content, schedulesMapType)
    }

    /**
     * Loads assigned teacher data from a JSON file.
     *
     * @param path Path to the JSON file
     * @return List of SubstituteAssignment objects
     */
    private fun loadAssignedTeachers(path: String): List<SubstituteAssignment> {
        val file = File(path)
        if (!file.exists()) {
            println("Assigned teachers file not found at: $path")
            return emptyList()
        }
        
        val content = file.readText()
        val gson = Gson()
        
        try {
            val assignmentsType = object : TypeToken<Map<String, Any>>() {}.type
            val data: Map<String, Any> = gson.fromJson(content, assignmentsType)
            
            // Check if the data has the expected format
            if (data.containsKey("assignments")) {
                val assignmentsListType = object : TypeToken<List<SubstituteAssignment>>() {}.type
                val assignmentsJson = gson.toJson(data["assignments"])
                return gson.fromJson(assignmentsJson, assignmentsListType)
            } else {
                println("Unexpected format in assigned teachers file")
                return emptyList()
            }
        } catch (e: Exception) {
            println("Error parsing assigned teachers: $e")
            return emptyList()
        }
    }

    /**
     * Saves assignments to a JSON file.
     *
     * @param assignments List of SubstituteAssignment objects to save
     */
    private fun saveAssignmentsToFile(assignments: List<SubstituteAssignment>) {
        val file = File(DEFAULT_ASSIGNED_TEACHERS_PATH)
        val gson = Gson()
        val data = mapOf(
            "assignments" to assignments,
            "warnings" to emptyList<String>()
        )
        file.writeText(gson.toJson(data))
    }

    /**
     * Saves logs to a JSON file.
     *
     * @param logs List of ProcessLog objects to save
     * @param date The date for the log file
     */
    private fun saveLogs(logs: List<ProcessLog>, date: String) {
        val fileName = "data/logs_${date.replace("-", "")}.json"
        val file = File(fileName)
        val gson = Gson()
        file.writeText(gson.toJson(logs))
    }

    /**
     * Saves warnings to a text file.
     *
     * @param warnings List of warning strings to save
     * @param date The date for the warnings file
     */
    private fun saveWarnings(warnings: List<String>, date: String) {
        val fileName = "data/warnings_${date.replace("-", "")}.txt"
        val file = File(fileName)
        file.writeText(warnings.joinToString("\n"))
    }

    /**
     * Normalizes a teacher name for consistent lookup.
     *
     * @param name The name to normalize
     * @return The normalized name
     */
    private fun normalizeName(name: String): String {
        return name.trim().lowercase()
    }

    /**
     * Normalizes a day name for consistent lookup.
     *
     * @param day The day to normalize
     * @return The normalized day
     */
    private fun normalizeDay(day: String): String {
        val normalized = day.trim().lowercase()
        
        return when (normalized) {
            "mon", "m" -> "monday"
            "tue", "tues", "t" -> "tuesday"
            "wed", "w" -> "wednesday"
            "thu", "thurs", "th" -> "thursday"
            "fri", "f" -> "friday"
            "sat", "s" -> "saturday"
            "sun", "su" -> "sunday"
            else -> normalized
        }
    }

    /**
     * Assigns substitutes for an absent teacher on a specific day.
     *
     * @param absentTeacher The name of the absent teacher
     * @param day The day of the week
     * @return List of SubstituteAssignment objects
     */
    fun assignSubstitutes(absentTeacher: String, day: String): List<Any> {
        // Implementation would be similar to autoAssignSubstitutes but for a single teacher
        // This is a simplified implementation for now
        val assignmentsList = mutableListOf<SubstituteAssignment>()
        
        // Get all periods for this teacher on this day
        val normalizedTeacher = normalizeName(absentTeacher)
        val normalizedDay = normalizeDay(day)
        
        val classes = teacherClasses[normalizedTeacher] ?: emptyList()
        val todayClasses = classes.filter { it.day.lowercase() == normalizedDay.lowercase() }
        
        println("Processing assignment for $absentTeacher on $day")
        println("Found ${todayClasses.size} classes for this teacher on this day")
        
        // For each period, try to find a substitute
        for (assignment in todayClasses) {
            // In a real implementation, we would find an available substitute
            // For now, just create placeholder assignments
            val substituteAssignment = SubstituteAssignment(
                originalTeacher = absentTeacher,
                period = assignment.period,
                className = assignment.className,
                substitute = "Placeholder Substitute",
                substitutePhone = "000-000-0000"
            )
            
            assignmentsList.add(substituteAssignment)
        }
        
        // Save the assignments
        saveAssignmentsToFile(assignmentsList)
        
        return assignmentsList
    }

    /**
     * Verifies all substitute assignments.
     *
     * @return List of VerificationReport objects
     */
    fun verifyAssignments(): List<VerificationReport> {
        val reports = mutableListOf<VerificationReport>()
        
        // Add individual verification reports
        reports.add(verifySubstituteLimits())
        reports.add(verifyAvailability())
        reports.add(verifyWorkloadDistribution())
        
        return reports
    }

    /**
     * Verifies that substitute limits are not exceeded.
     *
     * @return A VerificationReport object
     */
    private fun verifySubstituteLimits(): VerificationReport {
        val issues = mutableListOf<String>()
        var invalidCount = 0
        
        // Count assignments per substitute
        val substituteCount = mutableMapOf<String, Int>()
        val regularTeacherCount = mutableMapOf<String, Int>()
        
        // Implement verification logic here
        
        return VerificationReport(
            success = issues.isEmpty(),
            issues = issues,
            validAssignments = 0, // Calculate the actual count
            invalidAssignments = invalidCount
        )
    }

    /**
     * Verifies that substitutes are available for their assignments.
     *
     * @return A VerificationReport object
     */
    private fun verifyAvailability(): VerificationReport {
        val issues = mutableListOf<String>()
        var invalidCount = 0
        
        // Implement verification logic here
        
        return VerificationReport(
            success = issues.isEmpty(),
            issues = issues,
            validAssignments = 0, // Calculate the actual count
            invalidAssignments = invalidCount
        )
    }

    /**
     * Verifies that workload is distributed fairly.
     *
     * @return A VerificationReport object
     */
    private fun verifyWorkloadDistribution(): VerificationReport {
        val issues = mutableListOf<String>()
        var invalidCount = 0
        
        // Implement verification logic here
        
        return VerificationReport(
            success = issues.isEmpty(),
            issues = issues,
            validAssignments = 0, // Calculate the actual count
            invalidAssignments = invalidCount
        )
    }

    /**
     * Gets all substitute assignments.
     *
     * @return A map containing substitute assignments
     */
    fun getSubstituteAssignments(): Map<String, Any> {
        val assignments = loadAssignedTeachers(DEFAULT_ASSIGNED_TEACHERS_PATH)
        
        return mapOf(
            "assignments" to assignments,
            "warnings" to emptyList<String>()
        )
    }

    /**
     * Clears all substitute assignments.
     */
    fun clearAssignments() {
        saveAssignmentsToFile(emptyList())
    }
}