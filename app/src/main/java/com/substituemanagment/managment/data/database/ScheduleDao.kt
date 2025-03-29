package com.substituemanagment.managment.data.database

import androidx.room.*
import com.substituemanagment.managment.data.models.Schedule
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules")
    fun getAllSchedules(): Flow<List<Schedule>>

    @Query("SELECT * FROM schedules WHERE day = :day")
    fun getSchedulesByDay(day: String): Flow<List<Schedule>>

    @Query("SELECT * FROM schedules WHERE teacherId = :teacherId")
    fun getSchedulesByTeacher(teacherId: Int): Flow<List<Schedule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: Schedule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<Schedule>)

    @Update
    suspend fun updateSchedule(schedule: Schedule)

    @Delete
    suspend fun deleteSchedule(schedule: Schedule)

    @Query("DELETE FROM schedules")
    suspend fun deleteAllSchedules()
} 