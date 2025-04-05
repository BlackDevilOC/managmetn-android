package com.substituemanagment.managment.utils

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Utility class to check if required files exist on the device
 */
object FileChecker {
    private const val TAG = "FileChecker"
    private const val EXTERNAL_STORAGE_BASE_PATH = "/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data"
    
    // File path constants for JSON files
    private const val TIMETABLE_JSON_FILE = "$EXTERNAL_STORAGE_BASE_PATH/raw/timetable.json"
    private const val SUBSTITUTE_JSON_FILE = "$EXTERNAL_STORAGE_BASE_PATH/raw/substitutes.json"
    
    // File path constants for CSV files
    private const val TIMETABLE_CSV_FILE = "$EXTERNAL_STORAGE_BASE_PATH/raw/timetable.csv"
    private const val SUBSTITUTE_CSV_FILE = "$EXTERNAL_STORAGE_BASE_PATH/raw/substitutes.csv"
    
    /**
     * Check if the required files exist
     * @param context The application context
     * @return true if all required files exist, false otherwise
     */
    fun checkRequiredFiles(context: Context): Boolean {
        val timetableJsonFile = File(TIMETABLE_JSON_FILE)
        val timetableCsvFile = File(TIMETABLE_CSV_FILE)
        val substituteJsonFile = File(SUBSTITUTE_JSON_FILE)
        val substituteCsvFile = File(SUBSTITUTE_CSV_FILE)
        
        // Check for either JSON or CSV version of each file
        val hasTimetable = timetableJsonFile.exists() || timetableCsvFile.exists()
        val hasSubstitute = substituteJsonFile.exists() || substituteCsvFile.exists()
        
        Log.d(TAG, "Checking for required files:")
        Log.d(TAG, "Timetable file exists: $hasTimetable")
        Log.d(TAG, "Substitute file exists: $hasSubstitute")
        
        return hasTimetable && hasSubstitute
    }
    
    /**
     * Create base directories if they don't exist
     * @param context The application context
     */
    fun createRequiredDirectories(context: Context) {
        try {
            val baseDir = File(EXTERNAL_STORAGE_BASE_PATH)
            if (!baseDir.exists()) {
                val created = baseDir.mkdirs()
                Log.d(TAG, "Created base directory: $created - ${baseDir.absolutePath}")
            }
            
            val rawDir = File("$EXTERNAL_STORAGE_BASE_PATH/raw")
            if (!rawDir.exists()) {
                val created = rawDir.mkdirs()
                Log.d(TAG, "Created raw directory: $created - ${rawDir.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating directories", e)
        }
    }
} 