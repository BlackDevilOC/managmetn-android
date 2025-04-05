package com.substituemanagment.managment.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
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
    
    // UI State
    val templates = mutableStateListOf<SmsTemplate>()
    val teachers = mutableStateListOf<TeacherContact>()
    val smsHistory = mutableStateListOf<SmsHistory>()
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    
    // Selected template and message
    val selectedTemplate = mutableStateOf<SmsTemplate?>(null)
    val customMessage = mutableStateOf("")
    
    // Data classes for UI
    data class SmsTemplate(
        val id: String,
        val name: String,
        val content: String
    )
    
    data class TeacherContact(
        val id: String,
        val name: String,
        val phone: String,
        var selected: Boolean = false
    )
    
    data class SmsHistory(
        val id: String,
        val recipients: List<String>,
        val message: String,
        val timestamp: Long,
        val status: String
    )
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            
            try {
                withContext(Dispatchers.IO) {
                    // Load templates
                    val templateDtos = SmsDataManager.loadTemplates(getApplication())
                    val uiTemplates = templateDtos.map { dto ->
                        SmsTemplate(dto.id, dto.name, dto.content)
                    }
                    
                    // Load teacher contacts
                    val contactDtos = SmsDataManager.loadTeacherContacts(getApplication())
                    val uiContacts = contactDtos.map { dto ->
                        TeacherContact(dto.id, dto.name, dto.phone)
                    }
                    
                    // Load SMS history
                    val historyDtos = SmsDataManager.loadSmsHistory(getApplication())
                    val uiHistory = historyDtos.map { dto ->
                        SmsHistory(dto.id, dto.recipients, dto.message, dto.timestamp, dto.status)
                    }
                    
                    withContext(Dispatchers.Main) {
                        templates.clear()
                        templates.addAll(uiTemplates)
                        
                        teachers.clear()
                        teachers.addAll(uiContacts)
                        
                        smsHistory.clear()
                        smsHistory.addAll(uiHistory)
                    }
                }
            } catch (e: Exception) {
                errorMessage.value = "Failed to load data: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }
    
    fun saveNewTemplate(name: String, content: String) {
        viewModelScope.launch {
            try {
                val newId = UUID.randomUUID().toString()
                val newTemplate = SmsTemplate(newId, name, content)
                
                templates.add(newTemplate)
                
                // Save to storage
                withContext(Dispatchers.IO) {
                    val templateDtos = templates.map { template ->
                        SmsTemplateDto(template.id, template.name, template.content)
                    }
                    SmsDataManager.saveTemplates(getApplication(), templateDtos)
                }
            } catch (e: Exception) {
                errorMessage.value = "Failed to save template: ${e.message}"
            }
        }
    }
    
    fun deleteTemplate(templateId: String) {
        viewModelScope.launch {
            try {
                val index = templates.indexOfFirst { it.id == templateId }
                if (index != -1) {
                    templates.removeAt(index)
                    
                    // Save to storage
                    withContext(Dispatchers.IO) {
                        val templateDtos = templates.map { template ->
                            SmsTemplateDto(template.id, template.name, template.content)
                        }
                        SmsDataManager.saveTemplates(getApplication(), templateDtos)
                    }
                }
            } catch (e: Exception) {
                errorMessage.value = "Failed to delete template: ${e.message}"
            }
        }
    }
    
    fun addTeacherContact(name: String, phone: String) {
        viewModelScope.launch {
            try {
                val newId = UUID.randomUUID().toString()
                val newContact = TeacherContact(newId, name, phone)
                
                teachers.add(newContact)
                
                // Save to storage
                withContext(Dispatchers.IO) {
                    val contactDtos = teachers.map { contact ->
                        TeacherContactDto(contact.id, contact.name, contact.phone)
                    }
                    SmsDataManager.saveTeacherContacts(getApplication(), contactDtos)
                }
            } catch (e: Exception) {
                errorMessage.value = "Failed to add teacher: ${e.message}"
            }
        }
    }
    
    fun deleteTeacherContact(contactId: String) {
        viewModelScope.launch {
            try {
                val index = teachers.indexOfFirst { it.id == contactId }
                if (index != -1) {
                    teachers.removeAt(index)
                    
                    // Save to storage
                    withContext(Dispatchers.IO) {
                        val contactDtos = teachers.map { contact ->
                            TeacherContactDto(contact.id, contact.name, contact.phone)
                        }
                        SmsDataManager.saveTeacherContacts(getApplication(), contactDtos)
                    }
                }
            } catch (e: Exception) {
                errorMessage.value = "Failed to delete teacher: ${e.message}"
            }
        }
    }
    
    fun selectTemplate(templateId: String?) {
        if (templateId == null) {
            selectedTemplate.value = null
            customMessage.value = ""
            return
        }
        
        val template = templates.find { it.id == templateId }
        selectedTemplate.value = template
        if (template != null) {
            customMessage.value = template.content
        }
    }
    
    fun toggleTeacherSelection(teacherId: String) {
        val index = teachers.indexOfFirst { it.id == teacherId }
        if (index != -1) {
            teachers[index] = teachers[index].copy(selected = !teachers[index].selected)
        }
    }
    
    fun selectAllTeachers(selected: Boolean) {
        for (i in teachers.indices) {
            teachers[i] = teachers[i].copy(selected = selected)
        }
    }
    
    fun sendSms() {
        viewModelScope.launch {
            try {
                isLoading.value = true
                
                // Collect selected teachers
                val selectedTeachers = teachers.filter { it.selected }
                if (selectedTeachers.isEmpty()) {
                    errorMessage.value = "No teachers selected"
                    return@launch
                }
                
                // Check if we have a message
                if (customMessage.value.isBlank()) {
                    errorMessage.value = "Message is empty"
                    return@launch
                }
                
                // Use the SmsSender utility to send the SMS
                val (success, error) = SmsSender.sendSms(
                    context = getApplication(),
                    recipients = selectedTeachers,
                    message = customMessage.value,
                    saveToHistory = true
                )
                
                if (!success) {
                    errorMessage.value = error ?: "Failed to send SMS"
                    return@launch
                }
                
                // Reload SMS history
                reloadSmsHistory()
                
                // Reset UI state after successful sending
                customMessage.value = ""
                selectAllTeachers(false)
                selectedTemplate.value = null
                
            } catch (e: Exception) {
                errorMessage.value = "Failed to send SMS: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }
    
    private fun reloadSmsHistory() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Load SMS history
                    val historyDtos = SmsDataManager.loadSmsHistory(getApplication())
                    val uiHistory = historyDtos.map { dto ->
                        SmsHistory(dto.id, dto.recipients, dto.message, dto.timestamp, dto.status)
                    }
                    
                    withContext(Dispatchers.Main) {
                        smsHistory.clear()
                        smsHistory.addAll(uiHistory)
                    }
                }
            } catch (e: Exception) {
                errorMessage.value = "Failed to reload SMS history: ${e.message}"
            }
        }
    }
    
    fun refreshData() {
        loadData()
    }
} 