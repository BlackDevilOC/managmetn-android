package com.substituemanagment.managment.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader

class TeacherDataManager(private val context: Context) {
    private val TAG = "TeacherDataManager"
    private val gson = Gson()
    private val baseDir = File("/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data")
    private val absentTeachersFile = File(baseDir, "absent_teachers.json")

    init {
        // Create necessary directories if they don't exist
        baseDir.mkdirs()
        Log.d(TAG, "Base Directory: ${baseDir.absolutePath}")
    }

    fun markTeacherAbsent(teacherName: String) {
        try {
            val absentTeachers = getAbsentTeachers().toMutableSet()
            absentTeachers.add(teacherName)
            saveAbsentTeachers(absentTeachers)
            Log.i(TAG, "Marked teacher as absent: $teacherName")
        } catch (e: Exception) {
            Log.e(TAG, "Error marking teacher as absent", e)
        }
    }

    fun markTeacherPresent(teacherName: String) {
        try {
            val absentTeachers = getAbsentTeachers().toMutableSet()
            absentTeachers.remove(teacherName)
            saveAbsentTeachers(absentTeachers)
            Log.i(TAG, "Marked teacher as present: $teacherName")
        } catch (e: Exception) {
            Log.e(TAG, "Error marking teacher as present", e)
        }
    }

    fun isTeacherAbsent(teacherName: String): Boolean {
        return getAbsentTeachers().contains(teacherName)
    }

    private fun getAbsentTeachers(): Set<String> {
        return try {
            if (!absentTeachersFile.exists()) {
                return emptySet()
            }
            val type = object : TypeToken<Set<String>>() {}.type
            FileReader(absentTeachersFile).use { reader ->
                gson.fromJson(reader, type)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading absent teachers", e)
            emptySet()
        }
    }

    private fun saveAbsentTeachers(teachers: Set<String>) {
        try {
            FileOutputStream(absentTeachersFile).use { outputStream ->
                outputStream.write(gson.toJson(teachers).toByteArray())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving absent teachers", e)
        }
    }
} 