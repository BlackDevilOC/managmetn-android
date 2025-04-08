package com.substituemanagment.managment.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter

object SubstituteManager {
    private const val TAG = "SubstituteManager"
    private const val BASE_PATH = "/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data"
    private val gson = Gson()
    
    data class AssignedSubstitute(
        val absentTeacher: String,
        val substituteTeacher: String,
        val date: String,
        val period: String
    )
    
    data class AbsentTeacher(
        val name: String,
        val date: String,
        val reason: String
    )
    
    fun loadAssignedSubstitutes(): List<AssignedSubstitute> {
        return try {
            val file = File("$BASE_PATH/assigned_substitute.json")
            if (!file.exists()) return emptyList()
            
            gson.fromJson(
                FileReader(file),
                object : TypeToken<List<AssignedSubstitute>>() {}.type
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading assigned substitutes", e)
            emptyList()
        }
    }
    
    fun saveAssignedSubstitutes(substitutes: List<AssignedSubstitute>) {
        try {
            val file = File("$BASE_PATH/assigned_substitute.json")
            FileWriter(file).use { writer ->
                gson.toJson(substitutes, writer)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving assigned substitutes", e)
        }
    }
    
    fun loadAbsentTeachers(): List<AbsentTeacher> {
        return try {
            val file = File("$BASE_PATH/absent_teachers.json")
            if (!file.exists()) return emptyList()
            
            gson.fromJson(
                FileReader(file),
                object : TypeToken<List<AbsentTeacher>>() {}.type
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading absent teachers", e)
            emptyList()
        }
    }
    
    fun saveAbsentTeachers(teachers: List<AbsentTeacher>) {
        try {
            val file = File("$BASE_PATH/absent_teachers.json")
            FileWriter(file).use { writer ->
                gson.toJson(teachers, writer)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving absent teachers", e)
        }
    }
    
    suspend fun handleTeacherReturn(
        context: Context,
        teacherName: String,
        onSendCancellationSms: (String, String) -> Unit
    ) {
        try {
            // Get the assigned substitute for this teacher
            val assignedSubstitutes = loadAssignedSubstitutes()
            val teacherAssignments = assignedSubstitutes.filter { it.absentTeacher == teacherName }
            
            if (teacherAssignments.isNotEmpty()) {
                // Remove from assigned substitutes
                val updatedAssignments = assignedSubstitutes.filter { it.absentTeacher != teacherName }
                saveAssignedSubstitutes(updatedAssignments)
                
                // Remove from absent teachers list
                val absentTeachers = loadAbsentTeachers()
                val updatedAbsentTeachers = absentTeachers.filter { it.name != teacherName }
                saveAbsentTeachers(updatedAbsentTeachers)
                
                // For each substitute that was assigned to this teacher
                teacherAssignments.forEach { assignment ->
                    // Get substitute's contact info
                    val substituteContact = getTeacherContact(assignment.substituteTeacher)
                    if (substituteContact != null) {
                        // Create cancellation message
                        val message = "Your substitute duty for ${assignment.absentTeacher} " +
                            "on ${assignment.date} has been cancelled as the teacher has returned."
                        
                        // Trigger SMS sending
                        onSendCancellationSms(substituteContact.phone, message)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling teacher return: ${e.message}", e)
            throw e
        }
    }
    
    private fun getTeacherContact(teacherName: String): TeacherContactDto? {
        return try {
            val contactsFile = File("$BASE_PATH/sms/teacher_contacts.json")
            if (!contactsFile.exists()) return null
            
            val contacts: List<TeacherContactDto> = gson.fromJson(
                FileReader(contactsFile),
                object : TypeToken<List<TeacherContactDto>>() {}.type
            )
            
            contacts.find { it.name == teacherName }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting teacher contact: ${e.message}", e)
            null
        }
    }
} 