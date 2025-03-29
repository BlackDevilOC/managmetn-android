package com.substituemanagment.managment.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.substituemanagment.managment.data.converters.StringListConverter

@Entity(tableName = "teachers")
@TypeConverters(StringListConverter::class)
data class Teacher(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val phoneNumber: String?,
    val isSubstitute: Boolean,
    val variations: List<String> = emptyList()
) 