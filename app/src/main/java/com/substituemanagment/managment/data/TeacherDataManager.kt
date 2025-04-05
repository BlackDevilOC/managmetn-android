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

data class SubstituteInfo(
    val name: String,
    val phone: String = "",
    val assignmentDate: String = "",
    val periods: List<TeacherAssignmentInfo> = emptyList()
)

data class TeacherAssignmentInfo(
    val period: Int,
    val className: String
)

data class AbsentTeacherData(
    val id: Int,
    val name: String,
    val timestamp: String,
    val assignedSubstitute: Boolean = false
)

data class AssignmentRecord(
    val absentTeacher: String,
    val substituteTeacher: String,
    val substitutePhone: String = "",
    val assignmentDate: String,
    val timestamp: String,
    val periods: List<TeacherAssignmentInfo> = emptyList()
)

class TeacherDataManager(private val context: Context) {
    private val TAG = "TeacherDataManager"
    private val gson = Gson()
    private val baseDir = File("/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data")
    private val processedDir = File(baseDir, "processed")
    private val absentTeachersFile = File(baseDir, "absent_teachers.json")
    private val assignedTeachersFile = File(baseDir, "assigned_teachers.json")
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

    fun getAbsentTeachersWithoutSubstitutes(): Set<AbsentTeacherData> {
        return getAbsentTeachers().filter { !it.assignedSubstitute }.toSet()
    }

    fun getAbsentTeachersWithSubstitutes(): Set<AbsentTeacherData> {
        return getAbsentTeachers().filter { it.assignedSubstitute }.toSet()
    }

    fun markTeacherAbsent(teacherName: String): Boolean {
        try {
            val absentTeachers = getAbsentTeachers().toMutableSet()
            
            // Check if teacher is already marked as absent to prevent duplicates
            if (absentTeachers.any { it.name == teacherName }) {
                Log.i(TAG, "Teacher $teacherName is already marked as absent")
                return false
            }
            
            val timestamp = java.time.Instant.now().toString()
            absentTeachers.add(AbsentTeacherData(
                id = nextId++,
                name = teacherName,
                timestamp = timestamp,
                assignedSubstitute = false
            ))
            saveAbsentTeachers(absentTeachers)
            Log.i(TAG, "Marked teacher as absent: $teacherName")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error marking teacher as absent", e)
            return false
        }
    }

    fun markTeacherPresent(teacherName: String): Boolean {
        try {
            val absentTeachers = getAbsentTeachers().toMutableSet()
            val wasRemoved = absentTeachers.removeIf { it.name == teacherName }
            
            if (wasRemoved) {
                saveAbsentTeachers(absentTeachers)
                Log.i(TAG, "Marked teacher as present: $teacherName")
                return true
            } else {
                Log.i(TAG, "Teacher $teacherName was not marked as absent")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error marking teacher as present", e)
            return false
        }
    }

    fun isTeacherAbsent(teacherName: String): Boolean {
        return getAbsentTeachers().any { it.name == teacherName }
    }

    fun updateTeacherWithSubstitute(
        teacherName: String, 
        substitute: SubstituteInfo
    ): Boolean {
        try {
            val absentTeachers = getAbsentTeachers().toMutableSet()
            val teacher = absentTeachers.find { it.name == teacherName }
            
            if (teacher != null) {
                // Mark the teacher as having a substitute assigned, but don't store assignment details here
                absentTeachers.remove(teacher)
                absentTeachers.add(AbsentTeacherData(
                    id = teacher.id,
                    name = teacher.name,
                    timestamp = teacher.timestamp,
                    assignedSubstitute = true
                ))
                saveAbsentTeachers(absentTeachers)
                
                // Store the full assignment record in the assigned_teachers.json file
                val assignmentRecord = AssignmentRecord(
                    absentTeacher = teacher.name,
                    substituteTeacher = substitute.name,
                    substitutePhone = substitute.phone,
                    assignmentDate = substitute.assignmentDate,
                    timestamp = java.time.Instant.now().toString(),
                    periods = substitute.periods
                )
                addAssignmentRecord(assignmentRecord)
                
                Log.i(TAG, "Updated teacher with substitute: ${teacher.name} -> ${substitute.name}")
                return true
            } else {
                Log.w(TAG, "Teacher $teacherName not found in absent teachers list")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating teacher with substitute", e)
            return false
        }
    }

    private fun addAssignmentRecord(record: AssignmentRecord) {
        try {
            val records = getAssignmentRecords().toMutableList()
            records.add(record)
            saveAssignmentRecords(records)
            Log.i(TAG, "Added assignment record: ${record.absentTeacher} -> ${record.substituteTeacher}")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding assignment record", e)
        }
    }

    fun getAssignmentRecords(): List<AssignmentRecord> {
        return try {
            if (!assignedTeachersFile.exists()) {
                return emptyList()
            }
            val type = object : TypeToken<List<AssignmentRecord>>() {}.type
            FileReader(assignedTeachersFile).use { reader ->
                gson.fromJson(reader, type) ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading assignment records", e)
            emptyList()
        }
    }

    private fun saveAssignmentRecords(records: List<AssignmentRecord>) {
        try {
            FileOutputStream(assignedTeachersFile).use { outputStream ->
                outputStream.write(gson.toJson(records).toByteArray())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving assignment records", e)
        }
    }

    fun setTeacherAssigned(teacherName: String, isAssigned: Boolean): Boolean {
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
                return true
            } else {
                Log.w(TAG, "Teacher $teacherName not found in absent teachers list")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating teacher assignment status", e)
            return false
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
                gson.fromJson(reader, type) ?: emptySet()
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