package com.substituemanagment.managment.data

import android.content.Context
import com.substituemanagment.managment.data.models.Schedule
import com.substituemanagment.managment.data.models.Teacher
import com.substituemanagment.managment.data.repository.AbsenceRepository
import com.substituemanagment.managment.data.repository.ScheduleRepository
import com.substituemanagment.managment.data.repository.TeacherRepository
import com.substituemanagment.managment.utils.CsvProcessor
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DataManager @Inject constructor(
    private val context: Context,
    private val teacherRepository: TeacherRepository,
    private val scheduleRepository: ScheduleRepository,
    private val absenceRepository: AbsenceRepository
) {
    private val csvProcessor = CsvProcessor(context)

    suspend fun loadInitialData() {
        // Process and insert substitute teachers
        val substituteTeachers = csvProcessor.processSubstituteFile("Substitude_file.csv")
        teacherRepository.insertTeachers(substituteTeachers)

        // Get all teachers to map names to IDs
        val allTeachers = teacherRepository.getAllTeachers().first()
        val teacherNameToId = allTeachers.associateBy { it.name }

        // Process and insert schedules
        val schedules = csvProcessor.processTimetableFile("timetable_file.csv")
        val schedulesWithTeacherIds = schedules.map { schedule ->
            // Find teacher by name and update teacherId
            val teacherId = teacherNameToId[schedule.className]?.id ?: 0
            schedule.copy(teacherId = teacherId)
        }
        scheduleRepository.insertSchedules(schedulesWithTeacherIds)
    }

    suspend fun refreshData() {
        // Clear existing data
        teacherRepository.deleteAllTeachers()
        scheduleRepository.deleteAllSchedules()
        absenceRepository.deleteAllAbsences()

        // Reload data
        loadInitialData()
    }
} 