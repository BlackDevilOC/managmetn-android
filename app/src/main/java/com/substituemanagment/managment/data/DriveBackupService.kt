package com.substituemanagment.managment.data

import android.content.Context
import android.util.Log
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.Collections
import java.util.UUID

class DriveBackupService(private val context: Context) {
    private val TAG = "DriveBackupService"
    private val BASE_DIR = "/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data"
    private val BACKUP_FOLDER_NAME = "SubstituteManagerBackups"
    private val DEVICE_INFO_FILE = "device_info.json"
    private val LOGS_FILE = "substitute_logs.json"
    
    private var driveService: Drive? = null
    private var backupFolderId: String? = null
    
    data class DeviceInfo(
        val deviceId: String,
        val deviceName: String,
        val lastBackupTime: Long,
        val appVersion: String
    )
    
    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            try {
                val credentials = GoogleCredentials.fromStream(
                    context.assets.open("credentials.json")
                ).createScoped(Collections.singleton(DriveScopes.DRIVE_FILE))
                
                driveService = Drive.Builder(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    HttpCredentialsAdapter(credentials)
                ).setApplicationName("SubstituteManager")
                    .build()
                
                backupFolderId = getOrCreateBackupFolder()
                Log.d(TAG, "Drive service initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing Drive service", e)
                throw e
            }
        }
    }
    
    private suspend fun getOrCreateBackupFolder(): String {
        return withContext(Dispatchers.IO) {
            try {
                val result = driveService?.files()?.list()
                    ?.setQ("name='$BACKUP_FOLDER_NAME' and mimeType='application/vnd.google-apps.folder' and trashed=false")
                    ?.setSpaces("drive")
                    ?.execute()
                
                val folders = result?.files
                if (folders?.isNotEmpty() == true) {
                    return@withContext folders[0].id
                }
                
                // Create new folder
                val folderMetadata = File()
                    .setName(BACKUP_FOLDER_NAME)
                    .setMimeType("application/vnd.google-apps.folder")
                
                val folder = driveService?.files()?.create(folderMetadata)
                    ?.setFields("id")
                    ?.execute()
                
                folder?.id ?: throw IOException("Failed to create backup folder")
            } catch (e: Exception) {
                Log.e(TAG, "Error getting/creating backup folder", e)
                throw e
            }
        }
    }
    
    suspend fun createDeviceFolder(): String {
        return withContext(Dispatchers.IO) {
            try {
                val deviceId = getDeviceId()
                val result = driveService?.files()?.list()
                    ?.setQ("name='$deviceId' and mimeType='application/vnd.google-apps.folder' and trashed=false and '$backupFolderId' in parents")
                    ?.setSpaces("drive")
                    ?.execute()
                
                val folders = result?.files
                if (folders?.isNotEmpty() == true) {
                    return@withContext folders[0].id
                }
                
                // Create new device folder
                val folderMetadata = File()
                    .setName(deviceId)
                    .setMimeType("application/vnd.google-apps.folder")
                    .setParents(Collections.singletonList(backupFolderId))
                
                val folder = driveService?.files()?.create(folderMetadata)
                    ?.setFields("id")
                    ?.execute()
                
                folder?.id ?: throw IOException("Failed to create device folder")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating device folder", e)
                throw e
            }
        }
    }
    
    suspend fun backupData() {
        withContext(Dispatchers.IO) {
            try {
                val deviceFolderId = createDeviceFolder()
                val timestamp = System.currentTimeMillis()
                
                // Backup all files from the base directory
                val baseDir = File(BASE_DIR)
                if (baseDir.exists() && baseDir.isDirectory) {
                    baseDir.listFiles()?.forEach { file ->
                        if (file.isFile) {
                            uploadFile(file, deviceFolderId, timestamp)
                        }
                    }
                }
                
                // Update device info
                updateDeviceInfo(deviceFolderId, timestamp)
                
                Log.d(TAG, "Backup completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error during backup", e)
                throw e
            }
        }
    }
    
    private suspend fun uploadFile(file: java.io.File, deviceFolderId: String, timestamp: Long) {
        withContext(Dispatchers.IO) {
            try {
                val metadata = File()
                    .setName("${file.name}_${timestamp}")
                    .setParents(Collections.singletonList(deviceFolderId))
                
                val mediaContent = FileContent("application/octet-stream", file)
                
                driveService?.files()?.create(metadata, mediaContent)
                    ?.setFields("id")
                    ?.execute()
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading file: ${file.name}", e)
                throw e
            }
        }
    }
    
    private suspend fun updateDeviceInfo(deviceFolderId: String, timestamp: Long) {
        withContext(Dispatchers.IO) {
            try {
                val deviceInfo = DeviceInfo(
                    deviceId = getDeviceId(),
                    deviceName = android.os.Build.MODEL,
                    lastBackupTime = timestamp,
                    appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
                )
                
                val metadata = File()
                    .setName(DEVICE_INFO_FILE)
                    .setParents(Collections.singletonList(deviceFolderId))
                
                val content = context.gson.toJson(deviceInfo).toByteArray()
                val mediaContent = FileContent("application/json", java.io.File.createTempFile("device_info", ".json").apply {
                    writeBytes(content)
                })
                
                driveService?.files()?.create(metadata, mediaContent)
                    ?.setFields("id")
                    ?.execute()
            } catch (e: Exception) {
                Log.e(TAG, "Error updating device info", e)
                throw e
            }
        }
    }
    
    suspend fun restoreData() {
        withContext(Dispatchers.IO) {
            try {
                val deviceFolderId = createDeviceFolder()
                val result = driveService?.files()?.list()
                    ?.setQ("'$deviceFolderId' in parents and trashed=false")
                    ?.setSpaces("drive")
                    ?.setFields("files(id, name, createdTime)")
                    ?.execute()
                
                val files = result?.files ?: return@withContext
                
                // Find the latest backup
                val latestBackup = files.maxByOrNull { it.createdTime?.value ?: 0 }
                if (latestBackup != null) {
                    downloadAndRestoreFile(latestBackup.id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during restore", e)
                throw e
            }
        }
    }
    
    private suspend fun downloadAndRestoreFile(fileId: String) {
        withContext(Dispatchers.IO) {
            try {
                val outputFile = driveService?.files()?.get(fileId)?.executeMediaAndDownloadTo(java.io.File(BASE_DIR))
                if (outputFile != null) {
                    Log.d(TAG, "File restored successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading file", e)
                throw e
            }
        }
    }
    
    private fun getDeviceId(): String {
        val sharedPrefs = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
        var deviceId = sharedPrefs.getString("device_id", null)
        
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            sharedPrefs.edit().putString("device_id", deviceId).apply()
        }
        
        return deviceId
    }
} 