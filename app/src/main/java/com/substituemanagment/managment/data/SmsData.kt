package com.substituemanagment.managment.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.util.UUID

// Data models for SMS functionality

data class SmsTemplateDto(
    val id: String,
    val name: String,
    val content: String
)

data class TeacherContactDto(
    val id: String,
    val name: String,
    val phone: String
)

data class SmsHistoryDto(
    val id: String,
    val recipients: List<String>,
    val message: String,
    val timestamp: Long,
    val status: String
)

// Path constants
private const val TAG = "SmsDataManager"
private const val EXTERNAL_STORAGE_BASE_PATH = "/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data"
private const val SMS_DIR = "$EXTERNAL_STORAGE_BASE_PATH/sms"

// Helper class to manage SMS data
object SmsDataManager {
    private const val SMS_TEMPLATES_FILE = "sms_templates.json"
    private const val TEACHER_CONTACTS_FILE = "teacher_contacts.json"
    private const val SMS_HISTORY_FILE = "sms_history.json"
    private const val SAMPLE_TEACHERS_ASSET = "sample_teachers.json"
    
    private val gson = Gson()
    
    // Initialize method to ensure directories exist
    fun initialize(context: Context) {
        try {
            // Ensure base directory exists
            val baseDir = File(EXTERNAL_STORAGE_BASE_PATH)
            if (!baseDir.exists()) {
                val baseCreated = baseDir.mkdirs()
                Log.d(TAG, "Created base directory: $baseCreated - ${baseDir.absolutePath}")
            }
            
            // Ensure SMS directory exists
            val smsDir = File(SMS_DIR)
            if (!smsDir.exists()) {
                val created = smsDir.mkdirs()
                Log.d(TAG, "Created SMS directory: $created - ${smsDir.absolutePath}")
            }
            
            // Initialize files with empty arrays if they don't exist
            val historyFile = File(smsDir, SMS_HISTORY_FILE)
            if (!historyFile.exists()) {
                historyFile.createNewFile()
                historyFile.writeText("[]")
            }
            
            val templatesFile = File(smsDir, SMS_TEMPLATES_FILE)
            if (!templatesFile.exists()) {
                templatesFile.createNewFile()
                templatesFile.writeText("[]")
            }
            
            val contactsFile = File(smsDir, TEACHER_CONTACTS_FILE)
            if (!contactsFile.exists()) {
                contactsFile.createNewFile()
                contactsFile.writeText("[]")
            }
            
            // Log storage info for debugging
            Log.d(TAG, "Storage Info: ${getStorageInfo()}")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SMS directory", e)
            e.printStackTrace()
        }
    }
    
    // SMS Templates
    fun saveTemplates(context: Context, templates: List<SmsTemplateDto>) {
        val jsonString = gson.toJson(templates)
        saveToFile(context, SMS_TEMPLATES_FILE, jsonString)
    }
    
    fun loadTemplates(context: Context): List<SmsTemplateDto> {
        initialize(context)
        val jsonString = loadFromFile(context, SMS_TEMPLATES_FILE) ?: return getDefaultTemplates()
        return try {
            val type = object : TypeToken<List<SmsTemplateDto>>() {}.type
            gson.fromJson(jsonString, type) ?: getDefaultTemplates()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading templates", e)
            getDefaultTemplates()
        }
    }
    
    // SMS History
    fun saveSmsHistory(context: Context, history: List<SmsHistoryDto>) {
        val jsonString = gson.toJson(history)
        saveToFile(context, SMS_HISTORY_FILE, jsonString)
    }
    
    fun loadSmsHistory(context: Context): List<SmsHistoryDto> {
        initialize(context)
        val jsonString = loadFromFile(context, SMS_HISTORY_FILE) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<SmsHistoryDto>>() {}.type
            gson.fromJson(jsonString, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading SMS history", e)
            emptyList()
        }
    }
    
    fun addSmsToHistory(context: Context, sms: SmsHistoryDto) {
        try {
            val currentHistory = loadSmsHistory(context).toMutableList()
            currentHistory.add(0, sms) // Add to the beginning for most recent first
            saveSmsHistory(context, currentHistory)
            Log.d(TAG, "Successfully added SMS to history: ${sms.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding SMS to history", e)
            e.printStackTrace()
        }
    }
    
    // File utilities
    private fun saveToFile(context: Context, filename: String, content: String) {
        try {
            val file = File(SMS_DIR, filename)
            FileOutputStream(file).use { 
                it.write(content.toByteArray())
            }
            Log.d(TAG, "Successfully saved to $filename")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to $filename", e)
            e.printStackTrace()
        }
    }
    
    private fun loadFromFile(context: Context, filename: String): String? {
        return try {
            val file = File(SMS_DIR, filename)
            if (file.exists()) {
                file.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading from $filename", e)
            e.printStackTrace()
            null
        }
    }
    
    private fun getStorageInfo(): String {
        val smsDir = File(SMS_DIR)
        val templatesFile = File(smsDir, SMS_TEMPLATES_FILE)
        val contactsFile = File(smsDir, TEACHER_CONTACTS_FILE)
        val historyFile = File(smsDir, SMS_HISTORY_FILE)
        
        return """
            SMS Directory: $SMS_DIR
            SMS Directory exists: ${smsDir.exists()}
            SMS Templates: ${templatesFile.absolutePath} (exists: ${templatesFile.exists()})
            Teacher Contacts: ${contactsFile.absolutePath} (exists: ${contactsFile.exists()})
            SMS History: ${historyFile.absolutePath} (exists: ${historyFile.exists()})
        """.trimIndent()
    }
    
    private fun getDefaultTemplates(): List<SmsTemplateDto> {
        return listOf(
            SmsTemplateDto(
                id = UUID.randomUUID().toString(),
                name = "Default Template",
                content = "Dear {teacher}, you have been assigned to cover {class} Period {period} on {date}. Please confirm your availability."
            )
        )
    }
} 