package com.substituemanagment.managment.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
                teacherDataManager.markTeacherPresent(teacherName)
            } else {
                teacherDataManager.markTeacherAbsent(teacherName)
            }
            loadTeachers() // Reload the list to update UI
        }
    }
} 