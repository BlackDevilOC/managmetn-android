package com.substituemanagment.managment.di

import android.content.Context
import com.substituemanagment.managment.data.database.AppDatabase
import com.substituemanagment.managment.data.database.AbsenceDao
import com.substituemanagment.managment.data.database.ScheduleDao
import com.substituemanagment.managment.data.database.TeacherDao
import com.substituemanagment.managment.data.repository.AbsenceRepository
import com.substituemanagment.managment.data.repository.ScheduleRepository
import com.substituemanagment.managment.data.repository.TeacherRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideTeacherDao(database: AppDatabase) = database.teacherDao()

    @Provides
    @Singleton
    fun provideScheduleDao(database: AppDatabase) = database.scheduleDao()

    @Provides
    @Singleton
    fun provideAbsenceDao(database: AppDatabase) = database.absenceDao()

    @Provides
    @Singleton
    fun provideTeacherRepository(teacherDao: TeacherDao) = TeacherRepository(teacherDao)

    @Provides
    @Singleton
    fun provideScheduleRepository(scheduleDao: ScheduleDao) = ScheduleRepository(scheduleDao)

    @Provides
    @Singleton
    fun provideAbsenceRepository(absenceDao: AbsenceDao) = AbsenceRepository(absenceDao)
} 