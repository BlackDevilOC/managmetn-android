package com.substituemanagment.managment.data.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.substituemanagment.managment.MainActivity
import com.substituemanagment.managment.R
import com.substituemanagment.managment.data.utils.ReportManager
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit

/**
 * Service that manages the automatic generation of reports and backups
 * at application start, end, and at regular intervals while the app is running.
 */
class ReportingService : Service() {
    private val TAG = "ReportingService"
    private lateinit var reportManager: ReportManager
    private lateinit var sharedPreferences: SharedPreferences
    
    // Keys for shared preferences
    private val PREF_NAME = "ReportingServicePrefs"
    private val KEY_LAST_REPORT_TIME = "last_report_time"
    private val KEY_REPORT_ERROR_COUNT = "report_error_count"
    private val KEY_LAST_ERROR_TIME = "last_error_time"
    
    // Notification constants
    private val NOTIFICATION_CHANNEL_ID = "backup_channel"
    private val ONGOING_NOTIFICATION_ID = 1
    private val ERROR_NOTIFICATION_ID = 2
    
    // Background timer for periodic checks
    private var timer: Timer? = null
    
    // Error tracking
    private var consecutiveErrorCount = 0
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ReportingService created")
        
        reportManager = ReportManager(applicationContext)
        sharedPreferences = applicationContext.getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        
        // Load previous error count if any
        consecutiveErrorCount = sharedPreferences.getInt(KEY_REPORT_ERROR_COUNT, 0)
        
        // Create notification channel for Android 8.0+
        createNotificationChannel()
        
        // Start as a foreground service to prevent it from being killed
        startForeground(ONGOING_NOTIFICATION_ID, createStatusNotification("Backup service is running"))
        
        // Generate report on service creation (app start)
        generateReportIfNeeded(true)
        
        // Start a timer to periodically check if reports need to be generated
        startPeriodicCheck()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "ReportingService started with action: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_APP_START -> {
                // App started, start the reporting service
                reportManager.startReportService()
                Log.d(TAG, "Report service started on app start")
            }
            
            ACTION_APP_END -> {
                // App is ending, generate final report and stop
                try {
                    reportManager.generateReports()
                    resetErrorCount() // Reset error count on successful generation
                    Log.d(TAG, "Generated final report on app end")
                } catch (e: Exception) {
                    Log.e(TAG, "Error generating final report", e)
                    incrementErrorCount()
                }
                
                reportManager.stopReportService()
                
                // Stop the service after generating the report
                stopPeriodicCheck()
                stopForeground(true)
                stopSelf()
            }
            
            ACTION_CHECK_REPORTS -> {
                // Check if we need to generate a report (periodic check)
                generateReportIfNeeded(false)
            }
            
            ACTION_FORCE_REPORT -> {
                // Force report generation regardless of time
                try {
                    reportManager.generateReports()
                    resetErrorCount() // Reset error count on successful generation
                    
                    // Show success toast
                    showToast("Reports generated successfully")
                    Log.d(TAG, "Force-generated reports successfully")
                    
                    // Update the last report time
                    updateLastReportTime()
                } catch (e: Exception) {
                    Log.e(TAG, "Error force-generating reports", e)
                    incrementErrorCount()
                    
                    // Show error toast
                    showToast("Failed to generate reports. Check error logs.")
                }
            }
        }
        
        // If we're killed, restart with the last intent
        return START_REDELIVER_INTENT
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null // We don't provide binding
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Stop the report service when the service is destroyed
        reportManager.stopReportService()
        stopPeriodicCheck()
        Log.d(TAG, "ReportingService destroyed")
    }
    
    /**
     * Start periodic checking for report generation
     */
    private fun startPeriodicCheck() {
        Log.d(TAG, "Starting periodic report checks")
        stopPeriodicCheck() // Ensure we don't have multiple timers
        
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val intent = Intent(applicationContext, ReportingService::class.java).apply {
                    action = ACTION_CHECK_REPORTS
                }
                startService(intent)
            }
        }, PERIODIC_CHECK_INTERVAL_MS, PERIODIC_CHECK_INTERVAL_MS)
    }
    
    /**
     * Stop periodic checking
     */
    private fun stopPeriodicCheck() {
        timer?.cancel()
        timer = null
    }
    
    /**
     * Create the notification channel for Android 8.0+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Backup Service"
            val descriptionText = "Notifications about backup and reporting status"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Create a status notification for the foreground service
     */
    private fun createStatusNotification(message: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, 
            Intent(this, MainActivity::class.java), 
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Substitute Manager Backup")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    /**
     * Show an error notification to the user
     */
    private fun showErrorNotification(message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Backup Error")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(ERROR_NOTIFICATION_ID, notification)
    }
    
    /**
     * Show a toast message (only if we're in the foreground)
     */
    private fun showToast(message: String) {
        try {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing toast", e)
        }
    }
    
    /**
     * Generate a report if enough time has passed since the last one
     * @param force If true, generate a report regardless of the time elapsed
     */
    private fun generateReportIfNeeded(force: Boolean) {
        val lastReportTime = sharedPreferences.getLong(KEY_LAST_REPORT_TIME, 0)
        val now = Instant.now().toEpochMilli()
        
        val minutesSinceLastReport = if (lastReportTime > 0) {
            ChronoUnit.MINUTES.between(
                Instant.ofEpochMilli(lastReportTime), 
                Instant.ofEpochMilli(now)
            )
        } else {
            Long.MAX_VALUE // If never generated, consider it as maximum time
        }
        
        // Generate report if forced or if it's been more than the report interval
        if (force || minutesSinceLastReport >= REPORT_INTERVAL_MINUTES) {
            Log.d(TAG, "Generating report (force=$force, minutesSince=$minutesSinceLastReport)")
            
            try {
                reportManager.generateReports()
                
                // Update the last report time on success
                updateLastReportTime()
                
                // Reset error count on successful generation
                resetErrorCount()
                
                // Update foreground notification
                val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                val notification = createStatusNotification(
                    "Reports generated successfully at $timestamp"
                )
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(ONGOING_NOTIFICATION_ID, notification)
                
                Log.d(TAG, "Reports generated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error generating reports", e)
                incrementErrorCount()
                
                // If we have multiple consecutive errors, show a notification
                if (consecutiveErrorCount >= MAX_ERRORS_BEFORE_NOTIFY) {
                    showErrorNotification("Failed to generate reports after $consecutiveErrorCount attempts")
                }
            }
        } else {
            Log.d(TAG, "Skipping report generation, only $minutesSinceLastReport minutes since last report")
        }
    }
    
    /**
     * Update the timestamp of the last report generation
     */
    private fun updateLastReportTime() {
        val now = Instant.now().toEpochMilli()
        sharedPreferences.edit().putLong(KEY_LAST_REPORT_TIME, now).apply()
    }
    
    /**
     * Increment the error count and save it
     */
    private fun incrementErrorCount() {
        consecutiveErrorCount++
        val now = Instant.now().toEpochMilli()
        
        sharedPreferences.edit()
            .putInt(KEY_REPORT_ERROR_COUNT, consecutiveErrorCount)
            .putLong(KEY_LAST_ERROR_TIME, now)
            .apply()
        
        Log.w(TAG, "Report generation error count: $consecutiveErrorCount")
    }
    
    /**
     * Reset the error count
     */
    private fun resetErrorCount() {
        if (consecutiveErrorCount > 0) {
            consecutiveErrorCount = 0
            sharedPreferences.edit()
                .putInt(KEY_REPORT_ERROR_COUNT, 0)
                .apply()
            
            Log.d(TAG, "Reset error count")
        }
    }
    
    /**
     * Notify error with permission check
     */
    private fun notifyError(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Request the permission
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE_POST_NOTIFICATIONS)
            } else {
                // Permission already granted, proceed with notification
                notificationManager.notify(ERROR_NOTIFICATION_ID, notification)
            }
        } else {
            // For devices below Android 13, no need to request permission
            notificationManager.notify(ERROR_NOTIFICATION_ID, notification)
        }
    }
    
    companion object {
        // Actions for intent
        const val ACTION_APP_START = "com.substituemanagment.managment.ACTION_APP_START"
        const val ACTION_APP_END = "com.substituemanagment.managment.ACTION_APP_END"
        const val ACTION_CHECK_REPORTS = "com.substituemanagment.managment.ACTION_CHECK_REPORTS"
        const val ACTION_FORCE_REPORT = "com.substituemanagment.managment.ACTION_FORCE_REPORT"
        
        // How often to generate reports (in minutes)
        const val REPORT_INTERVAL_MINUTES = 30L
        
        // How often to check if reports need to be generated (in milliseconds)
        private val PERIODIC_CHECK_INTERVAL_MS = TimeUnit.MINUTES.toMillis(15)
        
        // How many consecutive errors before we notify the user
        private const val MAX_ERRORS_BEFORE_NOTIFY = 3
        
        // Request code for POST_NOTIFICATIONS permission
        private const val REQUEST_CODE_POST_NOTIFICATIONS = 1234
    }
}
