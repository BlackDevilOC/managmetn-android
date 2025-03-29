package com.substituemanagment.managment.data.converters

import androidx.room.TypeConverter
import com.substituemanagment.managment.data.models.AbsenceStatus

class AbsenceStatusConverter {
    @TypeConverter
    fun fromAbsenceStatus(value: AbsenceStatus): String {
        return value.name
    }

    @TypeConverter
    fun toAbsenceStatus(value: String): AbsenceStatus {
        return AbsenceStatus.valueOf(value)
    }
} 