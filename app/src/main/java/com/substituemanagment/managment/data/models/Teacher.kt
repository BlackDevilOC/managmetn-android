package com.substituemanagment.managment.data.models

data class Teacher(
    val id: String,
    val name: String,
    val isSubstitute: Boolean = false,
    val subjects: List<String> = emptyList(),
    val maxAssignments: Int = if (isSubstitute) 3 else 2
) 