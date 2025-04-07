package com.substituemanagment.managment.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * AppUpdateManager handles auto-update functionality for the app.
 * It checks for updates, downloads, and installs new versions automatically.
 */
class AppUpdateManager(private val context: Context) {
    private val TAG = "AppUpdateManager"
    private var downloadID: Long = 0
    private var updateCallback: UpdateCallback? = null
    
    // Update information
    private var updateInfo: UpdateInfo? = null
    
    // URL where update metadata is stored
    private val UPDATE_METADATA_URL = "https://raw.githubusercontent.com/BlackDevilOC/managmetn-android/new/update.json"
    
    /**
     * Data class to hold update information
     */
    data class UpdateInfo(
        val versionCode: Int,
        val versionName: String,
        val downloadUrl: String,
        val releaseNotes: String
    )
    
    /**
     * Interface for callback when update events occur
     */
    interface UpdateCallback {
        fun onUpdateAvailable(updateInfo: UpdateInfo)
        fun onNoUpdateAvailable()
        fun onUpdateDownloadStarted()
        fun onUpdateDownloadCompleted(file: File)
        fun onUpdateDownloadFailed(error: String)
        fun onUpdateCheckFailed(error: String)
    }
    
    /**
     * Set the callback for update events
     */
    fun setUpdateCallback(callback: UpdateCallback) {
        this.updateCallback = callback
    }
    
    /**
     * Check if an update is available
     * Returns true if an update is available, false otherwise
     */
    suspend fun checkForUpdate(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Get current app version
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            
            val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
            
            val currentVersionName = packageInfo.versionName
            
            // Fetch update metadata from server
            val url = URL(UPDATE_METADATA_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                
                // Parse JSON response
                val jsonObject = JSONObject(response.toString())
                val serverVersionCode = jsonObject.getInt("versionCode")
                val serverVersionName = jsonObject.getString("versionName")
                val downloadUrl = jsonObject.getString("downloadUrl")
                val releaseNotes = jsonObject.getString("releaseNotes")
                
                // Create UpdateInfo object
                updateInfo = UpdateInfo(
                    versionCode = serverVersionCode,
                    versionName = serverVersionName,
                    downloadUrl = downloadUrl,
                    releaseNotes = releaseNotes
                )
                
                // Compare versions
                val updateAvailable = serverVersionCode > currentVersionCode
                
                if (updateAvailable) {
                    Log.d(TAG, "Update available: $serverVersionName (current: $currentVersionName)")
                    updateCallback?.onUpdateAvailable(updateInfo!!)
                } else {
                    Log.d(TAG, "No update available. Current: $currentVersionName, Latest: $serverVersionName")
                    updateCallback?.onNoUpdateAvailable()
                }
                
                return@withContext updateAvailable
            } else {
                Log.e(TAG, "Failed to fetch update metadata. Response code: $responseCode")
                updateCallback?.onUpdateCheckFailed("Server returned error code: $responseCode")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates: ${e.message}", e)
            updateCallback?.onUpdateCheckFailed("Error: ${e.message}")
            return@withContext false
        }
    }
    
    /**
     * Download the update APK file
     */
    fun downloadUpdate() {
        updateInfo?.let { info ->
            try {
                // Create download request
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val uri = Uri.parse(info.downloadUrl)
                
                val request = DownloadManager.Request(uri).apply {
                    setTitle("Downloading Update")
                    setDescription("Downloading version ${info.versionName}")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        "app-update-${info.versionName}.apk"
                    )
                }
                
                // Register broadcast receiver to get notified when download completes
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                        if (id == downloadID) {
                            // Download completed
                            context.unregisterReceiver(this)
                            
                            // Get download info
                            val query = DownloadManager.Query().apply {
                                setFilterById(downloadID)
                            }
                            val cursor = downloadManager.query(query)
                            
                            if (cursor.moveToFirst()) {
                                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                                val status = cursor.getInt(statusIndex)
                                
                                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                    // Get downloaded file
                                    val localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                                    val localUri = cursor.getString(localUriIndex)
                                    val uri = Uri.parse(localUri)
                                    val file = File(uri.path!!)
                                    
                                    Log.d(TAG, "Download completed: ${file.absolutePath}")
                                    updateCallback?.onUpdateDownloadCompleted(file)
                                    
                                    // Install the APK
                                    installUpdate(file)
                                } else {
                                    val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                                    val reason = cursor.getInt(reasonIndex)
                                    Log.e(TAG, "Download failed: $reason")
                                    updateCallback?.onUpdateDownloadFailed("Download failed: $reason")
                                }
                            }
                            cursor.close()
                        }
                    }
                }
                
                // Register receiver
                context.registerReceiver(
                    receiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
                
                // Start download
                downloadID = downloadManager.enqueue(request)
                Log.d(TAG, "Started download with ID: $downloadID")
                updateCallback?.onUpdateDownloadStarted()
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading update: ${e.message}", e)
                updateCallback?.onUpdateDownloadFailed("Error: ${e.message}")
            }
        } ?: run {
            Log.e(TAG, "No update info available")
            updateCallback?.onUpdateDownloadFailed("No update info available")
        }
    }
    
    /**
     * Install the downloaded APK
     */
    private fun installUpdate(file: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                
                // For Android N and above, we need to use FileProvider
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
                }
            }
            
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error installing update: ${e.message}", e)
        }
    }
} 