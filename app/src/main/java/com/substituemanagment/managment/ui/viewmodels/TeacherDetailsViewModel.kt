package com.substituemanagment.managment.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.*
import java.util.*

class TeacherDetailsViewModel : ViewModel() {
    private val TAG = "TeacherDetailsViewModel"
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    
    // Base directory for app files - will be initialized in initialize()
    private lateinit var baseDir: File
    private lateinit var processedDir: File
    private lateinit var rawDir: File
    private lateinit var totalTeachersFilePath: File
    private lateinit var substituteFilePath: File
    private lateinit var teacherDetailsFilePath: File
    
    // UI state
    private val _uiState = MutableStateFlow<TeacherDetailsUiState>(TeacherDetailsUiState.Ready)
    val uiState: StateFlow<TeacherDetailsUiState> = _uiState.asStateFlow()
    
    // Teachers data
    val teachers = mutableStateListOf<TeacherDetails>()
    
    // Loading state
    val isLoading = mutableStateOf(false)
    
    // Model for Teacher Details
    data class TeacherDetails(
        val name: String,
        var phoneNumber: String = ""
    )
    
    // UI state sealed class
    sealed class TeacherDetailsUiState {
        object Ready : TeacherDetailsUiState()
        object Loading : TeacherDetailsUiState()
        data class Success(val message: String) : TeacherDetailsUiState()
        data class Error(val message: String) : TeacherDetailsUiState()
    }

    // Initialize basic file paths and display any existing teachers
    fun initialize(context: Context) {
        viewModelScope.launch {
            isLoading.value = true
            
            try {
                // Set up file paths using context
                initializeFilePaths(context)
                
                // Log file paths for debugging
                Log.d(TAG, "Base directory: ${baseDir.absolutePath}, exists: ${baseDir.exists()}")
                Log.d(TAG, "Processed directory: ${processedDir.absolutePath}, exists: ${processedDir.exists()}")
                Log.d(TAG, "Raw directory: ${rawDir.absolutePath}, exists: ${rawDir.exists()}")
                Log.d(TAG, "Total teachers file: ${totalTeachersFilePath.absolutePath}, exists: ${totalTeachersFilePath.exists()}")
                Log.d(TAG, "Substitute file: ${substituteFilePath.absolutePath}, exists: ${substituteFilePath.exists()}")
                Log.d(TAG, "Teacher details file: ${teacherDetailsFilePath.absolutePath}, exists: ${teacherDetailsFilePath.exists()}")
                
                // Ensure directories exist
                baseDir.mkdirs()
                processedDir.mkdirs()
                rawDir.mkdirs()
                
                // Check for files in alternative locations and copy if needed
                checkAndCopyDataFiles(context)
                
                // Load any existing teacher details and display
                loadExistingTeacherDetails()
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing: ${e.message}", e)
                _uiState.value = TeacherDetailsUiState.Error("Failed to initialize: ${e.message}")
            } finally {
                isLoading.value = false
            }
        }
    }
    
    // BUTTON 1: Load names from total_teachers.json to teacher_details.json
    fun loadTeacherNamesFromTotalTeachers() {
        viewModelScope.launch {
            isLoading.value = true
            _uiState.value = TeacherDetailsUiState.Loading
            
            try {
                // Check if total_teachers.json exists
                if (!totalTeachersFilePath.exists()) {
                    _uiState.value = TeacherDetailsUiState.Error("Total teachers file not found at: ${totalTeachersFilePath.absolutePath}")
                    return@launch
                }
                
                // Read total_teachers.json
                val json = totalTeachersFilePath.readText()
                if (json.isBlank()) {
                    _uiState.value = TeacherDetailsUiState.Error("Total teachers file is empty")
                    return@launch
                }
                
                // Try to parse as a complex object first (with name, phone, variations fields)
                try {
                    // Define a data class to match the structure in the file
                    data class TeacherJson(
                        val name: String,
                        val phone: String = "",
                        val variations: List<String> = listOf()
                    )
                    
                    val type = object : TypeToken<List<TeacherJson>>() {}.type
                    val teacherObjects = gson.fromJson<List<TeacherJson>>(json, type) ?: emptyList()
                    
                    if (teacherObjects.isNotEmpty()) {
                        Log.d(TAG, "Loaded ${teacherObjects.size} teacher objects from total_teachers.json")
                        
                        // Create TeacherDetails objects from the parsed data
                        val newTeachers = teacherObjects.map { 
                            TeacherDetails(it.name, it.phone) 
                        }
                        
                        // Save to teacher_details.json
                        saveTeacherDetails(newTeachers)
                        
                        // Update UI list
                        teachers.clear()
                        teachers.addAll(newTeachers)
                        
                        _uiState.value = TeacherDetailsUiState.Success("${newTeachers.size} teacher names imported successfully")
                        return@launch
                    }
                } catch (e: Exception) {
                    // If parsing as complex object fails, try the original way (simple list of strings)
                    Log.d(TAG, "Failed to parse JSON as complex objects: ${e.message}. Trying simple format.")
                }
                
                // Fallback to original parsing (simple list of strings)
                val type = object : TypeToken<List<String>>() {}.type
                val teacherNames = gson.fromJson<List<String>>(json, type) ?: emptyList()
                
                if (teacherNames.isEmpty()) {
                    _uiState.value = TeacherDetailsUiState.Error("No teacher names found in the file")
                    return@launch
                }
                
                Log.d(TAG, "Loaded ${teacherNames.size} teacher names from total_teachers.json")
                
                // Create TeacherDetails objects (just names, no phone numbers yet)
                val newTeachers = teacherNames.map { TeacherDetails(it, "") }
                
                // Save to teacher_details.json
                saveTeacherDetails(newTeachers)
                
                // Update UI list
                teachers.clear()
                teachers.addAll(newTeachers)
                
                _uiState.value = TeacherDetailsUiState.Success("${newTeachers.size} teacher names imported successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading teacher names: ${e.message}", e)
                _uiState.value = TeacherDetailsUiState.Error("Failed to load teacher names: ${e.message}")
            } finally {
                isLoading.value = false
            }
        }
    }
    
    // BUTTON 2: Match phone numbers from substitute file to teachers
    fun matchPhoneNumbersFromSubstituteFile() {
        viewModelScope.launch {
            isLoading.value = true
            _uiState.value = TeacherDetailsUiState.Loading
            
            try {
                // Check if substitute file exists
                if (!substituteFilePath.exists()) {
                    _uiState.value = TeacherDetailsUiState.Error("Substitute file not found at: ${substituteFilePath.absolutePath}")
                    return@launch
                }
                
                // Load current teacher details
                if (teachers.isEmpty()) {
                    loadExistingTeacherDetails()
                }
                
                if (teachers.isEmpty()) {
                    _uiState.value = TeacherDetailsUiState.Error("Please load teacher names first (Button 1)")
                    return@launch
                }
                
                // Read substitute file to get phone numbers
                val phoneMap = loadSubstitutePhones()
                if (phoneMap.isEmpty()) {
                    _uiState.value = TeacherDetailsUiState.Error("No phone numbers found in substitute file")
                    return@launch
                }
                
                // Match phone numbers with existing teachers
                var matchCount = 0
                val updatedTeachers = teachers.map { teacher ->
                    if (phoneMap.containsKey(teacher.name)) {
                        matchCount++
                        teacher.copy(phoneNumber = phoneMap[teacher.name] ?: "")
                    } else {
                        teacher
                    }
                }
                
                // Save updated teacher details
                saveTeacherDetails(updatedTeachers)
                
                // Update UI list
                teachers.clear()
                teachers.addAll(updatedTeachers)
                
                val missingCount = teachers.count { it.phoneNumber.isBlank() }
                _uiState.value = TeacherDetailsUiState.Success("Matched $matchCount phone numbers. $missingCount teachers still missing phone numbers.")
            } catch (e: Exception) {
                Log.e(TAG, "Error matching phone numbers: ${e.message}", e)
                _uiState.value = TeacherDetailsUiState.Error("Failed to match phone numbers: ${e.message}")
            } finally {
                isLoading.value = false
            }
        }
    }
    
    private fun initializeFilePaths(context: Context) {
        try {
            // Try to use the context to get the proper external files directory
            val externalFilesDir = context.getExternalFilesDir(null)
            Log.d(TAG, "External files directory: ${externalFilesDir?.absolutePath}, exists: ${externalFilesDir?.exists()}")
            
            baseDir = context.getExternalFilesDir("substitute_data") ?: 
                      File("/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data")
            
            // Fall back to internal storage if external storage isn't available
            if (!baseDir.exists() && !baseDir.mkdirs()) {
                Log.w(TAG, "Could not create external directory, falling back to internal storage")
                baseDir = File(context.filesDir, "substitute_data")
                if (!baseDir.exists()) {
                    baseDir.mkdirs()
                }
            }
            
            processedDir = File(baseDir, "processed")
            rawDir = File(baseDir, "raw")
            totalTeachersFilePath = File(processedDir, "total_teachers.json")
            substituteFilePath = File(rawDir, "Substitude_file.csv")
            teacherDetailsFilePath = File(baseDir, "teacher_details.json")
            
            // Log all file paths
            Log.d(TAG, "Using base directory: ${baseDir.absolutePath}, exists: ${baseDir.exists()}")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing file paths: ${e.message}", e)
            // Fallback to internal storage as last resort
            baseDir = File(context.filesDir, "substitute_data")
            processedDir = File(baseDir, "processed")
            rawDir = File(baseDir, "raw")
            totalTeachersFilePath = File(processedDir, "total_teachers.json")
            substituteFilePath = File(rawDir, "Substitude_file.csv")
            teacherDetailsFilePath = File(baseDir, "teacher_details.json")
        }
    }
    
    private fun loadExistingTeacherDetails() {
        try {
            if (!teacherDetailsFilePath.exists()) {
                Log.d(TAG, "Teacher details file does not exist: ${teacherDetailsFilePath.absolutePath}")
                return
            }
            
            val json = teacherDetailsFilePath.readText()
            Log.d(TAG, "Teacher details JSON content length: ${json.length}")
            if (json.isBlank()) {
                Log.w(TAG, "Teacher details file is empty")
                return
            }
            
            val type = object : TypeToken<List<TeacherDetails>>() {}.type
            val result = gson.fromJson<List<TeacherDetails>>(json, type) ?: emptyList()
            Log.d(TAG, "Loaded ${result.size} teachers from existing teacher details")
            
            // Update UI list
            teachers.clear()
            teachers.addAll(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading existing teacher details: ${e.message}", e)
        }
    }
    
    private fun loadSubstitutePhones(): Map<String, String> {
        val phoneMap = mutableMapOf<String, String>()
        
        try {
            if (!substituteFilePath.exists()) {
                Log.d(TAG, "Substitute file does not exist: ${substituteFilePath.absolutePath}")
                return phoneMap
            }
            
            BufferedReader(FileReader(substituteFilePath)).use { reader ->
                var lineCount = 0
                var validEntries = 0
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    lineCount++
                    line?.split(",")?.let { parts ->
                        if (parts.size >= 2) {
                            val name = parts[0].trim()
                            val phone = parts[1].trim()
                            if (name.isNotBlank()) {
                                phoneMap[name] = phone
                                validEntries++
                            }
                        } else if (parts.size == 1 && parts[0].isNotBlank()) {
                            // Teacher with no phone number
                            phoneMap[parts[0].trim()] = ""
                            validEntries++
                        }
                    }
                }
                Log.d(TAG, "Read $lineCount lines from substitute file, found $validEntries valid entries")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading substitute phones: ${e.message}", e)
        }
        
        return phoneMap
    }
    
    private fun saveTeacherDetails(teachers: List<TeacherDetails>) {
        try {
            // Ensure the parent directory exists
            if (!baseDir.exists() && !baseDir.mkdirs()) {
                Log.e(TAG, "Failed to create directory: ${baseDir.absolutePath}")
                return
            }
            
            val json = gson.toJson(teachers)
            // First write to a temporary file
            val tempFile = File(baseDir, "temp_teachers.json")
            tempFile.writeText(json)
            
            // Check if temporary file was written successfully
            if (tempFile.exists() && tempFile.length() > 0) {
                // If successful, rename to the actual file
                if (teacherDetailsFilePath.exists()) {
                    teacherDetailsFilePath.delete()
                }
                val success = tempFile.renameTo(teacherDetailsFilePath)
                if (success) {
                    Log.d(TAG, "Teacher details saved to ${teacherDetailsFilePath.absolutePath} (${teachers.size} teachers)")
                } else {
                    Log.e(TAG, "Failed to rename temp file to ${teacherDetailsFilePath.absolutePath}")
                }
            } else {
                Log.e(TAG, "Failed to write temp file: ${tempFile.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving teacher details: ${e.message}", e)
        }
    }
    
    fun updateTeacherPhone(name: String, phone: String) {
        viewModelScope.launch {
            val teacherIndex = teachers.indexOfFirst { it.name == name }
            if (teacherIndex >= 0) {
                val updatedTeacher = teachers[teacherIndex].copy(phoneNumber = phone)
                teachers[teacherIndex] = updatedTeacher
                
                // Save all teachers to file
                saveTeacherDetails(teachers)
                
                _uiState.value = TeacherDetailsUiState.Success("Phone number updated for ${updatedTeacher.name}")
            } else {
                _uiState.value = TeacherDetailsUiState.Error("Teacher not found: $name")
            }
        }
    }
    
    fun resetState() {
        _uiState.value = TeacherDetailsUiState.Ready
    }
    
    // Check for data files in alternative locations and copy them to expected paths
    private fun checkAndCopyDataFiles(context: Context) {
        try {
            // Possible alternative paths for total_teachers.json
            val potentialTotalTeachersFiles = listOf(
                File(context.getExternalFilesDir(null), "total_teacher_original.json"),
                File(context.getExternalFilesDir(null), "my_fiels/data/total_teacher_original.json"),
                File("/storage/emulated/0/Download/total_teacher_original.json"),
                File("/storage/emulated/0/my_fiels/data/total_teacher_original.json"),
                File("/storage/emulated/0/Android/data/com.substituemanagment.managment/files/total_teacher_original.json")
            )
            
            // Possible alternative paths for substitute file
            val potentialSubstituteFiles = listOf(
                File(context.getExternalFilesDir(null), "Substitude_file.csv"),
                File(context.getExternalFilesDir(null), "my_fiels/Substitude_file.csv"),
                File("/storage/emulated/0/Download/Substitude_file.csv"),
                File("/storage/emulated/0/my_fiels/Substitude_file.csv"),
                File("/storage/emulated/0/Android/data/com.substituemanagment.managment/files/Substitude_file.csv")
            )
            
            // Check if expected files already exist
            if (!totalTeachersFilePath.exists()) {
                // Try to find total_teachers.json in alternative locations
                for (file in potentialTotalTeachersFiles) {
                    if (file.exists()) {
                        Log.d(TAG, "Found total teachers file at alternative location: ${file.absolutePath}")
                        file.copyTo(totalTeachersFilePath, overwrite = true)
                        Log.d(TAG, "Copied total teachers file to: ${totalTeachersFilePath.absolutePath}")
                        break
                    }
                }
            }
            
            if (!substituteFilePath.exists()) {
                // Try to find Substitude_file.csv in alternative locations
                for (file in potentialSubstituteFiles) {
                    if (file.exists()) {
                        Log.d(TAG, "Found substitute file at alternative location: ${file.absolutePath}")
                        file.copyTo(substituteFilePath, overwrite = true)
                        Log.d(TAG, "Copied substitute file to: ${substituteFilePath.absolutePath}")
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copying data files: ${e.message}", e)
        }
    }
} 