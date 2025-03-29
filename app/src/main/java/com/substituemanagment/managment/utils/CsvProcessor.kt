package com.substituemanagment.managment.utils

import android.content.Context
import com.opencsv.CSVReader
import com.opencsv.exceptions.CsvValidationException
import com.substituemanagment.managment.data.models.Schedule
import com.substituemanagment.managment.data.models.Teacher
import java.io.BufferedReader
import java.io.InputStreamReader

class CsvProcessor(private val context: Context) {
    suspend fun processTimetableFile(fileName: String): List<Schedule> {
        val schedules = mutableListOf<Schedule>()
        try {
            val inputStream = context.assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val csvReader = CSVReader(reader)
            
            // Skip header row
            csvReader.readNext()
            
            var record: Array<String>?
            while (csvReader.readNext().also { record = it } != null) {
                record?.let {
                    if (it.size >= 4) {
                        val schedule = Schedule(
                            day = it[0].trim(),
                            period = it[1].toIntOrNull() ?: 0,
                            teacherId = 0, // Will be updated after teacher processing
                            className = it[3].trim()
                        )
                        schedules.add(schedule)
                    }
                }
            }
            
            csvReader.close()
            reader.close()
            inputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return schedules
    }

    suspend fun processSubstituteFile(fileName: String): List<Teacher> {
        val teachers = mutableListOf<Teacher>()
        try {
            val inputStream = context.assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val csvReader = CSVReader(reader)
            
            // Skip header row
            csvReader.readNext()
            
            var record: Array<String>?
            while (csvReader.readNext().also { record = it } != null) {
                record?.let {
                    if (it.size >= 2) {
                        val teacher = Teacher(
                            name = it[0].trim(),
                            phoneNumber = if (it.size > 1) it[1].trim() else null,
                            isSubstitute = true,
                            variations = generateNameVariations(it[0].trim())
                        )
                        teachers.add(teacher)
                    }
                }
            }
            
            csvReader.close()
            reader.close()
            inputStream.close()
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
        if (parts.size > 1) {
            variations.add(parts.last())
        }
        
        return variations
    }
} 