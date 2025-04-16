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
        var phoneNumber: String = "",
        val variations: List<String> = emptyList(),
        var isPresent: Boolean = true // Add attendance status
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
                
                // Automatically load teacher data
                autoLoadTeacherData()
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing: ${e.message}", e)
                _uiState.value = TeacherDetailsUiState.Error("Failed to initialize: ${e.message}")
            } finally {
                isLoading.value = false
            }
        }
    }
    
    // Automatically load teacher data (both steps in sequence)
    private fun autoLoadTeacherData() {
        viewModelScope.launch {
            isLoading.value = true
            Log.d(TAG, "Starting automatic data loading process")
            
            try {
                // Step 1: Load teacher names from total_teacher.json
                if (!totalTeachersFilePath.exists()) {
                    _uiState.value = TeacherDetailsUiState.Error("Total teachers file not found at: ${totalTeachersFilePath.absolutePath}")
                    return@launch
                }
                
                // Load existing data or load from total_teacher.json if needed
                if (teachers.isEmpty()) {
                    loadExistingTeacherDetails()
                }
                
                // If still empty, try loading from total_teacher.json
                if (teachers.isEmpty()) {
                    loadTeacherNamesFromTotalTeachers()
                    // Wait a moment for the first step to complete
                    kotlinx.coroutines.delay(500)
                }
                
                // Step 2: Match phone numbers from substitute file
                if (teachers.isNotEmpty() && substituteFilePath.exists()) {
                    matchPhoneNumbersFromSubstituteFile()
                }
                
                Log.d(TAG, "Automatic data loading process completed")
            } catch (e: Exception) {
                Log.e(TAG, "Error in automatic data loading: ${e.message}", e)
                _uiState.value = TeacherDetailsUiState.Error("Failed to load data automatically: ${e.message}")
            } finally {
                isLoading.value = false
            }
        }
    }
    
    // BUTTON 1: Load names from total_teacher.json to teacher_details.json
    fun loadTeacherNamesFromTotalTeachers() {
        viewModelScope.launch {
            isLoading.value = true
            _uiState.value = TeacherDetailsUiState.Loading
            
            try {
                // Check if total_teacher.json exists
                if (!totalTeachersFilePath.exists()) {
                    _uiState.value = TeacherDetailsUiState.Error("Total teachers file not found at: ${totalTeachersFilePath.absolutePath}")
                    return@launch
                }
                
                // Read total_teacher.json
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
                        Log.d(TAG, "Loaded ${teacherObjects.size} teacher objects from total_teacher.json")
                        
                        // Create TeacherDetails objects from the parsed data
                        val newTeachers = teacherObjects.map { 
                            TeacherDetails(it.name, it.phone, it.variations) 
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
                
                Log.d(TAG, "Loaded ${teacherNames.size} teacher names from total_teacher.json")
                
                // Create TeacherDetails objects (just names, no phone numbers yet)
                val newTeachers = teacherNames.map { TeacherDetails(it, "", emptyList()) }
                
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
                
                // Match phone numbers with existing teachers, but don't overwrite existing numbers
                var matchCount = 0
                var variationMatchCount = 0
                var normalizedMatchCount = 0
                
                val updatedTeachers = teachers.map { teacher ->
                    // Only update if the teacher doesn't have a phone number
                    if (teacher.phoneNumber.isBlank()) {
                        // Try to match by exact name first
                        if (phoneMap.containsKey(teacher.name)) {
                            matchCount++
                            teacher.copy(phoneNumber = phoneMap[teacher.name] ?: "")
                        } else {
                            // Try to match by normalized name
                            val normalizedName = normalizeTeacherName(teacher.name)
                            if (phoneMap.containsKey(normalizedName)) {
                                normalizedMatchCount++
                                Log.d(TAG, "Matched ${teacher.name} by normalized name: $normalizedName")
                                teacher.copy(phoneNumber = phoneMap[normalizedName] ?: "")
                            } else {
                                // If no direct match, try to match by variations
                                var matchedByVariation = false
                                var matchedPhone = ""
                                
                                for (variation in teacher.variations) {
                                    if (phoneMap.containsKey(variation)) {
                                        matchedByVariation = true
                                        matchedPhone = phoneMap[variation] ?: ""
                                        variationMatchCount++
                                        Log.d(TAG, "Matched ${teacher.name} by variation: $variation")
                                        break
                                    }
                                    
                                    // Try normalized variation
                                    val normalizedVariation = normalizeTeacherName(variation)
                                    if (phoneMap.containsKey(normalizedVariation)) {
                                        matchedByVariation = true
                                        matchedPhone = phoneMap[normalizedVariation] ?: ""
                                        variationMatchCount++
                                        Log.d(TAG, "Matched ${teacher.name} by normalized variation: $normalizedVariation")
                                        break
                                    }
                                }
                                
                                if (matchedByVariation) {
                                    teacher.copy(phoneNumber = matchedPhone)
                                } else {
                                    teacher
                                }
                            }
                        }
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
                val totalMatches = matchCount + normalizedMatchCount + variationMatchCount
                val matchDetails = buildString {
                    append("$matchCount direct matches")
                    if (normalizedMatchCount > 0) append(", $normalizedMatchCount by normalized names")
                    if (variationMatchCount > 0) append(", $variationMatchCount through variations")
                }
                
                _uiState.value = TeacherDetailsUiState.Success("Matched $totalMatches phone numbers ($matchDetails). $missingCount teachers still missing phone numbers.")
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
            // Use the exact path where files are saved on the user's device
            baseDir = File("/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data")
            processedDir = File(baseDir, "processed")
            rawDir = File(baseDir, "raw")
            totalTeachersFilePath = File(processedDir, "total_teacher.json")
            substituteFilePath = File(rawDir, "substitutes.csv")  // Use exact filename
            teacherDetailsFilePath = File(baseDir, "teacher_details.json")
            
            // Log all file paths
            Log.d(TAG, "Using base directory: ${baseDir.absolutePath}, exists: ${baseDir.exists()}")
            Log.d(TAG, "Using raw directory: ${rawDir.absolutePath}, exists: ${rawDir.exists()}")
            Log.d(TAG, "Using substitute file path: ${substituteFilePath.absolutePath}, exists: ${substituteFilePath.exists()}")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing file paths: ${e.message}", e)
            // Fallback to internal storage as last resort
            baseDir = File(context.filesDir, "substitute_data")
            processedDir = File(baseDir, "processed")
            rawDir = File(baseDir, "raw")
            totalTeachersFilePath = File(processedDir, "total_teacher.json")
            substituteFilePath = File(rawDir, "substitutes.csv")
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
            
            // If we have teacher details, update the substitute file to match
            if (result.isNotEmpty()) {
                updateSubstituteFileFromTeacherDetails(result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading existing teacher details: ${e.message}", e)
        }
    }

    private fun updateSubstituteFileFromTeacherDetails(teacherDetails: List<TeacherDetails>) {
        try {
            if (!substituteFilePath.exists()) {
                Log.d(TAG, "Substitute file does not exist, skipping update")
                return
            }
            
            val lines = substituteFilePath.readLines().toMutableList()
            val updatedLines = mutableListOf<String>()
            
            // Only update existing entries in substitute file
            for (line in lines) {
                val parts = line.split(",")
                if (parts.isNotEmpty()) {
                    val teacherName = parts[0].trim()
                    val teacher = teacherDetails.find { it.name == teacherName }
                    if (teacher != null) {
                        // Only update if the teacher exists in substitute file
                        updatedLines.add("${teacher.name},${teacher.phoneNumber}")
                        Log.d(TAG, "Updated phone number for existing substitute teacher: ${teacher.name}")
                    } else {
                        // Keep original line if teacher not found in teacher details
                        updatedLines.add(line)
                    }
                } else {
                    updatedLines.add(line)
                }
            }
            
            substituteFilePath.writeText(updatedLines.joinToString("\n"))
            Log.d(TAG, "Updated substitute file with existing teacher details")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating substitute file: ${e.message}", e)
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
                                // Add the name as-is
                                phoneMap[name] = phone
                                
                                // Add lowercase version
                                phoneMap[name.lowercase()] = phone
                                
                                // Add version without "sir" or "miss"
                                val nameWithoutTitle = name.replace(Regex("^(sir|miss)\\s+", RegexOption.IGNORE_CASE), "")
                                if (nameWithoutTitle.isNotBlank()) {
                                    phoneMap[nameWithoutTitle] = phone
                                    phoneMap[nameWithoutTitle.lowercase()] = phone
                                }
                                
                                // Special case for Faraz Ahmed
                                if (name.contains("Faraz", ignoreCase = true)) {
                                    phoneMap["sir faraz ahmed"] = phone
                                    phoneMap["sir Faraz Ahmed"] = phone
                                }
                                
                                validEntries++
                            }
                        } else if (parts.size == 1 && parts[0].isNotBlank()) {
                            // Teacher with no phone number
                            val name = parts[0].trim()
                            phoneMap[name] = ""
                            phoneMap[name.lowercase()] = ""
                            
                            // Add version without "sir" or "miss"
                            val nameWithoutTitle = name.replace(Regex("^(sir|miss)\\s+", RegexOption.IGNORE_CASE), "")
                            if (nameWithoutTitle.isNotBlank()) {
                                phoneMap[nameWithoutTitle] = ""
                                phoneMap[nameWithoutTitle.lowercase()] = ""
                            }
                            
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
    
    // Helper function to normalize teacher names for better matching
    private fun normalizeTeacherName(name: String): String {
        // Convert to lowercase
        var normalized = name.lowercase()
        
        // Remove common prefixes like "Sir", "Miss", etc.
        val prefixes = listOf("sir ", "miss ", "mr. ", "mrs. ", "ms. ", "dr. ")
        for (prefix in prefixes) {
            if (normalized.startsWith(prefix)) {
                normalized = normalized.substring(prefix.length)
                break
            }
        }
        
        // Remove extra spaces
        normalized = normalized.trim()
        
        return normalized
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
            try {
                // Update in memory
                val teacherIndex = teachers.indexOfFirst { it.name == name }
                if (teacherIndex >= 0) {
                    val updatedTeacher = teachers[teacherIndex].copy(phoneNumber = phone)
                    teachers[teacherIndex] = updatedTeacher
                    
                    // Save to teacher details file
                    saveTeacherDetails(teachers)
                    
                    // Update substitute file only for existing teachers
                    updateSubstituteFileFromTeacherDetails(teachers)
                    
                    _uiState.value = TeacherDetailsUiState.Success("Phone number updated for $name")
                } else {
                    _uiState.value = TeacherDetailsUiState.Error("Teacher not found: $name")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating phone number: ${e.message}", e)
                _uiState.value = TeacherDetailsUiState.Error("Failed to update phone number: ${e.message}")
            }
        }
    }
    
    fun resetState() {
        _uiState.value = TeacherDetailsUiState.Ready
    }
    
    // Check for data files in alternative locations and copy them to expected paths
    private fun checkAndCopyDataFiles(context: Context) {
        try {
            // Possible alternative paths for total_teacher.json
            val potentialTotalTeachersFiles = listOf(
                File(context.getExternalFilesDir(null), "total_teacher.json"),
                File(context.getExternalFilesDir(null), "my_fiels/data/total_teacher.json"),
                File("/storage/emulated/0/Download/total_teacher.json"),
                File("/storage/emulated/0/my_fiels/data/total_teacher.json"),
                File("/storage/emulated/0/Android/data/com.substituemanagment.managment/files/total_teacher.json"),
                // Include the original path with "original" in case that's what the user has
                File(context.getExternalFilesDir(null), "total_teacher_original.json"),
                File("/storage/emulated/0/Download/total_teacher_original.json")
            )
            
            // Possible alternative paths for substitute file
            val potentialSubstituteFiles = listOf(
                File(context.getExternalFilesDir(null), "Substitude_file.csv"),
                File(context.getExternalFilesDir(null), "my_fiels/Substitude_file.csv"),
                File("/storage/emulated/0/Download/Substitude_file.csv"),
                File("/storage/emulated/0/my_fiels/Substitude_file.csv"),
                File("/storage/emulated/0/Android/data/com.substituemanagment.managment/files/Substitude_file.csv"),
                File(context.getExternalFilesDir(null), "my_fiels/data/Substitude_file.csv"),
                File("/storage/emulated/0/my_fiels/data/Substitude_file.csv")
            )
            
            // Find and copy total_teacher.json if it exists in any alternative location
            for (file in potentialTotalTeachersFiles) {
                if (file.exists()) {
                    Log.d(TAG, "Found total_teacher.json at: ${file.absolutePath}")
                    if (!totalTeachersFilePath.exists()) {
                        file.copyTo(totalTeachersFilePath, overwrite = true)
                        Log.d(TAG, "Copied total_teacher.json to: ${totalTeachersFilePath.absolutePath}")
                    }
                    break
                }
            }
            
            // Find and copy substitute file if it exists in any alternative location
            for (file in potentialSubstituteFiles) {
                if (file.exists()) {
                    Log.d(TAG, "Found substitute file at: ${file.absolutePath}")
                    if (!substituteFilePath.exists()) {
                        // Ensure the raw directory exists
                        if (!rawDir.exists()) {
                            rawDir.mkdirs()
                        }
                        file.copyTo(substituteFilePath, overwrite = true)
                        Log.d(TAG, "Copied substitute file to: ${substituteFilePath.absolutePath}")
                    }
                    break
                }
            }
            
            // Log final file states
            Log.d(TAG, "Total teachers file exists: ${totalTeachersFilePath.exists()}")
            Log.d(TAG, "Substitute file exists: ${substituteFilePath.exists()}")
            Log.d(TAG, "Teacher details file exists: ${teacherDetailsFilePath.exists()}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking and copying data files: ${e.message}", e)
        }
    }
    
    // Add method to toggle teacher attendance status
    fun toggleTeacherAttendance(teacherName: String) {
        viewModelScope.launch {
            try {
                val teacherIndex = teachers.indexOfFirst { it.name == teacherName }
                if (teacherIndex != -1) {
                    val teacher = teachers[teacherIndex]
                    teacher.isPresent = !teacher.isPresent
                    teachers[teacherIndex] = teacher
                    
                    // Save updated teacher details
                    saveTeacherDetails(teachers)
                    
                    _uiState.value = TeacherDetailsUiState.Success(
                        "${teacher.name} marked as ${if (teacher.isPresent) "present" else "absent"}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling attendance: ${e.message}", e)
                _uiState.value = TeacherDetailsUiState.Error("Failed to update attendance: ${e.message}")
            }
        }
    }
} 