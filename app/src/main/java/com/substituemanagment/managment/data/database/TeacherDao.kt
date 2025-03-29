package com.substituemanagment.managment.data.database

import androidx.room.*
import com.substituemanagment.managment.data.models.Teacher
import kotlinx.coroutines.flow.Flow

@Dao
interface TeacherDao {
    @Query("SELECT * FROM teachers")
    fun getAllTeachers(): Flow<List<Teacher>>

    @Query("SELECT * FROM teachers WHERE isSubstitute = 1")
    fun getSubstituteTeachers(): Flow<List<Teacher>>

    @Query("SELECT * FROM teachers WHERE id = :teacherId")
    suspend fun getTeacherById(teacherId: Int): Teacher?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: Teacher): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeachers(teachers: List<Teacher>)

    @Update
    suspend fun updateTeacher(teacher: Teacher)

    @Delete
    suspend fun deleteTeacher(teacher: Teacher)

    @Query("DELETE FROM teachers")
    suspend fun deleteAllTeachers()
} 