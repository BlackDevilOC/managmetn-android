package com.substituemanagment.managment.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.substituemanagment.managment.data.DriveBackupService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val TAG = "BackupWorker"
    
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val backupService = DriveBackupService(context)
                backupService.initialize()
                backupService.backupData()
                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Error performing backup", e)
                Result.failure()
            }
        }
    }
} 