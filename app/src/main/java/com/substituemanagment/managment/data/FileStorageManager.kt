package com.substituemanagment.managment.data

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FileStorageManager(private val context: Context) {
    private val TAG = "FileStorageManager"
    private val baseDir = File(context.getExternalFilesDir(null), "substitute_data")
    private val rawDir = File(baseDir, "raw")
    private val backupDir = File(baseDir, "backups")

    init {
        // Create necessary directories
        listOf(baseDir, rawDir, backupDir).forEach { dir ->
            if (!dir.exists()) {
                val created = dir.mkdirs()
                Log.d(TAG, "Creating directory ${dir.absolutePath}: $created")
            }
        }
    }

    fun saveFile(uri: Uri, fileType: FileType): Result<String> {
        return try {
            Log.d(TAG, "Starting file save for type: $fileType")
            
            // Create standardized file names
            val targetFile = when (fileType) {
                FileType.TIMETABLE -> File(rawDir, "timetable.csv")
                FileType.SUBSTITUTE -> File(rawDir, "substitute.csv")
            }
            Log.d(TAG, "Target file path: ${targetFile.absolutePath}")

            // If file exists, create backup first
            if (targetFile.exists()) {
                Log.d(TAG, "Existing file found, creating backup")
                createBackup(fileType, targetFile)
            }

            // Read content from Uri and write to our file
            context.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    val bytes = input.copyTo(output)
                    Log.d(TAG, "File saved successfully. Bytes written: $bytes")
                }
            } ?: throw Exception("Could not read file content")

            val message = "File uploaded successfully to ${targetFile.absolutePath}"
            Log.d(TAG, message)
            Result.success(message)
        } catch (e: Exception) {
            val errorMsg = "Failed to save file: ${e.message}"
            Log.e(TAG, errorMsg, e)
            Result.failure(Exception(errorMsg))
        }
    }

    private fun createBackup(fileType: FileType, file: File) {
        try {
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
            val backupFolder = File(backupDir, timestamp)
            if (!backupFolder.exists()) {
                val created = backupFolder.mkdirs()
                Log.d(TAG, "Creating backup folder ${backupFolder.absolutePath}: $created")
            }

            val backupFile = when (fileType) {
                FileType.TIMETABLE -> File(backupFolder, "timetable.csv")
                FileType.SUBSTITUTE -> File(backupFolder, "substitute.csv")
            }

            file.copyTo(backupFile, overwrite = true)
            Log.d(TAG, "Backup created successfully at ${backupFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create backup", e)
            e.printStackTrace()
        }
    }

    fun getFile(fileType: FileType): File? {
        val file = when (fileType) {
            FileType.TIMETABLE -> File(rawDir, "timetable.csv")
            FileType.SUBSTITUTE -> File(rawDir, "substitute.csv")
        }
        val exists = file.exists()
        Log.d(TAG, "Checking file ${file.absolutePath}, exists: $exists")
        return if (exists) file else null
    }

    fun doesFileExist(fileType: FileType): Boolean {
        val file = when (fileType) {
            FileType.TIMETABLE -> File(rawDir, "timetable.csv")
            FileType.SUBSTITUTE -> File(rawDir, "substitute.csv")
        }
        val exists = file.exists()
        Log.d(TAG, "Checking if file exists ${file.absolutePath}: $exists")
        return exists
    }

    fun getStorageInfo(): String {
        return """
            Base Directory: ${baseDir.absolutePath}
            Raw Directory: ${rawDir.absolutePath}
            Backup Directory: ${backupDir.absolutePath}
            Base Directory exists: ${baseDir.exists()}
            Raw Directory exists: ${rawDir.exists()}
            Backup Directory exists: ${backupDir.exists()}
        """.trimIndent()
    }
}

enum class FileType {
    TIMETABLE,
    SUBSTITUTE
} 