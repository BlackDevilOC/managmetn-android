package com.substituemanagment.managment.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.substituemanagment.managment.data.SubstituteManager
import com.substituemanagment.managment.data.TeacherData
import com.substituemanagment.managment.data.TeacherDataManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    
    fun handleTeacherReturn(context: Context, teacherName: String, sendCancellationSms: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (sendCancellationSms) {
                    SubstituteManager.handleTeacherReturn(context, teacherName) { phone, message ->
                        // This callback will be triggered for each substitute that needs to be notified
                        // You can implement SMS sending here or handle it in the UI layer
                    }
                }
                markTeacherPresent(teacherName)
            } finally {
                _isLoading.value = false
                _showCancellationDialog.value = null
            }
        }
    }
    
    fun dismissCancellationDialog() {
        _showCancellationDialog.value = null
    }
} 