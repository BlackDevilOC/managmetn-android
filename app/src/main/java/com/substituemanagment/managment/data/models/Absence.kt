package com.substituemanagment.managment.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

enum class AbsenceStatus {
    PENDING, APPROVED, COMPLETED
}

@Entity(
    tableName = "absences",
    foreignKeys = [
        ForeignKey(
            entity = Teacher::class,
            parentColumns = ["id"],
            childColumns = ["teacherId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Teacher::class,
            parentColumns = ["id"],
            childColumns = ["substituteId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Absence(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val teacherId: Int,
    val date: String,
    val substituteId: Int?,
    val reason: String?,
    val status: AbsenceStatus
) 