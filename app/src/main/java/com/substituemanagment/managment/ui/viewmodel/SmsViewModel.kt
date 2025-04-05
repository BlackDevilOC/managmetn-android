package com.substituemanagment.managment.ui.viewmodel

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.substituemanagment.managment.data.SmsDataManager
import com.substituemanagment.managment.data.SmsHistoryDto
import com.substituemanagment.managment.data.SmsTemplateDto
import com.substituemanagment.managment.data.TeacherContactDto
import com.substituemanagment.managment.utils.SmsSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.util.*

// Paths to substitute data files
private const val EXTERNAL_STORAGE_BASE_PATH = "/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data"
private const val PROCESSED_PATH = "$EXTERNAL_STORAGE_BASE_PATH/processed"
private const val PROCESSED_ASSIGNED_SUBSTITUTES_PATH = "$PROCESSED_PATH/assigned_substitute.json"
private const val PROCESSED_TEACHERS_PATH = "$PROCESSED_PATH/total_teacher.json"

class SmsViewModel(application: Application) : AndroidViewModel(application) {
    // State holders
    val teachers = mutableStateListOf<TeacherContact>()
    val templates = mutableStateListOf<MessageTemplate>()
    val smsHistory = mutableStateListOf<SmsMessage>()
    
    // Selected teachers for the SMS process screen
    val selectedTeachersForSms = mutableStateListOf<TeacherContact>()
    
    // Selected template
    private val _selectedTemplate = mutableStateOf<MessageTemplate?>(null)
    val selectedTemplate: State<MessageTemplate?> = _selectedTemplate
    
    // Custom message
    val customMessage = mutableStateOf("")
    
    // Loading state
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading
    
    // Error message
    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage
    
    // Permission request state
    private val _needsPermission = mutableStateOf(false)
    val needsPermission: State<Boolean> = _needsPermission

    // Data models for UI
    data class TeacherContact(
        val id: String,
        val name: String,
        val phone: String,
        var selected: Boolean = false
    )
    
    // Model for substitute assignment data
    data class SubstituteAssignmentData(
        val originalTeacher: String,
        val substitute: String,
        val substitutePhone: String,
        val period: Int,
        val className: String
    )
    
    data class MessageTemplate(
        val id: String,
        val name: String,
        val content: String
    )
    
    data class SmsMessage(
        val id: String,
        val recipients: List<String>,
        val message: String,
        val timestamp: Long,
        val status: String
    )
    
    init {
        refreshData()
    }
    
    /**
     * Load or refresh all data from storage
     */
    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val context = getApplication<Application>()
                
                // Load templates
                val templateDtos = SmsDataManager.loadTemplates(context)
                val uiTemplates = templateDtos.map { dto ->
                    MessageTemplate(dto.id, dto.name, dto.content)
                }
                
                templates.clear()
                templates.addAll(uiTemplates)
                
                // Load substitute assignments first for real data
                val substitutes = loadSubstituteAssignments()
                
                // Load teachers from total_teacher.json to get all available teachers
                val allTeachers = loadAllTeachers()
                
                // Combine substitute data with all teacher data
                val teacherMap = mutableMapOf<String, TeacherContact>()
                
                // First add all teachers from the total_teacher.json
                allTeachers.forEach { teacher ->
                    teacherMap[teacher.name.lowercase()] = TeacherContact(
                        id = UUID.randomUUID().toString(),
                        name = teacher.name,
                        phone = teacher.phone
                    )
                }
                
                // Then update with substitute information for teachers who are substitutes
                substitutes.forEach { substitute ->
                    val substituteName = substitute.substitute.lowercase()
                    if (teacherMap.containsKey(substituteName)) {
                        // Update existing teacher with substitute phone
                        val existingTeacher = teacherMap[substituteName]!!
                        if (existingTeacher.phone.isBlank() && substitute.substitutePhone.isNotBlank()) {
                            teacherMap[substituteName] = existingTeacher.copy(
                                phone = substitute.substitutePhone
                            )
                        }
                    } else {
                        // Add new teacher from substitute data
                        teacherMap[substituteName] = TeacherContact(
                            id = UUID.randomUUID().toString(),
                            name = substitute.substitute,
                            phone = substitute.substitutePhone
                        )
                    }
                }
                
                // Convert map to list for UI
                teachers.clear()
                teachers.addAll(teacherMap.values)
                
                // Load SMS history
                val historyDtos = SmsDataManager.loadSmsHistory(context)
                val uiHistory = historyDtos.map { dto ->
                    SmsMessage(dto.id, dto.recipients, dto.message, dto.timestamp, dto.status)
                }
                
                smsHistory.clear()
                smsHistory.addAll(uiHistory)
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load substitute assignments from the assigned_substitute.json file
     */
    private suspend fun loadSubstituteAssignments(): List<SubstituteAssignmentData> = withContext(Dispatchers.IO) {
        try {
            val file = File(PROCESSED_ASSIGNED_SUBSTITUTES_PATH)
            if (!file.exists()) {
                return@withContext emptyList()
            }
            
            val gson = Gson()
            val content = FileReader(file).use { it.readText() }
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val data: Map<String, Any> = gson.fromJson(content, type)
            
            if (data.containsKey("assignments")) {
                val assignments = data["assignments"] as? List<*> ?: emptyList<Any>()
                return@withContext assignments.mapNotNull { assignment ->
                    if (assignment is Map<*, *>) {
                        SubstituteAssignmentData(
                            originalTeacher = (assignment["originalTeacher"] as? String ?: ""),
                            substitute = (assignment["substitute"] as? String ?: ""),
                            substitutePhone = (assignment["substitutePhone"] as? String ?: ""),
                            period = (assignment["period"] as? Double)?.toInt() 
                                  ?: (assignment["period"] as? Int) ?: 0,
                            className = (assignment["className"] as? String ?: "")
                        )
                    } else null
                }
            }
            
            return@withContext emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }
    
    /**
     * Load all teachers from total_teacher.json
     */
    private suspend fun loadAllTeachers(): List<Teacher> = withContext(Dispatchers.IO) {
        try {
            val file = File(PROCESSED_TEACHERS_PATH)
            if (!file.exists()) {
                return@withContext emptyList()
            }
            
            val gson = Gson()
            val content = FileReader(file).use { it.readText() }
            val type = object : TypeToken<List<Teacher>>() {}.type
            return@withContext gson.fromJson<List<Teacher>>(content, type)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }
    
    data class Teacher(
        val name: String,
        val phone: String,
        val isSubstitute: Boolean = false
    )
    
    /**
     * Prepare selected teachers for SMS processing
     * This should be called before navigating to the SMS process screen
     */
    fun prepareTeachersForSmsProcess() {
        selectedTeachersForSms.clear()
        selectedTeachersForSms.addAll(teachers.filter { it.selected })
    }
    
    /**
     * Update phone numbers from the SMS process screen
     */
    fun updatePhoneNumbers(updatedPhones: Map<String, String>) {
        // Update phone numbers in selectedTeachersForSms
        selectedTeachersForSms.forEachIndexed { index, teacher ->
            val updatedPhone = updatedPhones[teacher.id]
            if (updatedPhone != null && updatedPhone != teacher.phone) {
                selectedTeachersForSms[index] = teacher.copy(phone = updatedPhone)
            }
        }
        
        // Also update in the main teachers list
        for (i in teachers.indices) {
            val teacher = teachers[i]
            val updatedPhone = updatedPhones[teacher.id]
            if (updatedPhone != null && updatedPhone != teacher.phone) {
                teachers[i] = teacher.copy(phone = updatedPhone)
            }
        }
        
        // Persist updates to storage
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                
                // Update teacher contacts in storage
                val teacherDtos = teachers.map { teacher ->
                    TeacherContactDto(teacher.id, teacher.name, teacher.phone)
                }
                
                SmsDataManager.saveTeacherContacts(context, teacherDtos)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save updated phone numbers: ${e.message}"
            }
        }
    }
    
    /**
     * Toggle selection of a teacher by ID
     */
    fun toggleTeacherSelection(teacherId: String) {
        val index = teachers.indexOfFirst { it.id == teacherId }
        if (index >= 0) {
            val teacher = teachers[index]
            teachers[index] = teacher.copy(selected = !teacher.selected)
        }
    }
    
    /**
     * Select all teachers or deselect all
     */
    fun selectAllTeachers(selected: Boolean) {
        for (i in teachers.indices) {
            teachers[i] = teachers[i].copy(selected = selected)
        }
    }
    
    /**
     * Select a template by ID, or null for custom message
     */
    fun selectTemplate(templateId: String?) {
        if (templateId == null) {
            _selectedTemplate.value = null
            return
        }
        
        val template = templates.find { it.id == templateId }
        _selectedTemplate.value = template
        
        // Update custom message field with template content
        template?.let {
            customMessage.value = it.content
        }
    }
    
    /**
     * Save a new template
     */
    fun saveNewTemplate(name: String, content: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val newId = UUID.randomUUID().toString()
                val newTemplate = MessageTemplate(newId, name, content)
                
                templates.add(newTemplate)
                
                // Save to storage
                val templateDtos = templates.map { template ->
                    SmsTemplateDto(template.id, template.name, template.content)
                }
                
                SmsDataManager.saveTemplates(getApplication(), templateDtos)
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save template: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Check if SMS permissions are granted
     * @return true if permissions are granted, false otherwise
     */
    fun checkSmsPermissions(): Boolean {
        val context = getApplication<Application>()
        val hasSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
        
        _needsPermission.value = !hasSmsPermission
        return hasSmsPermission
    }
    
    /**
     * Request SMS permissions
     * @param activity The activity to request permissions from
     */
    fun requestSmsPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.SEND_SMS),
            SMS_PERMISSION_REQUEST_CODE
        )
    }
    
    /**
     * Handle permission result
     * @param requestCode The request code
     * @param grantResults The grant results
     */
    fun handlePermissionResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                _needsPermission.value = false
                // Permission was granted, try sending SMS again
                sendSms()
            } else {
                _errorMessage.value = "SMS permission denied. Cannot send messages."
            }
        }
    }
    
    /**
     * Send SMS to selected teachers
     * Uses the teachers in selectedTeachersForSms list (after phone verification)
     */
    fun sendSms() {
        // Use the verified teachers from the process screen
        val teachersToUse = if (selectedTeachersForSms.isNotEmpty()) {
            selectedTeachersForSms
        } else {
            // Fallback to selected teachers from the main list
            teachers.filter { it.selected }
        }
        
        if (teachersToUse.isEmpty()) {
            _errorMessage.value = "No recipients selected"
            return
        }
        
        if (customMessage.value.isBlank()) {
            _errorMessage.value = "Message is empty"
            return
        }
        
        // Check for SMS permission first
        if (!checkSmsPermissions()) {
            // Permission request will be handled by the UI
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val context = getApplication<Application>()
                
                val (success, message) = SmsSender.sendSms(
                    context = context,
                    recipients = teachersToUse,
                    message = customMessage.value
                )
                
                if (!success) {
                    _errorMessage.value = message ?: "Failed to send SMS"
                } else {
                    // Refresh history after sending
                    refreshData()
                    
                    // Reset UI state if no template selected
                    if (_selectedTemplate.value == null) {
                        customMessage.value = ""
                    }
                    
                    // Deselect all teachers
                    selectAllTeachers(false)
                    selectedTeachersForSms.clear()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error sending SMS: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    companion object {
        private const val SMS_PERMISSION_REQUEST_CODE = 101
    }
} 