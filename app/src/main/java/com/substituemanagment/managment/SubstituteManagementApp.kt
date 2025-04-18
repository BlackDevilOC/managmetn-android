package com.substituemanagment.managment

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlin.system.exitProcess

class SubstituteManagementApp : Application() {
    private val TAG = "SubstituteManagementApp"
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Enable Crashlytics
        FirebaseCrashlytics.getInstance().apply {
            setCrashlyticsCollectionEnabled(true)
            // Set some default keys that might help in debugging
            setCustomKey("app_version_name", BuildConfig.VERSION_NAME)
            setCustomKey("app_version_code", BuildConfig.VERSION_CODE)
            setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            log("Application started")
        }
        
        // Set up global exception handler
        setupGlobalExceptionHandler()
    }
    
    private fun setupGlobalExceptionHandler() {
        val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // Log the exception
                Log.e(TAG, "Uncaught exception in thread ${thread.name}", throwable)
                
                // Send to Crashlytics with additional context
                FirebaseCrashlytics.getInstance().apply {
                    setCustomKey("crash_thread", thread.name)
                    setCustomKey("crash_thread_id", thread.id)
                    setCustomKey("crash_time", System.currentTimeMillis())
                    
                    // Log the exception
                    recordException(throwable)
                    
                    // Force send the crash report immediately
                    sendUnsentReports()
                }
                
                // Wait a bit to ensure the crash report is sent
                Thread.sleep(1000)
            } catch (e: Exception) {
                Log.e(TAG, "Error in exception handler", e)
            } finally {
                // Call the default handler
                defaultExceptionHandler?.uncaughtException(thread, throwable) ?: run {
                    // If no default handler, terminate the process
                    exitProcess(1)
                }
            }
        }
    }
} 