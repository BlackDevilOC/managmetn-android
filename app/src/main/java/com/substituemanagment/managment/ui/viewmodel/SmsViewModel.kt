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
import com.substituemanagment.managment.ui.screens.AssignmentUI
import com.substituemanagment.managment.ui.screens.SubstituteTeacherUI
import com.substituemanagment.managment.utils.SmsSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.util.*
import android.util.Log

// Paths to substitute data files
private const val EXTERNAL_STORAGE_BASE_PATH = "/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data"
private const val PROCESSED_PATH = "$EXTERNAL_STORAGE_BASE_PATH/processed"
private const val PROCESSED_ASSIGNED_SUBSTITUTES_PATH = "$EXTERNAL_STORAGE_BASE_PATH/assigned_substitute.json"
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
                
                // Check if assigned_substitute.json exists
                val substitutesFile = File(PROCESSED_ASSIGNED_SUBSTITUTES_PATH)
                Log.d("SmsViewModel", "Assignment file path: $PROCESSED_ASSIGNED_SUBSTITUTES_PATH")
                Log.d("SmsViewModel", "Assignment file exists: ${substitutesFile.exists()}")
                
                // Load substitute assignments first for real data
                val substitutes = loadSubstituteAssignments()
                Log.d("SmsViewModel", "Loaded ${substitutes.size} substitute assignments")
                
                // Only load teachers from the assigned substitute file
                val teacherMap = mutableMapOf<String, TeacherContact>()
                
                // Add teachers who are assigned as substitutes
                substitutes.forEach { substitute ->
                    if (substitute.substitute.isNotBlank()) {
                        val substituteName = substitute.substitute.lowercase()
                        if (!teacherMap.containsKey(substituteName)) {
                            teacherMap[substituteName] = TeacherContact(
                                id = UUID.randomUUID().toString(),
                                name = substitute.substitute,
                                phone = substitute.substitutePhone
                            )
                        }
                    }
                }
                
                // If we found no teachers in assignments, load from total_teacher.json as fallback
                if (teacherMap.isEmpty()) {
                    Log.d("SmsViewModel", "No substitute teachers found in assignments, loading from all teachers")
                    val allTeachers = loadAllTeachers()
                    Log.d("SmsViewModel", "Loaded ${allTeachers.size} teachers from total_teacher.json")
                    
                    allTeachers.forEach { teacher ->
                        if (teacher.isSubstitute) {
                            teacherMap[teacher.name.lowercase()] = TeacherContact(
                                id = UUID.randomUUID().toString(),
                                name = teacher.name,
                                phone = teacher.phone
                            )
                        }
                    }
                    
                    if (teacherMap.isEmpty() && allTeachers.isNotEmpty()) {
                        // If no substitutes found but teachers exist, use all teachers as a last resort
                        Log.d("SmsViewModel", "No substitutes found, using all teachers")
                        allTeachers.forEach { teacher ->
                            teacherMap[teacher.name.lowercase()] = TeacherContact(
                                id = UUID.randomUUID().toString(),
                                name = teacher.name,
                                phone = teacher.phone
                            )
                        }
                    }
                }
                
                // Convert map to list for UI
                val oldSelectedIds = teachers.filter { it.selected }.map { it.id }
                teachers.clear()
                
                // Add all teachers from the map and preserve selection state if possible
                val teacherList = teacherMap.values.toList().sortedBy { it.name }
                teachers.addAll(teacherList)
                
                // Restore selected state if this is a refresh
                if (oldSelectedIds.isNotEmpty()) {
                    teachers.forEach { teacher ->
                        if (teacher.id in oldSelectedIds) {
                            val index = teachers.indexOf(teacher)
                            if (index >= 0) {
                                teachers[index] = teacher.copy(selected = true)
                            }
                        }
                    }
                }
                
                // Load SMS history
                val historyDtos = SmsDataManager.loadSmsHistory(context)
                val uiHistory = historyDtos.map { dto ->
                    SmsMessage(dto.id, dto.recipients, dto.message, dto.timestamp, dto.status)
                }
                
                smsHistory.clear()
                smsHistory.addAll(uiHistory)
                
            } catch (e: Exception) {
                Log.e("SmsViewModel", "Error refreshing data", e)
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
                Log.w("SmsViewModel", "Substitute assignment file does not exist at: $PROCESSED_ASSIGNED_SUBSTITUTES_PATH")
                return@withContext emptyList()
            }
            
            val fileSize = file.length()
            if (fileSize == 0L) {
                Log.w("SmsViewModel", "Substitute assignment file is empty")
                return@withContext emptyList()
            }
            
            val gson = Gson()
            val content = FileReader(file).use { it.readText() }
            
            if (content.isBlank()) {
                Log.w("SmsViewModel", "Substitute assignment file content is blank")
                return@withContext emptyList()
            }
            
            Log.d("SmsViewModel", "Parsing assignment file content: ${content.take(100)}...")
            
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val data: Map<String, Any> = gson.fromJson(content, type)
            
            if (data.isEmpty()) {
                Log.w("SmsViewModel", "Parsed data is empty")
                return@withContext emptyList()
            }
            
            if (data.containsKey("assignments")) {
                val assignments = data["assignments"] as? List<*> ?: emptyList<Any>()
                Log.d("SmsViewModel", "Found ${assignments.size} assignments in file")
                
                return@withContext assignments.mapNotNull { assignment ->
                    if (assignment is Map<*, *>) {
                        val originalTeacher = assignment["originalTeacher"] as? String ?: ""
                        val substitute = assignment["substitute"] as? String ?: ""
                        val substitutePhone = assignment["substitutePhone"] as? String ?: ""
                        val period = (assignment["period"] as? Double)?.toInt() 
                              ?: (assignment["period"] as? Int) ?: 0
                        val className = assignment["className"] as? String ?: ""
                        
                        SubstituteAssignmentData(
                            originalTeacher = originalTeacher,
                            substitute = substitute,
                            substitutePhone = substitutePhone,
                            period = period,
                            className = className
                        )
                    } else null
                }
            } else {
                Log.w("SmsViewModel", "No 'assignments' key found in data")
                return@withContext emptyList()
            }
        } catch (e: Exception) {
            Log.e("SmsViewModel", "Error loading substitute assignments", e)
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
        val selectedTeachers = teachers.filter { it.selected }
        
        // Only proceed if there are selected teachers
        if (selectedTeachers.isNotEmpty()) {
            selectedTeachersForSms.clear()
            selectedTeachersForSms.addAll(selectedTeachers)
        } else {
            _errorMessage.value = "No teachers selected for SMS"
        }
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