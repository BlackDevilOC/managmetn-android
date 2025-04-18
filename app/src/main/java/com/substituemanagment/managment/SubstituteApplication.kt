package com.substituemanagment.managment

import android.app.Application
import android.util.Log
import com.substituemanagment.managment.utils.BackupManager

class SubstituteApplication : Application() {
    private val TAG = "SubstituteApplication"
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            // Initialize backup system
            val backupManager = BackupManager(this)
            
            // Schedule periodic backups
            backupManager.scheduleBackup()
            
            // Check if this is a fresh install and restore data if needed
            backupManager.checkAndRestoreData()
            
            Log.d(TAG, "Application initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing application", e)
        }
    }
} 