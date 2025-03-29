package com.substituemanagment.managment.data.database

import androidx.room.*
import com.substituemanagment.managment.data.models.Absence
import com.substituemanagment.managment.data.models.AbsenceStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AbsenceDao {
    @Query("SELECT * FROM absences")
    fun getAllAbsences(): Flow<List<Absence>>

    @Query("SELECT * FROM absences WHERE date = :date")
    fun getAbsencesByDate(date: String): Flow<List<Absence>>

    @Query("SELECT * FROM absences WHERE teacherId = :teacherId")
    fun getAbsencesByTeacher(teacherId: Int): Flow<List<Absence>>

    @Query("SELECT * FROM absences WHERE status = :status")
    fun getAbsencesByStatus(status: AbsenceStatus): Flow<List<Absence>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbsence(absence: Absence)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbsences(absences: List<Absence>)

    @Update
    suspend fun updateAbsence(absence: Absence)

    @Delete
    suspend fun deleteAbsence(absence: Absence)

    @Query("DELETE FROM absences")
    suspend fun deleteAllAbsences()
} 