package com.substituemanagment.managment.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
            // Load teachers from the JSON file
            val teacherList = listOf(
                "Sir Bakir Shah",
                "Sir Mushtaque Ahmed",
                "Sir Irshad Qureshi",
                "Sir Muhammad Taha",
                "Sir Shafique Ahmed",
                "Sir Iqbal Ahmed",
                "Sir Asif Nawaz",
                "Sir Amir Shaikh Senior",
                "Miss Romalia",
                "Sir Jamalullah",
                "Sir Amir Shaikh Junior",
                "Sir Abdul Hameed",
                "Sir Abdul Sami",
                "Sir Faiz Hussain",
                "Sir Usama",
                "Sir Faraz Ahmed",
                "Sir Waqar Ali",
                "Sir Faisal Ali",
                "Sir Romanulabdin",
                "Sir Rafaqat Maseeh",
                "Sir Shabeer Ahmed",
                "Sir Anwar Ali",
                "Sir Irshad Bhutto",
                "Miss Anum Hussain",
                "Sir Abdul Rehman",
                "Sir Aman Arif",
                "Sir Khadim Hussain",
                "Sir Jawad Ul Haq",
                "Sir Imran Ahmed",
                "Sir Meer Muhammad",
                "Sir Shumail Arsalan",
                "Sir Shabeer Ahmedb",
                "Sir Shoaib",
                "Sir Farhan Siddiqui"
            ).map { name ->
                Teacher(
                    name = name,
                    isAbsent = teacherDataManager.isTeacherAbsent(name)
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