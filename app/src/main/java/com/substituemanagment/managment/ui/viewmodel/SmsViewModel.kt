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
import com.substituemanagment.managment.data.SmsDataManager
import com.substituemanagment.managment.data.SmsHistoryDto
import com.substituemanagment.managment.data.SmsTemplateDto
import com.substituemanagment.managment.data.TeacherContactDto
import com.substituemanagment.managment.utils.SmsSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class SmsViewModel(application: Application) : AndroidViewModel(application) {
    // State holders
    val teachers = mutableStateListOf<TeacherContact>()
    val templates = mutableStateListOf<MessageTemplate>()
    val smsHistory = mutableStateListOf<SmsMessage>()
    
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
                
                // Load teacher contacts
                val teacherDtos = SmsDataManager.loadTeacherContacts(context)
                val uiTeachers = teacherDtos.map { dto ->
                    TeacherContact(dto.id, dto.name, dto.phone)
                }
                
                teachers.clear()
                teachers.addAll(uiTeachers)
                
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
     */
    fun sendSms() {
        if (teachers.none { it.selected }) {
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
                val selectedTeachers = teachers.filter { it.selected }
                
                val (success, message) = SmsSender.sendSms(
                    context = context,
                    recipients = selectedTeachers,
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