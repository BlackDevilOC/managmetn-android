package com.substituemanagment.managment.data.repository

import com.substituemanagment.managment.data.database.AbsenceDao
import com.substituemanagment.managment.data.models.Absence
import com.substituemanagment.managment.data.models.AbsenceStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AbsenceRepository @Inject constructor(
    private val absenceDao: AbsenceDao
) {
    fun getAllAbsences(): Flow<List<Absence>> = absenceDao.getAllAbsences()
    
    fun getAbsencesByDate(date: String): Flow<List<Absence>> = absenceDao.getAbsencesByDate(date)
    
    fun getAbsencesByTeacher(teacherId: Int): Flow<List<Absence>> = absenceDao.getAbsencesByTeacher(teacherId)
    
    fun getAbsencesByStatus(status: AbsenceStatus): Flow<List<Absence>> = absenceDao.getAbsencesByStatus(status)
    
    suspend fun insertAbsence(absence: Absence) = absenceDao.insertAbsence(absence)
    
    suspend fun insertAbsences(absences: List<Absence>) = absenceDao.insertAbsences(absences)
    
    suspend fun updateAbsence(absence: Absence) = absenceDao.updateAbsence(absence)
    
    suspend fun deleteAbsence(absence: Absence) = absenceDao.deleteAbsence(absence)
    
    suspend fun deleteAllAbsences() = absenceDao.deleteAllAbsences()
} 