package com.substituemanagment.managment.data.repository

import com.substituemanagment.managment.data.database.ScheduleDao
import com.substituemanagment.managment.data.models.Schedule
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ScheduleRepository @Inject constructor(
    private val scheduleDao: ScheduleDao
) {
    fun getAllSchedules(): Flow<List<Schedule>> = scheduleDao.getAllSchedules()
    
    fun getSchedulesByDay(day: String): Flow<List<Schedule>> = scheduleDao.getSchedulesByDay(day)
    
    fun getSchedulesByTeacher(teacherId: Int): Flow<List<Schedule>> = scheduleDao.getSchedulesByTeacher(teacherId)
    
    suspend fun insertSchedule(schedule: Schedule) = scheduleDao.insertSchedule(schedule)
    
    suspend fun insertSchedules(schedules: List<Schedule>) = scheduleDao.insertSchedules(schedules)
    
    suspend fun updateSchedule(schedule: Schedule) = scheduleDao.updateSchedule(schedule)
    
    suspend fun deleteSchedule(schedule: Schedule) = scheduleDao.deleteSchedule(schedule)
    
    suspend fun deleteAllSchedules() = scheduleDao.deleteAllSchedules()
} 