package com.substituemanagment.managment.data

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AutoBackupManager private constructor(private val context: Context) {
    private val TAG = "AutoBackupManager"
    private val baseDir = File("/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data")
    private val backupBaseDir = File(baseDir, "auto_backups")
    
    private val filesToBackup = listOf(
        "absent_teachers.json",
        "assigned_substitute.json",
        "sms_history.json",
        "teacher_details.json",
        "total_teacher.json"
    )
    
    suspend fun createDailyBackup() {
        try {
            withContext(Dispatchers.IO) {
                val calendar = Calendar.getInstance()
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                
                // Create directory structure: year/month/date
                val yearDir = File(backupBaseDir, year.toString())
                val monthDir = File(yearDir, month)
                val dateDir = File(monthDir, date)
                
                // Create all directories
                dateDir.mkdirs()
                
                Log.d(TAG, "Creating backup in: ${dateDir.absolutePath}")
                
                // Backup each file
                filesToBackup.forEach { fileName ->
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
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Failed to backup $fileName: ${e.message}")
                        }
                    } else {
                        Log.w(TAG, "⚠️ Source file $fileName not found")
                    }
                }
                
                // Backup processed directory
                val processedDir = File(baseDir, "processed")
                if (processedDir.exists()) {
                    val backupProcessedDir = File(dateDir, "processed")
                    backupProcessedDir.mkdirs()
                    
                    processedDir.listFiles()?.forEach { file ->
                        try {
                            if (file.isFile) {
                                val content = file.readText()
                                val backupFile = File(backupProcessedDir, file.name)
                                
                                // Pretty print JSON if it's a JSON file
                                if (file.extension.equals("json", ignoreCase = true)) {
                                    val gson = GsonBuilder().setPrettyPrinting().create()
                                    val jsonElement = Gson().fromJson(content, com.google.gson.JsonElement::class.java)
                                    backupFile.writeText(gson.toJson(jsonElement))
                                } else {
                                    backupFile.writeText(content)
                                }
                                Log.d(TAG, "✅ Backed up processed/${file.name}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Failed to backup processed/${file.name}: ${e.message}")
                        }
                    }
                }
                
                // Create backup summary
                val summary = mapOf(
                    "backup_time" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(calendar.time),
                    "backup_location" to dateDir.absolutePath,
                    "backed_up_files" to filesToBackup,
                    "processed_files_backed_up" to (processedDir.listFiles()?.size ?: 0)
                )
                
                val summaryFile = File(dateDir, "backup_summary.json")
                val gson = GsonBuilder().setPrettyPrinting().create()
                summaryFile.writeText(gson.toJson(summary))
                
                Log.d(TAG, """
                    ✅ Daily backup completed successfully!
                    📂 Location: ${dateDir.absolutePath}
                    📊 Total files backed up: ${filesToBackup.size + (processedDir.listFiles()?.size ?: 0)}
                """.trimIndent())
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during daily backup: ${e.message}")
            e.printStackTrace()
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