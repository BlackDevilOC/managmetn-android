package com.substituemanagment.managment.data.repository

import com.substituemanagment.managment.data.database.TeacherDao
import com.substituemanagment.managment.data.models.Teacher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TeacherRepository @Inject constructor(
    private val teacherDao: TeacherDao
) {
    fun getAllTeachers(): Flow<List<Teacher>> = teacherDao.getAllTeachers()
    
    fun getSubstituteTeachers(): Flow<List<Teacher>> = teacherDao.getSubstituteTeachers()
    
    suspend fun getTeacherById(teacherId: Int): Teacher? = teacherDao.getTeacherById(teacherId)
    
    suspend fun insertTeacher(teacher: Teacher): Long = teacherDao.insertTeacher(teacher)
    
    suspend fun insertTeachers(teachers: List<Teacher>) = teacherDao.insertTeachers(teachers)
    
    suspend fun updateTeacher(teacher: Teacher) = teacherDao.updateTeacher(teacher)
    
    suspend fun deleteTeacher(teacher: Teacher) = teacherDao.deleteTeacher(teacher)
    
    suspend fun deleteAllTeachers() = teacherDao.deleteAllTeachers()
} 