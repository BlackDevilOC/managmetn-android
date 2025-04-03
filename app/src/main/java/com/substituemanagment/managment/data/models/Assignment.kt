package com.substituemanagment.managment.data.models

data class Assignment(
    val id: String,
    val className: String,
    val period: Int,
    val day: String,
    val teacherId: String,
    val subject: String
) 