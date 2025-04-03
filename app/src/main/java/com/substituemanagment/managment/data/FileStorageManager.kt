package com.substituemanagment.managment.data

import android.content.Context
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FileStorageManager(private val context: Context) {
    private val baseDir = File(context.getExternalFilesDir(null), "substitute_data")
    private val rawDir = File(baseDir, "raw")
    private val backupDir = File(baseDir, "backups")

    init {
        // Create necessary directories
        listOf(baseDir, rawDir, backupDir).forEach { dir ->
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
    }

    fun saveFile(fileType: FileType, content: String): Result<String> {
        return try {
            // Check if file exists and create backup
            val existingFile = when (fileType) {
                FileType.TIMETABLE -> File(rawDir, "timetable.csv")
                FileType.SUBSTITUTE -> File(rawDir, "substitute.csv")
            }

            if (existingFile.exists()) {
                createBackup(fileType, existingFile)
            }

            // Save new file
            existingFile.writeText(content)
            Result.success("File uploaded successfully")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to save file: ${e.message}"))
        }
    }

    private fun createBackup(fileType: FileType, file: File) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        val backupFolder = File(backupDir, timestamp)
        if (!backupFolder.exists()) {
            backupFolder.mkdirs()
        }

        val backupFile = when (fileType) {
            FileType.TIMETABLE -> File(backupFolder, "timetable.csv")
            FileType.SUBSTITUTE -> File(backupFolder, "substitute.csv")
        }

        file.copyTo(backupFile, overwrite = true)
    }

    fun getFile(fileType: FileType): File? {
        val file = when (fileType) {
            FileType.TIMETABLE -> File(rawDir, "timetable.csv")
            FileType.SUBSTITUTE -> File(rawDir, "substitute.csv")
        }
        return if (file.exists()) file else null
    }
}

enum class FileType {
    TIMETABLE,
    SUBSTITUTE
} 