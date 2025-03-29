package com.substituemanagment.managment.data.utils

import android.content.Context
import com.substituemanagment.managment.data.models.Schedule
import com.substituemanagment.managment.data.models.Teacher
import java.io.BufferedReader
import java.io.InputStreamReader

class CsvProcessor(private val context: Context) {
    fun processTimetableFile(fileName: String): List<Schedule> {
        val schedules = mutableListOf<Schedule>()
        try {
            context.assets.open(fileName).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    // Skip header row
                    reader.readLine()
                    
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val columns = line!!.split(",")
                        if (columns.size >= 4) {
                            val day = columns[0].trim()
                            val period = columns[1].trim().toIntOrNull() ?: continue
                            val teacherName = columns[2].trim()
                            val className = columns[3].trim()
                            
                            schedules.add(
                                Schedule(
                                    day = day,
                                    period = period,
                                    teacherId = 0, // Will be updated after teacher processing
                                    className = className
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return schedules
    }

    fun processSubstituteFile(fileName: String): List<Teacher> {
        val teachers = mutableListOf<Teacher>()
        try {
            context.assets.open(fileName).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    // Skip header row
                    reader.readLine()
                    
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val columns = line!!.split(",")
                        if (columns.size >= 2) {
                            val name = columns[0].trim()
                            val phoneNumber = columns[1].trim().takeIf { it.isNotEmpty() }
                            val isSubstitute = columns.getOrNull(2)?.trim()?.toBoolean() ?: false
                            
                            teachers.add(
                                Teacher(
                                    name = name,
                                    phoneNumber = phoneNumber,
                                    isSubstitute = isSubstitute,
                                    variations = generateNameVariations(name)
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return teachers
    }

    private fun generateNameVariations(name: String): List<String> {
        val variations = mutableListOf<String>()
        val parts = name.split(" ")
        
        // Add full name
        variations.add(name)
        
        // Add name without titles
        val nameWithoutTitle = name.replace(Regex("^(Mr|Mrs|Ms|Dr|Sir|Madam)\\s+"), "")
        if (nameWithoutTitle != name) {
            variations.add(nameWithoutTitle)
        }
        
        // Add initials
        if (parts.size > 1) {
            val initials = parts.map { it.firstOrNull()?.toString() ?: "" }.joinToString(" ")
            variations.add(initials)
        }
        
        // Add last name only
        variations.add(parts.last())
        
        return variations.distinct()
    }
} 