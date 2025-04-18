package com.substituemanagment.managment.data

import android.content.Context
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AutoBackupManager private constructor(private val context: Context) {
    private val TAG = "AutoBackupManager"
    private val crashlytics = FirebaseCrashlytics.getInstance()
    private val baseDir = File("/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data")
    private val backupBaseDir = File(baseDir, "auto_backups")
    
    private val filesToBackup = listOf(
        "absent_teachers.json",
        "assigned_substitute.json",
        "sms_history.json",
        "teacher_details.json",
        "total_teacher.json"
    )
    
    init {
        // Set custom keys for better crash reporting
        crashlytics.setCustomKey("component", "AutoBackupManager")
    }
    
    suspend fun createDailyBackup() {
        try {
            withContext(Dispatchers.IO) {
                val calendar = Calendar.getInstance()
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                
                // Set backup context for crash reports
                crashlytics.setCustomKey("backup_year", year)
                crashlytics.setCustomKey("backup_month", month)
                crashlytics.setCustomKey("backup_date", date)
                
                // Create directory structure: year/month/date
                val yearDir = File(backupBaseDir, year.toString())
                val monthDir = File(yearDir, month)
                val dateDir = File(monthDir, date)
                
                // Create all directories
                dateDir.mkdirs()
                
                Log.d(TAG, "Creating backup in: ${dateDir.absolutePath}")
                crashlytics.log("Starting backup in: ${dateDir.absolutePath}")
                
                var successfulBackups = 0
                var failedBackups = 0
                
                // Backup each file
                filesToBackup.forEach { fileName ->
                    crashlytics.setCustomKey("current_file", fileName)
                    val sourceFile = File(baseDir, fileName)
                    if (sourceFile.exists()) {
                        try {
                            // Read source file
                            val content = sourceFile.readText()
                            
                            // Create backup file with timestamp
                            val timestamp = SimpleDateFormat("HH-mm", Locale.getDefault()).format(calendar.time)
                            val backupFile = File(dateDir, "${fileName.removeSuffix(".json")}_$timestamp.json")
                            
                            // Pretty print JSON before saving
                            val gson = GsonBuilder().setPrettyPrinting().create()
                            val jsonElement = Gson().fromJson(content, com.google.gson.JsonElement::class.java)
                            val prettyJson = gson.toJson(jsonElement)
                            
                            // Write to backup file
                            backupFile.writeText(prettyJson)
                            Log.d(TAG, "✅ Backed up $fileName to ${backupFile.name}")
                            successfulBackups++
                        } catch (e: Exception) {
                            val errorMsg = "❌ Failed to backup $fileName: ${e.message}"
                            Log.e(TAG, errorMsg)
                            crashlytics.recordException(e)
                            crashlytics.log(errorMsg)
                            failedBackups++
                        }
                    } else {
                        val warningMsg = "⚠️ Source file $fileName not found"
                        Log.w(TAG, warningMsg)
                        crashlytics.log(warningMsg)
                        failedBackups++
                    }
                }
                
                // Backup processed directory
                crashlytics.setCustomKey("backup_stage", "processed_directory")
                val processedDir = File(baseDir, "processed")
                if (processedDir.exists()) {
                    val backupProcessedDir = File(dateDir, "processed")
                    backupProcessedDir.mkdirs()
                    
                    processedDir.listFiles()?.forEach { file ->
                        crashlytics.setCustomKey("current_processed_file", file.name)
                        try {
                            if (file.isFile) {
                                val content = file.readText()
                                val backupFile = File(backupProcessedDir, file.name)
                                
                                if (file.extension.equals("json", ignoreCase = true)) {
                                    val gson = GsonBuilder().setPrettyPrinting().create()
                                    val jsonElement = Gson().fromJson(content, com.google.gson.JsonElement::class.java)
                                    backupFile.writeText(gson.toJson(jsonElement))
                                } else {
                                    backupFile.writeText(content)
                                }
                                Log.d(TAG, "✅ Backed up processed/${file.name}")
                                successfulBackups++
                            }
                        } catch (e: Exception) {
                            val errorMsg = "❌ Failed to backup processed/${file.name}: ${e.message}"
                            Log.e(TAG, errorMsg)
                            crashlytics.recordException(e)
                            crashlytics.log(errorMsg)
                            failedBackups++
                        }
                    }
                }
                
                // Create backup summary
                val summary = mapOf(
                    "backup_time" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(calendar.time),
                    "backup_location" to dateDir.absolutePath,
                    "backed_up_files" to filesToBackup,
                    "processed_files_backed_up" to (processedDir.listFiles()?.size ?: 0),
                    "successful_backups" to successfulBackups,
                    "failed_backups" to failedBackups
                )
                
                crashlytics.setCustomKey("successful_backups", successfulBackups)
                crashlytics.setCustomKey("failed_backups", failedBackups)
                
                val summaryFile = File(dateDir, "backup_summary.json")
                val gson = GsonBuilder().setPrettyPrinting().create()
                summaryFile.writeText(gson.toJson(summary))
                
                val completionMsg = """
                    ✅ Daily backup completed!
                    📂 Location: ${dateDir.absolutePath}
                    📊 Stats:
                    - Successful backups: $successfulBackups
                    - Failed backups: $failedBackups
                    - Total files: ${successfulBackups + failedBackups}
                """.trimIndent()
                
                Log.d(TAG, completionMsg)
                crashlytics.log(completionMsg)
            }
        } catch (e: Exception) {
            val errorMsg = "❌ Critical error during daily backup: ${e.message}"
            Log.e(TAG, errorMsg)
            crashlytics.recordException(e)
            crashlytics.log(errorMsg)
            e.printStackTrace()
            throw e // Re-throw to notify caller of failure
        }
    }
    
    companion object {
        @Volatile private var instance: AutoBackupManager? = null
        
        fun getInstance(context: Context): AutoBackupManager {
            return instance ?: synchronized(this) {
                instance ?: AutoBackupManager(context.applicationContext).also { instance = it }
            }
        }
    }
} 