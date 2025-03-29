package com.substituemanagment.managment.di

import android.content.Context
import com.substituemanagment.managment.data.AppDatabase
import com.substituemanagment.managment.data.repository.AbsenceRepository
import com.substituemanagment.managment.data.repository.ScheduleRepository
import com.substituemanagment.managment.data.repository.TeacherRepository
import com.substituemanagment.managment.utils.CsvProcessor

object ServiceLocator {
    private var database: AppDatabase? = null
    private var teacherRepository: TeacherRepository? = null
    private var scheduleRepository: ScheduleRepository? = null
    private var absenceRepository: AbsenceRepository? = null
    private var csvProcessor: CsvProcessor? = null

    fun initialize(context: Context) {
        database = AppDatabase.getDatabase(context)
        teacherRepository = TeacherRepository(database!!.teacherDao())
        scheduleRepository = ScheduleRepository(database!!.scheduleDao())
        absenceRepository = AbsenceRepository(database!!.absenceDao())
        csvProcessor = CsvProcessor(context)
    }

    fun getTeacherRepository(): TeacherRepository {
        return teacherRepository ?: throw IllegalStateException("ServiceLocator not initialized")
    }

    fun getScheduleRepository(): ScheduleRepository {
        return scheduleRepository ?: throw IllegalStateException("ServiceLocator not initialized")
    }

    fun getAbsenceRepository(): AbsenceRepository {
        return absenceRepository ?: throw IllegalStateException("ServiceLocator not initialized")
    }

    fun getCsvProcessor(): CsvProcessor {
        return csvProcessor ?: throw IllegalStateException("ServiceLocator not initialized")
    }
} 