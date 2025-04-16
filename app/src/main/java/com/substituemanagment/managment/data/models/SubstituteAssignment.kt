package com.substituemanagment.managment.data.models

data class SubstituteAssignment(
    val id: String,
    val date: String,
    val absentTeacherId: String,
    val substituteTeacherId: String,
    val className: String,
    val period: Int,
    val subject: String
) 