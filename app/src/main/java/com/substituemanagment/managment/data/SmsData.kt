package com.substituemanagment.managment.data

import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
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
    
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    
    // Initialize method to ensure directories exist
    fun initialize(context: Context) {
        try {
            val smsDir = File(SMS_DIR)
            if (!smsDir.exists()) {
                val created = smsDir.mkdirs()
                Log.d(TAG, "Created SMS directory: $created - ${smsDir.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SMS directory", e)
            e.printStackTrace()
        }
    }
    
    // SMS Templates
    fun saveTemplates(context: Context, templates: List<SmsTemplateDto>) {
        val jsonString = json.encodeToString(templates)
        saveToFile(context, SMS_TEMPLATES_FILE, jsonString)
    }
    
    fun loadTemplates(context: Context): List<SmsTemplateDto> {
        initialize(context)
        val jsonString = loadFromFile(context, SMS_TEMPLATES_FILE) ?: return getDefaultTemplates()
        return try {
            json.decodeFromString<List<SmsTemplateDto>>(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading templates", e)
            getDefaultTemplates()
        }
    }
    
    private fun getDefaultTemplates(): List<SmsTemplateDto> {
        return listOf(
            SmsTemplateDto(
                "1", 
                "Standard Substitution", 
                "[SCHOOL] You have been assigned as substitute for Class {class} on {date} ({day}), Period {period}, Room {room}. Please confirm."
            ),
            SmsTemplateDto(
                "2", 
                "Urgent Substitution", 
                "[URGENT] Immediate substitution required today ({day}) for Class {class}, Period {period}, Room {room}. Please reply with Y/N as soon as possible."
            ),
            SmsTemplateDto(
                "3", 
                "Detailed Substitution", 
                "[SCHOOL] Substitution Assignment: Class {class}, Teacher: {teacher}, Date: {date} ({day}), Period: {period}, Room: {room}. Please arrive 10 minutes early."
            )
        )
    }
    
    // Teacher Contacts
    fun saveTeacherContacts(context: Context, contacts: List<TeacherContactDto>) {
        val jsonString = json.encodeToString(contacts)
        saveToFile(context, TEACHER_CONTACTS_FILE, jsonString)
    }
    
    fun loadTeacherContacts(context: Context): List<TeacherContactDto> {
        initialize(context)
        val jsonString = loadFromFile(context, TEACHER_CONTACTS_FILE)
        
        // If no teacher data exists, try to load sample data from assets
        if (jsonString == null) {
            return loadSampleTeachers(context)
        }
        
        return try {
            json.decodeFromString<List<TeacherContactDto>>(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading teacher contacts", e)
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
            Log.e(TAG, "Error loading sample teachers", e)
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
        initialize(context)
        val jsonString = loadFromFile(context, SMS_HISTORY_FILE) ?: return emptyList()
        return try {
            json.decodeFromString<List<SmsHistoryDto>>(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading SMS history", e)
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
            val file = File(SMS_DIR, fileName)
            FileOutputStream(file).use { stream ->
                stream.write(content.toByteArray())
            }
            Log.d(TAG, "File saved: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving file: $fileName", e)
            e.printStackTrace()
        }
    }
    
    private fun loadFromFile(context: Context, fileName: String): String? {
        return try {
            val file = File(SMS_DIR, fileName)
            if (!file.exists()) {
                Log.d(TAG, "File does not exist: ${file.absolutePath}")
                return null
            }
            
            FileReader(file).use { reader ->
                reader.readText()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading file: $fileName", e)
            e.printStackTrace()
            null
        }
    }
    
    // This function can be used for data migration
    fun getStorageInfo(): String {
        val smsDir = File(SMS_DIR)
        return """
            SMS Directory: ${smsDir.absolutePath}
            SMS Directory exists: ${smsDir.exists()}
            SMS Templates: ${File(smsDir, SMS_TEMPLATES_FILE).absolutePath} (exists: ${File(smsDir, SMS_TEMPLATES_FILE).exists()})
            Teacher Contacts: ${File(smsDir, TEACHER_CONTACTS_FILE).absolutePath} (exists: ${File(smsDir, TEACHER_CONTACTS_FILE).exists()})
            SMS History: ${File(smsDir, SMS_HISTORY_FILE).absolutePath} (exists: ${File(smsDir, SMS_HISTORY_FILE).exists()})
        """.trimIndent()
    }
} 