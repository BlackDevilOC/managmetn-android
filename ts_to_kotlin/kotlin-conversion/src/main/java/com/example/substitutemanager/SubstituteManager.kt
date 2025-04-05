package com.example.substitutemanager

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.example.substitutemanager.Constants.DEFAULT_TIMETABLE_PATH
import com.example.substitutemanager.Constants.DEFAULT_SUBSTITUTES_PATH
import com.example.substitutemanager.Constants.DEFAULT_TEACHERS_PATH
import com.example.substitutemanager.Constants.DEFAULT_SCHEDULES_PATH
import com.example.substitutemanager.Constants.DEFAULT_ASSIGNMENTS_PATH
import com.example.substitutemanager.Constants.PREFS_NAME
import com.example.substitutemanager.Constants.KEY_SUBSTITUTE_ASSIGNMENTS
import com.example.substitutemanager.Constants.TAG

/**
 * Main class for managing substitute teacher assignments
 * 
 * @property context The Android context used for accessing assets and SharedPreferences
 */
class SubstituteManager(private val context: Context) {
    // Data structures
    private val schedule: MutableMap<String, MutableMap<Int, List<String>>> = mutableMapOf()
    private val substitutes: MutableMap<String, String> = mutableMapOf()
    private val teacherClasses: MutableMap<String, List<Assignment>> = mutableMapOf()
    private val substituteAssignments: MutableMap<String, MutableList<Assignment>> = mutableMapOf()
    private val teacherWorkload: MutableMap<String, Int> = mutableMapOf()
    
    // Constants
    private val maxSubstituteAssignments = 3
    private val maxRegularTeacherAssignments = 2
    
    // Collections
    private val allAssignments: MutableList<Assignment> = mutableListOf()
    private val allTeachers: MutableList<Teacher> = mutableListOf()
    private val timetable: MutableList<Map<String, String>> = mutableListOf()
    
    /**
     * Load all data needed for the substitute manager
     * 
     * @param timetablePath Path to the timetable CSV file
     * @param substitutesPath Path to the substitutes CSV file
     */
    suspend fun loadData(
        timetablePath: String = DEFAULT_TIMETABLE_PATH,
        substitutesPath: String = DEFAULT_SUBSTITUTES_PATH
    ) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Loading data from $timetablePath and $substitutesPath")
                
                // Load timetable data
                context.assets.open(timetablePath).use { inputStream ->
                    val content = InputStreamReader(inputStream).readText()
                    val fixedContent = CsvParser.fixCsvContent(content)
                    val parsedTimetable = CsvParser.parseTimetable(fixedContent.byteInputStream())
                    timetable.clear()
                    timetable.addAll(parsedTimetable)
                    parseTimetable(fixedContent)
                }
                
                // Load substitutes data
                context.assets.open(substitutesPath).use { inputStream ->
                    val content = InputStreamReader(inputStream).readText()
                    val fixedContent = CsvParser.fixCsvContent(content)
                    val parsedSubstitutes = CsvParser.parseSubstitutes(fixedContent.byteInputStream())
                    substitutes.clear()
                    substitutes.putAll(parsedSubstitutes)
                    parseSubstitutes(fixedContent)
                }
                
                // Load teachers
                allTeachers.clear()
                allTeachers.addAll(loadTeachers(DEFAULT_TEACHERS_PATH))
                
                // Load teacher schedules
                teacherClasses.clear()
                teacherClasses.putAll(loadSchedules(DEFAULT_SCHEDULES_PATH))
                
                // Load previously assigned substitutes
                allAssignments.clear()
                substituteAssignments.clear()
                
                val assignedTeachers = loadAssignedTeachers(DEFAULT_ASSIGNMENTS_PATH)
                for (assignment in assignedTeachers) {
                    val normalizedSubstitute = normalizeName(assignment.substitute)
                    val normalizedTeacher = normalizeName(assignment.originalTeacher)
                    
                    // Add to allAssignments
                    allAssignments.add(
                        Assignment(
                            day = "",  // We don't store the day in this JSON format
                            period = assignment.period,
                            className = assignment.className,
                            originalTeacher = normalizedTeacher,
                            substitute = normalizedSubstitute
                        )
                    )
                    
                    // Update substituteAssignments map
                    if (!substituteAssignments.containsKey(normalizedSubstitute)) {
                        substituteAssignments[normalizedSubstitute] = mutableListOf()
                    }
                    
                    substituteAssignments[normalizedSubstitute]!!.add(
                        Assignment(
                            day = "",
                            period = assignment.period,
                            className = assignment.className,
                            originalTeacher = normalizedTeacher,
                            substitute = normalizedSubstitute
                        )
                    )
                }
                
                // Initialize workload tracking from existing assignments
                for ((substitute, assignments) in substituteAssignments) {
                    teacherWorkload[substitute] = assignments.size
                }
                
                Log.d(TAG, "Data loaded successfully. ${allTeachers.size} teachers, " +
                        "${timetable.size} timetable rows, ${substitutes.size} substitutes, " +
                        "${allAssignments.size} existing assignments")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading data", e)
                throw e
            }
        }
    }
    
    /**
     * Parse timetable CSV content
     * 
     * @param content The CSV content as a string
     */
    private fun parseTimetable(content: String) {
        try {
            val lines = content.lines().filter { it.isNotBlank() }
            if (lines.isEmpty()) return
            
            val headers = lines[0].split(",").map { it.trim() }
            
            for (i in 1 until lines.size) {
                val line = lines[i]
                val values = line.split(",").map { it.trim() }
                
                if (values.size < 2) continue
                
                val day = values[0].lowercase()
                val periodStr = values[1]
                val period = periodStr.toIntOrNull() ?: continue
                
                if (!schedule.containsKey(day)) {
                    schedule[day] = mutableMapOf()
                }
                
                val teachers = mutableListOf<String>()
                
                for (j in 2 until values.size) {
                    val teacher = values[j]
                    if (teacher.isNotBlank()) {
                        teachers.add(teacher.lowercase())
                    }
                }
                
                schedule[day]!![period] = teachers
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing timetable", e)
        }
    }
    
    /**
     * Parse substitutes CSV content
     * 
     * @param content The CSV content as a string
     */
    private fun parseSubstitutes(content: String) {
        try {
            val lines = content.lines().filter { it.isNotBlank() }
            
            for (line in lines) {
                val parts = line.split(",").map { it.trim() }
                if (parts.size >= 2) {
                    val name = parts[0].lowercase()
                    val phone = parts[1]
                    substitutes[name] = phone
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing substitutes", e)
        }
    }
