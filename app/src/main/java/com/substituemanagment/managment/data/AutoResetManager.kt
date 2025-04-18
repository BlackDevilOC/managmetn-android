package com.substituemanagment.managment.data

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.GsonBuilder
import com.substituemanagment.managment.algorithm.PROCESSED_ASSIGNED_SUBSTITUTES_PATH
import com.substituemanagment.managment.algorithm.SubstituteManager
import com.substituemanagment.managment.ui.viewmodels.TeachersViewModel
import kotlinx.coroutines.*
import java.io.File
import java.util.*

class AutoResetManager private constructor(private val context: Context) {
    private val TAG = "AutoResetManager"
    private val PREFS_NAME = "auto_reset_settings"
    private val KEY_AUTO_RESET = "auto_reset_enabled"
    private val KEY_RESET_HOUR = "reset_hour"
    private val KEY_RESET_MINUTE = "reset_minute"
    private val KEY_RESET_IS_AM = "reset_is_am"
    
    private var autoResetEnabled = false
    private var resetHour = 7  // Default 7 AM
    private var resetMinute = 45  // Default 45 minutes
    private var resetIsAM = true  // Default to AM
    private var resetJob: Job? = null
    
    private val teachersViewModel = TeachersViewModel()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val autoBackupManager = AutoBackupManager.getInstance(context)
    
    init {
        loadSettings()
        teachersViewModel.initialize(context)
        startMonitoring()
    }
    
    private fun loadSettings() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        autoResetEnabled = prefs.getBoolean(KEY_AUTO_RESET, false)
        resetHour = prefs.getInt(KEY_RESET_HOUR, 7)
        resetMinute = prefs.getInt(KEY_RESET_MINUTE, 45)
        resetIsAM = prefs.getBoolean(KEY_RESET_IS_AM, true)
        Log.d(TAG, "Settings loaded - Enabled: $autoResetEnabled, Time: $resetHour:$resetMinute ${if (resetIsAM) "AM" else "PM"}")
    }
    
    private fun saveSettings() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(KEY_AUTO_RESET, autoResetEnabled)
            putInt(KEY_RESET_HOUR, resetHour)
            putInt(KEY_RESET_MINUTE, resetMinute)
            putBoolean(KEY_RESET_IS_AM, resetIsAM)
            apply()
        }
        Log.d(TAG, "Settings saved - Enabled: $autoResetEnabled, Time: $resetHour:$resetMinute ${if (resetIsAM) "AM" else "PM"}")
    }
    
    fun setAutoResetEnabled(enabled: Boolean) {
        autoResetEnabled = enabled
        saveSettings()
        if (enabled) {
            startMonitoring()
        } else {
            stopMonitoring()
        }
    }
    
    fun setResetTime(hour: Int, minute: Int, isAM: Boolean) {
        resetHour = hour
        resetMinute = minute
        resetIsAM = isAM
        saveSettings()
        Log.d(TAG, "New reset time set: $hour:$minute ${if (isAM) "AM" else "PM"}")
    }
    
    private fun startMonitoring() {
        stopMonitoring() // Stop any existing monitoring
        
        resetJob = coroutineScope.launch {
            while (isActive) {
                if (autoResetEnabled) {
                    checkAndReset()
                }
                delay(15000) // Check every 15 seconds
            }
        }
        Log.d(TAG, "Auto-reset monitoring started")
    }
    
    private fun stopMonitoring() {
        resetJob?.cancel()
        resetJob = null
        Log.d(TAG, "Auto-reset monitoring stopped")
    }
    
    private suspend fun checkAndReset() {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentIsAM = calendar.get(Calendar.AM_PM) == Calendar.AM
        
        // Convert 12 to 0 for comparison if needed
        val compareCurrentHour = if (currentHour == 12) 0 else currentHour
        val compareResetHour = if (resetHour == 12) 0 else resetHour
        
        Log.d(TAG, """
            🕒 Auto-reset check:
            Current time: $currentHour:$currentMinute ${if (currentIsAM) "AM" else "PM"}
            Target time: $resetHour:$resetMinute ${if (resetIsAM) "AM" else "PM"}
            Auto-reset enabled: $autoResetEnabled
            Hour match: ${compareCurrentHour == compareResetHour}
            Minute match: ${currentMinute == resetMinute}
            AM/PM match: ${currentIsAM == resetIsAM}
        """.trimIndent())
        
        if (compareCurrentHour == compareResetHour && 
            currentMinute == resetMinute && 
            currentIsAM == resetIsAM) {
            Log.d(TAG, "🔄 Auto-reset time match detected! Starting reset process...")
            performReset()
        }
    }
    
    private suspend fun performReset() {
        try {
            withContext(Dispatchers.IO) {
                // Step 1: Create backup first
                Log.d(TAG, "📦 Step 1: Creating backup before reset...")
                try {
                    autoBackupManager.createDailyBackup()
                    Log.d(TAG, "✅ Backup completed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Backup failed, aborting reset for safety: ${e.message}")
                    e.printStackTrace()
                    return@withContext
                }

                // Step 2: Only proceed with reset if backup was successful
                Log.d(TAG, "🔄 Step 2: Starting reset process...")
                
                try {
                    // Reset attendance first
                    Log.d(TAG, "2.1: Starting attendance reset...")
                    teachersViewModel.resetAttendanceAndAssignments(context)
                    Log.d(TAG, "✅ Completed attendance reset")
                    
                    // Then reset substitutions
                    Log.d(TAG, "2.2: Starting substitutions reset...")
                    val substituteManager = SubstituteManager(context)
                    substituteManager.loadData()
                    substituteManager.clearAssignments()
                    Log.d(TAG, "✅ Completed substitutions manager reset")
                    
                    // Finally clear assignments file
                    Log.d(TAG, "2.3: Writing empty assignments file...")
                    val file = File(PROCESSED_ASSIGNED_SUBSTITUTES_PATH)
                    val emptyAssignments = mapOf(
                        "assignments" to emptyList<Any>(),
                        "unassignedClasses" to emptyList<Any>(),
                        "warnings" to emptyList<String>()
                    )
                    val gson = GsonBuilder().setPrettyPrinting().create()
                    
                    file.parentFile?.mkdirs()
                    file.writeText(gson.toJson(emptyAssignments))
                    Log.d(TAG, "✅ Successfully cleared assignments file")
                    
                    Log.d(TAG, """
                        ✅ Auto-reset completed successfully!
                        📝 Summary:
                        - Backup created
                        - Attendance reset
                        - Substitutions cleared
                        - Assignment file cleared
                    """.trimIndent())
                } catch (e: Exception) {
                    Log.e(TAG, """
                        ❌ Error during reset operations: ${e.message}
                        ℹ️ Note: Backup was created successfully before error occurred
                    """.trimIndent())
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Critical error in performReset: ${e.message}")
            e.printStackTrace()
        }
    }
    
    companion object {
        @Volatile private var instance: AutoResetManager? = null
        
        fun getInstance(context: Context): AutoResetManager {
            return instance ?: synchronized(this) {
                instance ?: AutoResetManager(context.applicationContext).also { instance = it }
            }
        }
    }
} 