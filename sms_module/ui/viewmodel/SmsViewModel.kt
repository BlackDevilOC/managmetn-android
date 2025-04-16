package com.substituemanagment.managment.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SmsViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val SMS_PERMISSION_REQUEST_CODE = 101
        
        fun getCurrentDate(): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return dateFormat.format(Date())
        }
        
        fun getCurrentDay(): String {
            val dateFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            return dateFormat.format(Date())
        }
    }

    private val TAG = "SmsViewModel"
    
    // Assignment data
    val assignments = mutableStateListOf<AssignmentData>()
    val teacherAssignments = mutableStateMapOf<String, List<AssignmentData>>()
    val selectedTeachers = mutableStateMapOf<String, Boolean>()
    val teachersToProcess = mutableStateListOf<TeacherWithAssignments>()
    val phoneNumbers = mutableStateMapOf<String, String>()
    
    // State management
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading
    
    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage
    
    private val _needsPermission = mutableStateOf(false)
    val needsPermission: State<Boolean> = _needsPermission
    
    // SMS history
    val smsHistory = mutableStateListOf<SmsMessage>()
    
    // Message template
    val messageTemplate = mutableStateOf("Dear {substitute}, you have been assigned to cover {class} Period {period} on {date} (${getCurrentDay()}). Please confirm your availability.")
    
    // Phone number sources tracking
    val phoneNumberSources = mutableStateMapOf<String, String>()
    
    init {
        refreshData()
    }
    
    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // In a real app, this would load data from a repository
                // For now, we'll just simulate loading
                val loadedAssignments = loadAssignmentsFromFile()
                assignments.clear()
                assignments.addAll(loadedAssignments)
                
                // Group assignments by teacher
                val groupedAssignments = loadedAssignments.groupBy { it.originalTeacher }
                teacherAssignments.clear()
                teacherAssignments.putAll(groupedAssignments)
                
                loadPhoneNumbers()
                loadSelectedTeachersFromFile()
            } catch (e: Exception) {
                _errorMessage.value = "Error loading data: ${e.message}"
                Log.e(TAG, "Error loading data", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun loadAssignmentsFromFile(): List<AssignmentData> {
        return withContext(Dispatchers.IO) {
            // In a real app, this would load from a file or database
            // For now, we'll return sample data
            listOf(
                AssignmentData("John Doe", "Jane Smith", "1234567890", 1, "Math", getCurrentDate()),
                AssignmentData("Alice Johnson", "Bob Brown", "2345678901", 2, "Science", getCurrentDate()),
                AssignmentData("Carol White", "David Green", "3456789012", 3, "English", getCurrentDate())
            )
        }
    }
    
    fun loadPhoneNumbers() {
        // In a real app, this would load from a database or file
        // For now, we'll just set some sample data
        phoneNumbers["John Doe"] = "1234567890"
        phoneNumbers["Alice Johnson"] = "2345678901"
        phoneNumbers["Carol White"] = "3456789012"
        
        // Track the source of phone numbers
        phoneNumberSources["John Doe"] = "Auto-loaded"
        phoneNumberSources["Alice Johnson"] = "Auto-loaded"
        phoneNumberSources["Carol White"] = "Auto-loaded"
    }
    
    fun loadSelectedTeachersFromFile() {
        // In a real app, this would load from a file or database
        // For now, we'll just select all teachers
        teacherAssignments.keys.forEach { teacher ->
            selectedTeachers[teacher] = true
        }
    }
    
    private fun saveSelectedTeachersToFile() {
        // In a real app, this would save to a file or database
        Log.d(TAG, "Saving selected teachers: ${selectedTeachers.filter { it.value }.keys}")
    }
    
    fun toggleTeacherSelection(teacherName: String) {
        val currentValue = selectedTeachers[teacherName] ?: false
        selectedTeachers[teacherName] = !currentValue
    }
    
    fun selectAllTeachers(selected: Boolean) {
        teacherAssignments.keys.forEach { teacher ->
            selectedTeachers[teacher] = selected
        }
    }
    
    fun prepareSelectedTeachersForSms() {
        teachersToProcess.clear()
        
        // Get selected teachers
        val selectedTeacherNames = selectedTeachers.filter { it.value }.keys
        
        // Create TeacherWithAssignments objects for each selected teacher
        selectedTeacherNames.forEach { teacherName ->
            val assignments = teacherAssignments[teacherName] ?: emptyList()
            val phone = phoneNumbers[teacherName] ?: ""
            teachersToProcess.add(TeacherWithAssignments(teacherName, phone, assignments))
        }
        
        // Sort by teacher name
        teachersToProcess.sortBy { it.name }
    }
    
    fun generateSmsForTeacher(teacher: TeacherWithAssignments): String {
        val template = messageTemplate.value
        val assignments = teacher.assignments
        
        if (assignments.isEmpty()) {
            return "No assignments for ${teacher.name}"
        }
        
        // For single assignment, use the template directly
        if (assignments.size == 1) {
            val assignment = assignments[0]
            return createPreviewMessage(template, teacher.name, assignment)
        }
        
        // For multiple assignments, create a custom message
        val baseTemplate = "Dear ${teacher.name}, you have been assigned to cover the following classes on ${getCurrentDate()} (${getCurrentDay()}):\n"
        val assignmentDetails = assignments.joinToString("\n") { 
            "- ${it.className} Period ${it.period}" 
        }
        return baseTemplate + assignmentDetails + "\nPlease confirm your availability."
    }
    
    private fun createPreviewMessage(template: String, teacherName: String, assignment: AssignmentData): String {
        var message = template
        message = message.replace("{substitute}", teacherName)
        message = message.replace("{class}", assignment.className)
        message = message.replace("{period}", assignment.period.toString())
        message = message.replace("{date}", assignment.date)
        return message
    }
    
    fun sendSmsToTeachersOneByOne(context: Context, onAllSent: () -> Unit) {
        viewModelScope.launch {
            try {
                if (!checkSmsPermissions()) {
                    _needsPermission.value = true
                    return@launch
                }
                
                _isLoading.value = true
                
                // Make sure we have teachers to process
                if (teachersToProcess.isEmpty()) {
                    prepareSelectedTeachersForSms()
                }
                
                // Send SMS to each teacher
                teachersToProcess.forEach { teacher ->
                    if (teacher.phone.isNotBlank() && isValidPhoneNumber(teacher.phone)) {
                        val message = generateSmsForTeacher(teacher)
                        sendSms(context, teacher.phone, message)
                        
                        // Record in history
                        val smsMessage = SmsMessage(
                            UUID.randomUUID().toString(),
                            teacher.name,
                            teacher.phone,
                            message,
                            System.currentTimeMillis(),
                            "Sent"
                        )
                        smsHistory.add(0, smsMessage) // Add to beginning of list
                    }
                }
                
                onAllSent()
            } catch (e: Exception) {
                _errorMessage.value = "Error sending SMS: ${e.message}"
                Log.e(TAG, "Error sending SMS", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun sendSms(context: Context, phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.d(TAG, "SMS sent to $phoneNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
            throw e
        }
    }
    
    fun updatePhoneNumber(teacherName: String, phoneNumber: String) {
        phoneNumbers[teacherName] = phoneNumber
        phoneNumberSources[teacherName] = "Manually entered"
    }
    
    fun isValidPhoneNumber(phone: String): Boolean {
        // Simple validation - should be at least 10 digits
        return phone.replace(Regex("[^0-9]"), "").length >= 10
    }
    
    fun checkAllPhoneNumbersValid(): Boolean {
        return teachersToProcess.all { teacher -> 
            teacher.phone.isNotBlank() && isValidPhoneNumber(teacher.phone)
        }
    }
    
    fun getTeachersWithMissingPhoneNumbers(): List<String> {
        return teachersToProcess
            .filter { it.phone.isBlank() || !isValidPhoneNumber(it.phone) }
            .map { it.name }
    }
    
    fun checkSmsPermissions(): Boolean {
        val permission = android.Manifest.permission.SEND_SMS
        val context = getApplication<Application>().applicationContext
        return ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    
    fun requestSmsPermissions(activity: Activity) {
        val permission = android.Manifest.permission.SEND_SMS
        ActivityCompat.requestPermissions(activity, arrayOf(permission), SMS_PERMISSION_REQUEST_CODE)
    }
    
    private fun normalizeTeacherName(name: String): String {
        return capitalizeWords(name.trim())
    }
    
    private fun capitalizeWords(text: String): String {
        return text.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
            }
        }
    }
    
    // Data classes
    data class AssignmentData(
        val originalTeacher: String,
        val substitute: String,
        val substitutePhone: String,
        val period: Int,
        val className: String,
        val date: String = getCurrentDate()
    )
    
    data class TeacherWithAssignments(
        val name: String,
        val phone: String,
        val assignments: List<AssignmentData>
    )
    
    data class SmsMessage(
        val id: String,
        val teacherName: String,
        val teacherPhone: String,
        val message: String,
        val timestamp: Long,
        val status: String
    )
}
