package com.substituemanagment.managment.data.repository

import com.substituemanagment.managment.data.dao.TeacherDao
import com.substituemanagment.managment.data.models.Teacher
import kotlinx.coroutines.flow.Flow

class TeacherRepository(
    private val teacherDao: TeacherDao
) {
    fun getAllTeachers(): Flow<List<Teacher>> = teacherDao.getAllTeachers()
    
    fun getAllSubstitutes(): Flow<List<Teacher>> = teacherDao.getAllSubstitutes()
    
    suspend fun getTeacherById(id: Int): Teacher? = teacherDao.getTeacherById(id)
    
    suspend fun insertTeacher(teacher: Teacher): Long = teacherDao.insertTeacher(teacher)
    
    suspend fun insertTeachers(teachers: List<Teacher>) = teacherDao.insertTeachers(teachers)
    
    suspend fun updateTeacher(teacher: Teacher) = teacherDao.updateTeacher(teacher)
    
    suspend fun deleteTeacher(teacher: Teacher) = teacherDao.deleteTeacher(teacher)
    
    suspend fun deleteAllTeachers() = teacherDao.deleteAllTeachers()
} 