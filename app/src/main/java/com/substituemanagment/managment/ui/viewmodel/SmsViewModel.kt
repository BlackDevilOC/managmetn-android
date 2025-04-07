package com.substituemanagment.managment.ui.viewmodel

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.text.SimpleDateFormat
import java.util.*

// Paths to substitute data files
private const val EXTERNAL_STORAGE_BASE_PATH = "/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data"
private const val PROCESSED_ASSIGNED_SUBSTITUTES_PATH = "$EXTERNAL_STORAGE_BASE_PATH/assigned_substitute.json"
private const val SELECTED_TEACHERS_PATH = "$EXTERNAL_STORAGE_BASE_PATH/sms/selected_teachers.json"
private const val PROCESSED_PATH = "$EXTERNAL_STORAGE_BASE_PATH/processed"
private const val PROCESSED_TEACHERS_PATH = "$PROCESSED_PATH/total_teacher.json"

class SmsViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "SmsViewModel"
    
    // State for assignments and teachers
    val assignments = mutableStateListOf<AssignmentData>()
    val teacherAssignments = mutableStateMapOf<String, List<AssignmentData>>()
    val selectedTeachers = mutableStateMapOf<String, Boolean>()
    
    // State for processing
    val teachersToProcess = mutableStateListOf<TeacherWithAssignments>()
    val phoneNumbers = mutableStateMapOf<String, String>()
    
    // UI state
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading
    
    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage
    
    private val _needsPermission = mutableStateOf(false)
    val needsPermission: State<Boolean> = _needsPermission

    // SMS history
    val smsHistory = mutableStateListOf<SmsMessage>()
    
    // SMS message template
    val messageTemplate = mutableStateOf(
        "Dear {substitute}, you have been assigned to cover {class} Period {period} on {date} (${getCurrentDay()}). Please confirm your availability."
    )
    
    // Data models
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
    
    init {
        refreshData()
    }
    
    /**
     * Load teacher assignments from assigned_substitute.json
     */
    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                // Load SMS history
                val context = getApplication<Application>()
                val historyDtos = SmsDataManager.loadSmsHistory(context)
                smsHistory.clear()
                smsHistory.addAll(historyDtos.map { dto ->
                    SmsMessage(
                        id = dto.id,
                        teacherName = dto.recipients.firstOrNull() ?: "Unknown",
                        teacherPhone = "",
                        message = dto.message,
                        timestamp = dto.timestamp,
                        status = dto.status
                    )
                })
                
                // Load assignments from file
                val assignmentsData = loadAssignmentsFromFile()
                
                // Clear existing data
                assignments.clear()
                teacherAssignments.clear()
                selectedTeachers.clear()
                
                // Add assignments to state
                assignments.addAll(assignmentsData)
                
                // Group assignments by substitute teacher
                val groupedByTeacher = assignmentsData.groupBy { it.substitute }
                
                // Update state
                groupedByTeacher.forEach { (teacher, teacherAssignmentsList) ->
                    if (teacher.isNotEmpty()) {
                        teacherAssignments[teacher] = teacherAssignmentsList
                        // Initialize all teachers as unselected
                        selectedTeachers[teacher] = false
                    }
                }
                
                Log.d(TAG, "Loaded ${assignments.size} assignments for ${teacherAssignments.size} teachers")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading assignments", e)
                _errorMessage.value = "Failed to load data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load assignments from the assigned_substitute.json file
     */
    private suspend fun loadAssignmentsFromFile(): List<AssignmentData> = withContext(Dispatchers.IO) {
        try {
            val file = File(PROCESSED_ASSIGNED_SUBSTITUTES_PATH)
            if (!file.exists()) {
                Log.w(TAG, "Substitute assignment file does not exist at: $PROCESSED_ASSIGNED_SUBSTITUTES_PATH")
                return@withContext emptyList()
            }
            
            val fileSize = file.length()
            if (fileSize == 0L) {
                Log.w(TAG, "Substitute assignment file is empty")
                return@withContext emptyList()
            }
            
            val gson = Gson()
            val content = FileReader(file).use { it.readText() }
            
            if (content.isBlank()) {
                Log.w(TAG, "Substitute assignment file content is blank")
                return@withContext emptyList()
            }
            
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val data: Map<String, Any> = gson.fromJson(content, type)
            
            if (data.isEmpty()) {
                Log.w(TAG, "Parsed data is empty")
                return@withContext emptyList()
            }
            
            if (data.containsKey("assignments")) {
                val assignments = data["assignments"] as? List<*> ?: emptyList<Any>()
                Log.d(TAG, "Found ${assignments.size} assignments in file")
                
                return@withContext assignments.mapNotNull { assignment ->
                    if (assignment is Map<*, *>) {
                        val originalTeacher = (assignment["originalTeacher"] as? String ?: "").capitalizeWords()
                        val substitute = (assignment["substitute"] as? String ?: "").capitalizeWords()
                        val substitutePhone = assignment["substitutePhone"] as? String ?: ""
                        val period = (assignment["period"] as? Double)?.toInt() 
                              ?: (assignment["period"] as? Int) ?: 0
                        val className = assignment["className"] as? String ?: ""
                        
                        AssignmentData(
                            originalTeacher = originalTeacher,
                            substitute = substitute,
                            substitutePhone = substitutePhone,
                            period = period,
                            className = className
                        )
                    } else null
                }
            } else {
                Log.w(TAG, "No 'assignments' key found in data")
                return@withContext emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading substitute assignments", e)
            return@withContext emptyList()
        }
    }
    
    /**
     * Toggle selection state for a teacher
     */
    fun toggleTeacherSelection(teacherName: String) {
        if (selectedTeachers.containsKey(teacherName)) {
            selectedTeachers[teacherName] = !(selectedTeachers[teacherName] ?: false)
        }
    }
    
    /**
     * Select or deselect all teachers
     */
    fun selectAllTeachers(selected: Boolean) {
        selectedTeachers.keys.forEach { teacherName ->
            selectedTeachers[teacherName] = selected
        }
    }
    
    /**
     * Prepare selected teachers for SMS processing
     */
    fun prepareSelectedTeachersForSms() {
        teachersToProcess.clear()
        phoneNumbers.clear()
        
        // Get selected teachers and their assignments
        val selectedTeachersList = selectedTeachers.filter { it.value }.keys.toList()
        
        Log.d(TAG, "Preparing ${selectedTeachersList.size} selected teachers for SMS")
        
        selectedTeachersList.forEach { teacherName ->
            val assignments = teacherAssignments[teacherName] ?: emptyList()
            
            if (assignments.isNotEmpty()) {
                // Get phone number from the first assignment (they should all be the same)
                val phone = assignments.first().substitutePhone
                
                val teacherWithAssignments = TeacherWithAssignments(
                    name = teacherName,
                    phone = phone,
                    assignments = assignments
                )
                
                teachersToProcess.add(teacherWithAssignments)
                
                // Initialize phone number map
                phoneNumbers[teacherName] = phone
            }
        }
        
        // Save selected teachers to file for persistence
        saveSelectedTeachersToFile()
        
        Log.d(TAG, "Prepared ${teachersToProcess.size} teachers for SMS processing")
    }
    
    /**
     * Save selected teachers to a file to ensure persistence between screens
     */
    private fun saveSelectedTeachersToFile() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Create directory if it doesn't exist
                val smsDir = File("$EXTERNAL_STORAGE_BASE_PATH/sms")
                if (!smsDir.exists()) {
                    smsDir.mkdirs()
                }
                
                val file = File(SELECTED_TEACHERS_PATH)
                
                // Create a JSON object with selected teachers
                val teachersData = teachersToProcess.map { teacher ->
                    mapOf(
                        "name" to teacher.name,
                        "phone" to teacher.phone,
                        "assignments" to teacher.assignments.map { assignment ->
                            mapOf(
                                "originalTeacher" to assignment.originalTeacher,
                                "substitute" to assignment.substitute,
                                "substitutePhone" to assignment.substitutePhone,
                                "period" to assignment.period,
                                "className" to assignment.className,
                                "date" to assignment.date
                            )
                        }
                    )
                }
                
                val jsonData = mapOf("teachers" to teachersData)
                val gson = Gson()
                val jsonString = gson.toJson(jsonData)
                
                // Write to file
                file.writeText(jsonString)
                
                Log.d(TAG, "Saved ${teachersToProcess.size} selected teachers to file: ${file.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving selected teachers to file", e)
            }
        }
    }
    
    /**
     * Load selected teachers from file
     */
    fun loadSelectedTeachersFromFile() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(SELECTED_TEACHERS_PATH)
                if (!file.exists()) {
                    Log.d(TAG, "Selected teachers file does not exist, skipping load")
                    return@launch
                }
                
                val jsonString = file.readText()
                val gson = Gson()
                val type = object : TypeToken<Map<String, List<Map<String, Any>>>>() {}.type
                val data: Map<String, List<Map<String, Any>>> = gson.fromJson(jsonString, type)
                
                withContext(Dispatchers.Main) {
                    // Clear the current list
                    teachersToProcess.clear()
                    
                    // Parse the teachers from the JSON data
                    val teachersData = data["teachers"] ?: emptyList()
                    teachersData.forEach { teacherMap ->
                        val name = teacherMap["name"] as? String ?: ""
                        val phone = teacherMap["phone"] as? String ?: ""
                        
                        val assignmentsData = teacherMap["assignments"] as? List<Map<String, Any>> ?: emptyList()
                        val assignments = assignmentsData.mapNotNull { assignmentMap ->
                            try {
                                val originalTeacher = assignmentMap["originalTeacher"] as? String ?: ""
                                val substitute = assignmentMap["substitute"] as? String ?: ""
                                val substitutePhone = assignmentMap["substitutePhone"] as? String ?: ""
                                val period = (assignmentMap["period"] as? Double)?.toInt() 
                                      ?: (assignmentMap["period"] as? Int) ?: 0
                                val className = assignmentMap["className"] as? String ?: ""
                                val date = assignmentMap["date"] as? String ?: getCurrentDate()
                                
                                AssignmentData(
                                    originalTeacher = originalTeacher,
                                    substitute = substitute,
                                    substitutePhone = substitutePhone,
                                    period = period,
                                    className = className,
                                    date = date
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing assignment data", e)
                                null
                            }
                        }
                        
                        if (name.isNotEmpty() && assignments.isNotEmpty()) {
                            teachersToProcess.add(
                                TeacherWithAssignments(
                                    name = name,
                                    phone = phone,
                                    assignments = assignments
                                )
                            )
                            
                            // Update the phone numbers map
                            phoneNumbers[name] = phone
                        }
                    }
                    
                    Log.d(TAG, "Loaded ${teachersToProcess.size} selected teachers from file")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading selected teachers from file", e)
            }
        }
    }
    
    /**
     * Generate SMS message for a teacher based on their assignments
     */
    fun generateSmsForTeacher(teacher: TeacherWithAssignments): String {
        if (teacher.assignments.isEmpty()) return ""
        
        // Sort assignments by period
        val sortedAssignments = teacher.assignments.sortedBy { it.period }
        
        // For single assignment, use the full template
        if (sortedAssignments.size == 1) {
            val assignment = sortedAssignments.first()
            return messageTemplate.value
                .replace("{substitute}", teacher.name)
                .replace("{class}", assignment.className)
                .replace("{period}", assignment.period.toString())
                .replace("{date}", assignment.date)
                .replace("{day}", getCurrentDay())
                .replace("{original_teacher}", assignment.originalTeacher)
        } 
        
        // For multiple assignments, create a more structured message
        val baseMessage = "Dear ${teacher.name}, you have been assigned to the following classes on ${sortedAssignments.first().date} (${getCurrentDay()}):"
        
        val assignmentsText = sortedAssignments.joinToString("\n") { assignment ->
            "• Period ${assignment.period}: ${assignment.className} (for ${assignment.originalTeacher})"
        }
        
        return "$baseMessage\n\n$assignmentsText\n\nPlease confirm your availability."
    }
    
    /**
     * Update phone number for a teacher
     */
    fun updatePhoneNumber(teacherName: String, phoneNumber: String) {
        phoneNumbers[teacherName] = phoneNumber
        
        // Also update in the teachersToProcess list
        val index = teachersToProcess.indexOfFirst { it.name == teacherName }
        if (index >= 0) {
            val teacher = teachersToProcess[index]
            teachersToProcess[index] = teacher.copy(phone = phoneNumber)
        }
    }
    
    /**
     * Check if all teachers have valid phone numbers
     */
    fun checkAllPhoneNumbersValid(): Boolean {
        return teachersToProcess.all { isValidPhoneNumber(phoneNumbers[it.name] ?: "") }
    }
    
    /**
     * Get missing phone numbers
     */
    fun getTeachersWithMissingPhoneNumbers(): List<String> {
        return teachersToProcess.filter { !isValidPhoneNumber(phoneNumbers[it.name] ?: "") }
            .map { it.name }
    }
    
    /**
     * Send SMS to teachers one by one, showing progress
     * This method sends SMS messages sequentially to reduce failure risk
     */
    fun sendSmsToTeachersOneByOne(context: Context, onAllSent: () -> Unit) {
        if (teachersToProcess.isEmpty()) {
            _errorMessage.value = "No teachers selected for SMS"
            return
        }
        
        if (!checkSmsPermissions()) {
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val totalTeachers = teachersToProcess.size
                var successCount = 0
                val failedTeachers = mutableListOf<String>()
                
                // Send SMS one by one
                for ((index, teacher) in teachersToProcess.withIndex()) {
                    val message = generateSmsForTeacher(teacher)
                    val phoneNumber = phoneNumbers[teacher.name] ?: ""
                    
                    if (isValidPhoneNumber(phoneNumber) && message.isNotBlank()) {
                        // Use a delay between messages to prevent flooding
                        if (index > 0) {
                            delay(1000) // 1 second delay between messages
                        }
                        
                        // Update loading message with progress
                        _errorMessage.value = "Sending SMS to ${teacher.name} (${index + 1}/$totalTeachers)..."
                        
                        val success = SmsSender.sendSingleSms(
                            context = context,
                            phoneNumber = phoneNumber,
                            message = message
                        )
                        
                        if (success) {
                            successCount++
                            
                            // Save to history
                            val historyItem = SmsHistoryDto(
                                id = UUID.randomUUID().toString(),
                                recipients = listOf(teacher.name),
                                message = message,
                                timestamp = System.currentTimeMillis(),
                                status = "Sent"
                            )
                            SmsDataManager.addSmsToHistory(context, historyItem)
                            
                            // Add to local history list
                            smsHistory.add(0, SmsMessage(
                                id = historyItem.id,
                                teacherName = teacher.name,
                                teacherPhone = phoneNumber,
                                message = message,
                                timestamp = historyItem.timestamp,
                                status = "Sent"
                            ))
                            
                            Log.d(TAG, "Successfully sent SMS to ${teacher.name}")
                        } else {
                            failedTeachers.add(teacher.name)
                            
                            // Save failed to history
                            val historyItem = SmsHistoryDto(
                                id = UUID.randomUUID().toString(),
                                recipients = listOf(teacher.name),
                                message = message,
                                timestamp = System.currentTimeMillis(),
                                status = "Failed"
                            )
                            SmsDataManager.addSmsToHistory(context, historyItem)
                            
                            // Add to local history list
                            smsHistory.add(0, SmsMessage(
                                id = historyItem.id,
                                teacherName = teacher.name,
                                teacherPhone = phoneNumber,
                                message = message,
                                timestamp = historyItem.timestamp,
                                status = "Failed"
                            ))
                            
                            Log.e(TAG, "Failed to send SMS to ${teacher.name}")
                        }
                    } else {
                        failedTeachers.add(teacher.name)
                        Log.e(TAG, "Invalid phone number or message for ${teacher.name}")
                    }
                }
                
                // Set final message
                if (failedTeachers.isEmpty() && successCount > 0) {
                    _errorMessage.value = null
                    onAllSent()
                } else if (failedTeachers.isNotEmpty() && successCount > 0) {
                    _errorMessage.value = "Sent SMS to $successCount teachers, failed for ${failedTeachers.size} teachers"
                } else {
                    _errorMessage.value = "Failed to send SMS to all selected teachers"
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending SMS", e)
                _errorMessage.value = "Error sending SMS: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Check if SMS permissions are granted
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
     */
    fun requestSmsPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.SEND_SMS),
            SMS_PERMISSION_REQUEST_CODE
        )
    }
    
    /**
     * Helper function to capitalize first letter of each word
     */
    private fun String.capitalizeWords(): String {
        return this.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
    
    /**
     * Validate phone number format
     */
    fun isValidPhoneNumber(phone: String): Boolean {
        if (phone.isBlank()) return false
        val phoneRegex = Regex("^[+]?[(]?[0-9]{3}[)]?[-\\s.]?[0-9]{3}[-\\s.]?[0-9]{4,6}\$")
        return phone.matches(phoneRegex)
    }
    
    companion object {
        private const val SMS_PERMISSION_REQUEST_CODE = 101
        
        /**
         * Get current date formatted as yyyy-MM-dd
         */
        fun getCurrentDate(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }
        
        /**
         * Get current day of week
         */
        fun getCurrentDay(): String {
            return SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
        }
    }
} 