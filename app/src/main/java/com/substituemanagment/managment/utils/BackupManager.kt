package com.substituemanagment.managment.utils

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.substituemanagment.managment.worker.BackupWorker
import java.util.concurrent.TimeUnit

class BackupManager(private val context: Context) {
    private val TAG = "BackupManager"
    private val BACKUP_WORK_NAME = "substitute_manager_backup"
    
    fun scheduleBackup() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(
                12, // Every 12 hours
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    BACKUP_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    backupRequest
                )
            
            Log.d(TAG, "Backup scheduled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling backup", e)
        }
    }
    
    fun cancelBackup() {
        try {
            WorkManager.getInstance(context)
                .cancelUniqueWork(BACKUP_WORK_NAME)
            Log.d(TAG, "Backup cancelled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling backup", e)
        }
    }
    
    fun checkAndRestoreData() {
        try {
            val sharedPrefs = context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
            val isFirstInstall = sharedPrefs.getBoolean("is_first_install", true)
            
            if (isFirstInstall) {
                // This is a fresh install, try to restore from backup
                val backupService = DriveBackupService(context)
                backupService.initialize()
                backupService.restoreData()
                
                // Mark as not first install
                sharedPrefs.edit().putBoolean("is_first_install", false).apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking/restoring data", e)
        }
    }
} 