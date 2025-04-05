package com.substituemanagment.managment.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader

data class TeacherData(
    val name: String,
    val phone: String = "",
    val variations: List<String> = emptyList()
)

data class AbsentTeacherData(
    val id: Int,
    val name: String,
    val timestamp: String,
    val assignedSubstitute: Boolean = false
)

class TeacherDataManager(private val context: Context) {
    private val TAG = "TeacherDataManager"
    private val gson = Gson()
    private val baseDir = File("/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data")
    private val processedDir = File(baseDir, "processed")
    private val absentTeachersFile = File(baseDir, "absent_teachers.json")
    private val totalTeachersFile = File(processedDir, "total_teacher.json")
    private var nextId = 1

    init {
        // Create necessary directories if they don't exist
        baseDir.mkdirs()
        processedDir.mkdirs()
        Log.d(TAG, "Base Directory: ${baseDir.absolutePath}")
        Log.d(TAG, "Processed Directory: ${processedDir.absolutePath}")
        
        // Initialize nextId based on existing data
        nextId = getAbsentTeachers().maxOfOrNull { it.id }?.plus(1) ?: 1
    }

    fun getAllTeachers(): List<TeacherData> {
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
            emptyList()
        }
    }

    fun markTeacherAbsent(teacherName: String) {
        try {
            val absentTeachers = getAbsentTeachers().toMutableSet()
            val timestamp = java.time.Instant.now().toString()
            absentTeachers.add(AbsentTeacherData(
                id = nextId++,
                name = teacherName,
                timestamp = timestamp,
                assignedSubstitute = false
            ))
            saveAbsentTeachers(absentTeachers)
            Log.i(TAG, "Marked teacher as absent: $teacherName")
        } catch (e: Exception) {
            Log.e(TAG, "Error marking teacher as absent", e)
        }
    }

    fun markTeacherPresent(teacherName: String) {
        try {
            val absentTeachers = getAbsentTeachers().toMutableSet()
            absentTeachers.removeIf { it.name == teacherName }
            saveAbsentTeachers(absentTeachers)
            Log.i(TAG, "Marked teacher as present: $teacherName")
        } catch (e: Exception) {
            Log.e(TAG, "Error marking teacher as present", e)
        }
    }

    fun isTeacherAbsent(teacherName: String): Boolean {
        return getAbsentTeachers().any { it.name == teacherName }
    }

    fun setTeacherAssigned(teacherName: String, isAssigned: Boolean) {
        try {
            val absentTeachers = getAbsentTeachers().toMutableSet()
            val teacher = absentTeachers.find { it.name == teacherName }
            if (teacher != null) {
                absentTeachers.remove(teacher)
                absentTeachers.add(AbsentTeacherData(
                    id = teacher.id,
                    name = teacher.name,
                    timestamp = teacher.timestamp,
                    assignedSubstitute = isAssigned
                ))
                saveAbsentTeachers(absentTeachers)
                Log.i(TAG, "Updated teacher assignment status: $teacherName = $isAssigned")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating teacher assignment status", e)
        }
    }

    fun isTeacherAssigned(teacherName: String): Boolean {
        return getAbsentTeachers().find { it.name == teacherName }?.assignedSubstitute ?: false
    }

    fun getAbsentTeachers(): Set<AbsentTeacherData> {
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
            emptySet()
        }
    }

    private fun saveAbsentTeachers(teachers: Set<AbsentTeacherData>) {
        try {
            FileOutputStream(absentTeachersFile).use { outputStream ->
                outputStream.write(gson.toJson(teachers).toByteArray())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving absent teachers", e)
        }
    }
} 