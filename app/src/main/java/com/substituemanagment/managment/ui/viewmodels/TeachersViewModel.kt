package com.substituemanagment.managment.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.substituemanagment.managment.algorithm.SubstituteManager
import com.substituemanagment.managment.data.TeacherData
import com.substituemanagment.managment.data.TeacherDataManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.FileOutputStream

data class Teacher(
    val name: String,
    val isAbsent: Boolean = false
)

class TeachersViewModel : ViewModel() {
    private lateinit var teacherDataManager: TeacherDataManager
    private val _teachers = MutableStateFlow<List<Teacher>>(emptyList())
    val teachers: StateFlow<List<Teacher>> = _teachers.asStateFlow()
    
    private val _showCancellationDialog = MutableStateFlow<String?>(null)
    val showCancellationDialog: StateFlow<String?> = _showCancellationDialog.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun initialize(context: Context) {
        teacherDataManager = TeacherDataManager(context)
        loadTeachers()
    }

    private fun loadTeachers() {
        viewModelScope.launch {
            val teacherDataList = teacherDataManager.getAllTeachers()
            val teacherList = teacherDataList.map { teacherData ->
                Teacher(
                    name = teacherData.name,
                    isAbsent = teacherDataManager.isTeacherAbsent(teacherData.name)
                )
            }
            _teachers.value = teacherList
        }
    }

    fun toggleTeacherAttendance(teacherName: String) {
        viewModelScope.launch {
            if (teacherDataManager.isTeacherAbsent(teacherName)) {
                // Teacher is being marked as present
                if (teacherDataManager.isTeacherAssigned(teacherName)) {
                    // Show dialog to confirm cancellation notifications
                    _showCancellationDialog.value = teacherName
                } else {
                    // No substitute assigned, just mark as present
                    markTeacherPresent(teacherName)
                }
            } else {
                // Mark as absent
                teacherDataManager.markTeacherAbsent(teacherName)
                loadTeachers()
            }
        }
    }
    
    fun markTeacherPresent(teacherName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                teacherDataManager.markTeacherPresent(teacherName)
                loadTeachers()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun dismissCancellationDialog() {
        _showCancellationDialog.value = null
    }

    fun resetAttendanceAndAssignments(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Reset attendance records
                val baseDir = File("/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data")
                val absentTeachersFile = File(baseDir, "absent_teachers.json")
                
                // Clear absent teachers file
                FileOutputStream(absentTeachersFile).use { outputStream ->
                    outputStream.write("[]".toByteArray())
                }

                // Reset substitute assignments
                val substituteManager = SubstituteManager(context)
                substituteManager.clearAssignments()

                // Reload teachers to update UI
                loadTeachers()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
} 