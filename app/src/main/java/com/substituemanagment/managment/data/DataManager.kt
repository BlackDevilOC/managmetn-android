package com.substituemanagment.managment.data

import android.content.Context
import com.substituemanagment.managment.data.database.AppDatabase
import com.substituemanagment.managment.data.repository.AbsenceRepository
import com.substituemanagment.managment.data.repository.ScheduleRepository
import com.substituemanagment.managment.data.repository.TeacherRepository
import com.substituemanagment.managment.data.utils.CsvProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataManager @Inject constructor(
    private val context: Context,
    private val teacherRepository: TeacherRepository,
    private val scheduleRepository: ScheduleRepository,
    private val absenceRepository: AbsenceRepository
) {
    private val csvProcessor = CsvProcessor(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    fun loadInitialData() {
        scope.launch {
            try {
                // Load teachers first
                val teachers = csvProcessor.processSubstituteFile("Substitude_file.csv")
                teacherRepository.insertTeachers(teachers)

                // Create a map of teacher names to IDs
                val teacherMap = teachers.associateBy { it.name }

                // Load schedules and update teacher IDs
                val schedules = csvProcessor.processTimetableFile("timetable_file.csv")
                val updatedSchedules = schedules.map { schedule ->
                    schedule.copy(
                        teacherId = teacherMap[schedule.teacherName]?.id ?: 0
                    )
                }
                scheduleRepository.insertSchedules(updatedSchedules)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun refreshData() {
        scope.launch {
            try {
                // Clear existing data
                teacherRepository.deleteAllTeachers()
                scheduleRepository.deleteAllSchedules()
                absenceRepository.deleteAllAbsences()

                // Reload data
                loadInitialData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
} 