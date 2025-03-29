package com.substituemanagment.managment.data.repository

import com.substituemanagment.managment.data.dao.ScheduleDao
import com.substituemanagment.managment.data.models.Schedule
import kotlinx.coroutines.flow.Flow

class ScheduleRepository(
    private val scheduleDao: ScheduleDao
) {
    fun getAllSchedules(): Flow<List<Schedule>> = scheduleDao.getAllSchedules()
    
    fun getSchedulesByTeacher(teacherId: Int): Flow<List<Schedule>> = 
        scheduleDao.getSchedulesByTeacher(teacherId)
    
    fun getSchedulesByDay(day: String): Flow<List<Schedule>> = 
        scheduleDao.getSchedulesByDay(day)
    
    suspend fun insertSchedule(schedule: Schedule): Long = scheduleDao.insertSchedule(schedule)
    
    suspend fun insertSchedules(schedules: List<Schedule>) = scheduleDao.insertSchedules(schedules)
    
    suspend fun updateSchedule(schedule: Schedule) = scheduleDao.updateSchedule(schedule)
    
    suspend fun deleteSchedule(schedule: Schedule) = scheduleDao.deleteSchedule(schedule)
    
    suspend fun deleteAllSchedules() = scheduleDao.deleteAllSchedules()
} 