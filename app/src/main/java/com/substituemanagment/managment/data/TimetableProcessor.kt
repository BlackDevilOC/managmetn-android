package com.substituemanagment.managment.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.collections.ArrayList
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.toList
import kotlin.collections.toSet
import kotlin.text.Regex
import kotlin.text.RegexOption
import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs

data class ClassSchedule(
    val className: String,
    val schedules: List<Schedule>
)

data class DaySchedule(
    val day: String,
    val schedules: List<Schedule>
)

data class TeacherSchedule(
    val teacherId: Int,
    val teacherName: String,
    val schedules: List<Schedule>
)

data class PeriodSchedule(
    val period: Int,
    val schedules: List<Schedule>
)

data class Schedule(
    val id: Int,
    val day: String,
    val period: Int,
    val teacherId: Int,
    val className: String
)

data class Teacher(
    val name: String,
    val phone: String = "",
    val variations: List<String>
)

class TeacherNormalizer {
    private val SIMILARITY_THRESHOLD = 0.98  // Increased threshold for even stricter matching
    private val TEACHER_MAP: MutableMap<String, TeacherData> = mutableMapOf()

    data class TeacherData(
        var canonicalName: String,
        var phone: String = "",
        val variations: MutableSet<String> = mutableSetOf()
    )

    fun normalizeName(name: String): String {
        if (name.isBlank()) return ""
        
        return name.lowercase()
            .replace(Regex("(sir|miss|mr|ms|mrs|sr|dr)\\.?\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[^a-z\\s-]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun simplifiedMetaphone(str: String): String {
        if (str.isBlank()) return ""
        
        return str.lowercase()
            .replace(Regex("[aeiou]"), "a")
            .replace(Regex("[^a-z]"), "")
            .replace(Regex("(.)\\1+"), "$1")
            .take(8)
    }

    fun levenshtein(a: String, b: String): Int {
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val matrix = Array(b.length + 1) { IntArray(a.length + 1) }

        for (i in 0..a.length) matrix[0][i] = i
        for (j in 0..b.length) matrix[j][0] = j

        for (j in 1..b.length) {
            for (i in 1..a.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                matrix[j][i] = min(
                    min(matrix[j][i - 1] + 1, matrix[j - 1][i] + 1),
                    matrix[j - 1][i - 1] + cost
                )
            }
        }

        return matrix[b.length][a.length]
    }

    fun nameSimilarity(a: String, b: String): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0

        // Check for exact matches (case-insensitive)
        if (a.equals(b, ignoreCase = true)) {
            return 1.0
        }

        // Check for exact matches after normalization
        val aNorm = normalizeName(a)
        val bNorm = normalizeName(b)
        if (aNorm == bNorm) {
            return 0.99
        }

        val aMeta = simplifiedMetaphone(a)
        val bMeta = simplifiedMetaphone(b)
        val distance = levenshtein(aMeta, bMeta)
        var similarity = 1.0 - distance.toDouble() / max(aMeta.length, bMeta.length)

        // Check for substring matches
        if (a.contains(b, ignoreCase = true) || b.contains(a, ignoreCase = true)) {
            similarity = max(similarity, 0.95)
        }

        // Check for common name parts
        val aParts = a.split(" ").toSet()
        val bParts = b.split(" ").toSet()
        val commonParts = aParts.intersect(bParts)
        if (commonParts.isNotEmpty()) {
            similarity = max(similarity, 0.85 + (0.05 * commonParts.size))
        }

        return similarity
    }

    fun generateVariations(name: String): List<String> {
        val variations = mutableSetOf<String>()
        
        // Add original name
        variations.add(name)
        
        // Add lowercase version
        variations.add(name.lowercase())
        
        // Add version without honorific
        val withoutHonorific = name.replace(Regex("^(sir|miss|mr|ms|mrs|sr|dr)\\s+", RegexOption.IGNORE_CASE), "")
            .trim()
        variations.add(withoutHonorific)
        variations.add(withoutHonorific.lowercase())
        
        // Add version with different honorific
        when {
            name.startsWith("Sir", ignoreCase = true) -> {
                variations.add(name.replace(Regex("^Sir", RegexOption.IGNORE_CASE), "Miss"))
            }
            name.startsWith("Miss", ignoreCase = true) -> {
                variations.add(name.replace(Regex("^Miss", RegexOption.IGNORE_CASE), "Sir"))
            }
        }

        return variations.toList()
    }

    fun registerTeacher(rawName: String, phone: String = ""): Boolean {
        if (rawName.isBlank() || rawName.equals("empty", ignoreCase = true) || rawName.trim().length < 2) {
            return false
        }

        val normalized = normalizeName(rawName)
        if (normalized.isEmpty()) return false

        // Check exact matches first
        if (TEACHER_MAP.containsKey(normalized)) {
            val existing = TEACHER_MAP[normalized]!!
            existing.variations.add(rawName)
            if (phone.isNotEmpty() && existing.phone.isEmpty()) {
                existing.phone = phone
            }
            return true
        }

        // Check for similar names with stricter matching
        for ((_, teacher) in TEACHER_MAP) {
            val teacherNormalized = normalizeName(teacher.canonicalName)
            val similarity = nameSimilarity(normalized, teacherNormalized)
            
            // Only merge if very similar and not already registered
            if (similarity >= SIMILARITY_THRESHOLD && !teacher.variations.contains(rawName)) {
                // Additional check to prevent merging different teachers
                if (isSameTeacher(normalized, teacherNormalized)) {
                    teacher.variations.add(rawName)
                    if (phone.isNotEmpty() && teacher.phone.isEmpty()) {
                        teacher.phone = phone
                    }
                    return true
                }
            }
        }

        // Add new teacher
        TEACHER_MAP[normalized] = TeacherData(
            canonicalName = rawName.trim().replace(Regex("\\s+"), " "),
            phone = phone,
            variations = mutableSetOf(rawName)
        )

        return true
    }

    private fun isSameTeacher(name1: String, name2: String): Boolean {
        // Check if the names are too different to be the same teacher
        val parts1 = name1.split(" ").toSet()
        val parts2 = name2.split(" ").toSet()
        
        // If one name is significantly longer than the other, they're probably different teachers
        if (abs(parts1.size - parts2.size) > 2) return false
        
        // If they share no common parts, they're different teachers
        if (parts1.intersect(parts2).isEmpty()) return false
        
        // If they have completely different first names, they're different teachers
        if (parts1.first() != parts2.first()) return false
        
        return true
    }

    fun getTeachers(): List<TeacherData> = TEACHER_MAP.values.toList()
}

class TimetableProcessor(private val context: Context) {
    private val TAG = "TimetableProcessor"
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val baseDir = File("/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data")
    private val rawDir = File(baseDir, "raw")
    private val processedDir = File(baseDir, "processed")
    private val teacherNormalizer = TeacherNormalizer()

    init {
        // Create necessary directories if they don't exist
        processedDir.mkdirs()
        rawDir.mkdirs()
        Log.d(TAG, "Base Directory: ${baseDir.absolutePath}")
        Log.d(TAG, "Raw Directory: ${rawDir.absolutePath}")
        Log.d(TAG, "Processed Directory: ${processedDir.absolutePath}")
    }

    fun processTimetable(): Boolean {
        try {
            Log.i(TAG, "Starting timetable processing...")
            
            // Read timetable file
            val timetableFile = File(rawDir, "timetable.csv")
            Log.d(TAG, "Looking for timetable file at: ${timetableFile.absolutePath}")
            Log.d(TAG, "File exists: ${timetableFile.exists()}")
            
            if (!timetableFile.exists()) {
                throw IOException("Timetable file not found at ${timetableFile.absolutePath}")
            }

            val records = timetableFile.readLines().map { it.split(",").map { cell -> cell.trim() } }
            Log.i(TAG, "Found ${records.size} rows in timetable")
            
            if (records.size < 2) {
                throw IOException("Invalid timetable file format")
            }

            // Process header
            val header = records[0]
            if (header.size < 3) {
                throw IOException("Invalid header format. Expected at least 3 columns (day, period, and at least one class)")
            }
            val validClasses = header.subList(2, header.size) // Skip day and period columns
            Log.i(TAG, "Processing ${validClasses.size} classes")

            // Initialize data structures
            val classSchedules = mutableMapOf<String, MutableList<Schedule>>()
            val daySchedules = mutableMapOf<String, MutableList<Schedule>>()
            val teacherSchedules = mutableMapOf<Int, MutableList<Schedule>>()
            val periodSchedules = mutableMapOf<Int, MutableList<Schedule>>()
            var scheduleId = 1

            // Process each row
            for (i in 1 until records.size) {
                val row = records[i]
                if (row.size < 3) {
                    Log.w(TAG, "Skipping row $i: insufficient data (${row.size} columns)")
                    continue
                }

                val day = normalizeDay(row[0])
                val periodStr = row[1]
                val period = periodStr.toIntOrNull()
                if (period == null) {
                    Log.w(TAG, "Skipping row $i: invalid period '$periodStr'")
                    continue
                }

                // Process teacher assignments
                for (j in 2 until row.size) {
                    val teacherName = row[j].trim()
                    if (teacherName.isNotEmpty() && teacherName.lowercase() != "empty") {
                        // Register teacher with normalizer
                        teacherNormalizer.registerTeacher(teacherName)
                        
                        val className = validClasses[j - 2]
                        val teacherId = generateTeacherId(teacherName)

                        val schedule = Schedule(
                            id = scheduleId++,
                            day = day,
                            period = period,
                            teacherId = teacherId,
                            className = className
                        )

                        // Add to class schedules
                        classSchedules.getOrPut(className) { mutableListOf() }.add(schedule)

                        // Add to day schedules
                        daySchedules.getOrPut(day) { mutableListOf() }.add(schedule)

                        // Add to teacher schedules
                        teacherSchedules.getOrPut(teacherId) { mutableListOf() }.add(schedule)

                        // Add to period schedules
                        periodSchedules.getOrPut(period) { mutableListOf() }.add(schedule)
                    }
                }
            }

            Log.i(TAG, "Generated ${scheduleId - 1} schedules")
            Log.i(TAG, "Found ${teacherNormalizer.getTeachers().size} unique teachers")

            // Convert to final data structures
            val finalClassSchedules = classSchedules.map { (className, schedules) ->
                ClassSchedule(className, schedules)
            }

            val finalDaySchedules = daySchedules.map { (day, schedules) ->
                DaySchedule(day, schedules)
            }

            val finalTeacherSchedules = teacherSchedules.map { (teacherId, schedules) ->
                TeacherSchedule(teacherId, getTeacherName(teacherId), schedules)
            }

            val finalPeriodSchedules = periodSchedules.map { (period, schedules) ->
                PeriodSchedule(period, schedules)
            }

            // Save processed data
            saveProcessedData(
                finalClassSchedules,
                finalDaySchedules,
                finalTeacherSchedules,
                finalPeriodSchedules
            )

            // Generate and save total_teacher.json
            generateTotalTeacherJson()

            Log.i(TAG, "Timetable processing completed successfully")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error processing timetable", e)
            e.printStackTrace()
            return false
        }
    }

    private fun generateTotalTeacherJson() {
        val teachers = teacherNormalizer.getTeachers().map { teacher ->
            Teacher(
                name = teacher.canonicalName,
                phone = teacher.phone,
                variations = teacher.variations.toList()
            )
        }

        val file = File(processedDir, "total_teacher.json")
        FileOutputStream(file).use { outputStream ->
            outputStream.write(gson.toJson(teachers).toByteArray())
        }
        Log.i(TAG, "Saved total_teacher.json with ${teachers.size} teachers to: ${file.absolutePath}")
    }

    private fun saveProcessedData(
        classSchedules: List<ClassSchedule>,
        daySchedules: List<DaySchedule>,
        teacherSchedules: List<TeacherSchedule>,
        periodSchedules: List<PeriodSchedule>
    ) {
        Log.i(TAG, "Saving processed data...")
        
        // Save class schedules
        saveJsonFile("class_schedules.json", classSchedules)
        Log.i(TAG, "Saved ${classSchedules.size} class schedules")

        // Save day schedules
        saveJsonFile("day_schedules.json", daySchedules)
        Log.i(TAG, "Saved ${daySchedules.size} day schedules")

        // Save teacher schedules
        saveJsonFile("teacher_schedules.json", teacherSchedules)
        Log.i(TAG, "Saved ${teacherSchedules.size} teacher schedules")

        // Save period schedules
        saveJsonFile("period_schedules.json", periodSchedules)
        Log.i(TAG, "Saved ${periodSchedules.size} period schedules")
    }

    private fun saveJsonFile(filename: String, data: Any) {
        val file = File(processedDir, filename)
        FileOutputStream(file).use { outputStream ->
            outputStream.write(gson.toJson(data).toByteArray())
        }
        Log.d(TAG, "Saved processed data to: ${file.absolutePath}")
    }

    private fun normalizeDay(day: String): String {
        return day.lowercase().trim()
    }

    private fun generateTeacherId(teacherName: String): Int {
        // Use the normalized name for consistent ID generation
        return teacherNormalizer.normalizeName(teacherName).hashCode()
    }

    private fun getTeacherName(teacherId: Int): String {
        // Find the teacher by ID in the normalizer's map
        return teacherNormalizer.getTeachers()
            .find { it.canonicalName.hashCode() == teacherId }
            ?.canonicalName
            ?: "Unknown Teacher"
    }
} 