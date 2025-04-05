package com.substituemanagment.managment.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

// Data models for SMS functionality

@Serializable
data class SmsTemplateDto(
    val id: String,
    val name: String,
    val content: String
)

@Serializable
data class TeacherContactDto(
    val id: String,
    val name: String,
    val phone: String
)

@Serializable
data class SmsHistoryDto(
    val id: String,
    val recipients: List<String>,
    val message: String,
    val timestamp: Long,
    val status: String
)

// Helper class to manage SMS data
object SmsDataManager {
    private const val SMS_TEMPLATES_FILE = "sms_templates.json"
    private const val TEACHER_CONTACTS_FILE = "teacher_contacts.json"
    private const val SMS_HISTORY_FILE = "sms_history.json"
    private const val SAMPLE_TEACHERS_ASSET = "sample_teachers.json"
    
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    
    // SMS Templates
    fun saveTemplates(context: Context, templates: List<SmsTemplateDto>) {
        val jsonString = json.encodeToString(templates)
        saveToFile(context, SMS_TEMPLATES_FILE, jsonString)
    }
    
    fun loadTemplates(context: Context): List<SmsTemplateDto> {
        val jsonString = loadFromFile(context, SMS_TEMPLATES_FILE) ?: return getDefaultTemplates()
        return try {
            json.decodeFromString<List<SmsTemplateDto>>(jsonString)
        } catch (e: Exception) {
            getDefaultTemplates()
        }
    }
    
    private fun getDefaultTemplates(): List<SmsTemplateDto> {
        return listOf(
            SmsTemplateDto(
                "1", 
                "Substitution Notice", 
                "Dear teacher, you have been assigned a substitution on {date} for period {period}. Please check the app for details."
            ),
            SmsTemplateDto(
                "2", 
                "Schedule Change", 
                "Dear teacher, there has been a change in your schedule for {date}. Please check the app for details."
            ),
            SmsTemplateDto(
                "3", 
                "Meeting Reminder", 
                "Dear teacher, this is a reminder for the staff meeting on {date} at {time}."
            )
        )
    }
    
    // Teacher Contacts
    fun saveTeacherContacts(context: Context, contacts: List<TeacherContactDto>) {
        val jsonString = json.encodeToString(contacts)
        saveToFile(context, TEACHER_CONTACTS_FILE, jsonString)
    }
    
    fun loadTeacherContacts(context: Context): List<TeacherContactDto> {
        val jsonString = loadFromFile(context, TEACHER_CONTACTS_FILE)
        
        // If no teacher data exists, try to load sample data from assets
        if (jsonString == null) {
            return loadSampleTeachers(context)
        }
        
        return try {
            json.decodeFromString<List<TeacherContactDto>>(jsonString)
        } catch (e: Exception) {
            loadSampleTeachers(context)
        }
    }
    
    private fun loadSampleTeachers(context: Context): List<TeacherContactDto> {
        try {
            val inputStream = context.assets.open(SAMPLE_TEACHERS_ASSET)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val jsonString = String(buffer, Charsets.UTF_8)
            
            val sampleTeachers = json.decodeFromString<List<TeacherContactDto>>(jsonString)
            
            // Save the sample data to local storage for future use
            saveTeacherContacts(context, sampleTeachers)
            
            return sampleTeachers
        } catch (e: IOException) {
            e.printStackTrace()
            return emptyList()
        }
    }
    
    // SMS History
    fun saveSmsHistory(context: Context, history: List<SmsHistoryDto>) {
        val jsonString = json.encodeToString(history)
        saveToFile(context, SMS_HISTORY_FILE, jsonString)
    }
    
    fun loadSmsHistory(context: Context): List<SmsHistoryDto> {
        val jsonString = loadFromFile(context, SMS_HISTORY_FILE) ?: return emptyList()
        return try {
            json.decodeFromString<List<SmsHistoryDto>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun addSmsToHistory(context: Context, sms: SmsHistoryDto) {
        val currentHistory = loadSmsHistory(context).toMutableList()
        currentHistory.add(0, sms) // Add to the beginning for most recent first
        saveSmsHistory(context, currentHistory)
    }
    
    // File utilities
    private fun saveToFile(context: Context, fileName: String, content: String) {
        try {
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { stream ->
                stream.write(content.toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun loadFromFile(context: Context, fileName: String): String? {
        return try {
            val file = File(context.filesDir, fileName)
            if (!file.exists()) return null
            
            context.openFileInput(fileName).bufferedReader().use { reader ->
                reader.readText()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
} 