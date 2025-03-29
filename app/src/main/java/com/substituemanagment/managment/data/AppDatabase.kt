package com.substituemanagment.managment.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.substituemanagment.managment.data.converters.AbsenceStatusConverter
import com.substituemanagment.managment.data.converters.StringListConverter
import com.substituemanagment.managment.data.dao.AbsenceDao
import com.substituemanagment.managment.data.dao.ScheduleDao
import com.substituemanagment.managment.data.dao.TeacherDao
import com.substituemanagment.managment.data.models.Absence
import com.substituemanagment.managment.data.models.Schedule
import com.substituemanagment.managment.data.models.Teacher

@Database(
    entities = [Teacher::class, Schedule::class, Absence::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(StringListConverter::class, AbsenceStatusConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun teacherDao(): TeacherDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun absenceDao(): AbsenceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "substitution_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 