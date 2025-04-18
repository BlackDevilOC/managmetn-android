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
                
                // Create a more unique filename to avoid conflicts
                val apkFileName = "app-update-${info.versionName}-${System.currentTimeMillis()}.apk"
                
                val request = DownloadManager.Request(uri).apply {
                    setTitle("Downloading Update")
                    setDescription("Downloading version ${info.versionName}")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        apkFileName
                    )
                }
                
                // Register broadcast receiver to get notified when download completes
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                        if (id == downloadID) {
                            Log.d(TAG, "Download with ID $downloadID completed")
                            
                            try {
                                // Unregister receiver first to avoid memory leaks
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
                                        // Get download location
                                        val localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                                        val localUri = cursor.getString(localUriIndex)
                                        Log.d(TAG, "Download URI: $localUri")
                                        
                                        // Get the file from the Downloads directory
                                        val file = File(
                                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                            apkFileName
                                        )
                                        
                                        if (file.exists()) {
                                            Log.d(TAG, "APK file found at: ${file.absolutePath}")
                                            updateCallback?.onUpdateDownloadCompleted(file)
                                            
                                            // Launch the DownloadCompleteActivity to handle installation
                                            launchInstallActivity(file, info.versionName)
                                        } else {
                                            // Try alternative method to get the file
                                            try {
                                                val uri = Uri.parse(localUri)
                                                val path = uri.path
                                                if (path != null) {
                                                    val alternativeFile = File(path)
                                                    if (alternativeFile.exists()) {
                                                        Log.d(TAG, "APK file found at alternative path: ${alternativeFile.absolutePath}")
                                                        updateCallback?.onUpdateDownloadCompleted(alternativeFile)
                                                        launchInstallActivity(alternativeFile, info.versionName)
                                                    } else {
                                                        Log.e(TAG, "Alternative file does not exist: $path")
                                                        updateCallback?.onUpdateDownloadFailed("Downloaded file not found")
                                                    }
                                                } else {
                                                    Log.e(TAG, "URI path is null: $localUri")
                                                    updateCallback?.onUpdateDownloadFailed("Downloaded file path is null")
                                                }
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Error finding downloaded file: ${e.message}", e)
                                                updateCallback?.onUpdateDownloadFailed("Error finding downloaded file: ${e.message}")
                                            }
                                        }
                                    } else {
                                        val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                                        val reason = cursor.getInt(reasonIndex)
                                        Log.e(TAG, "Download failed: Status = $status, Reason = $reason")
                                        updateCallback?.onUpdateDownloadFailed("Download failed: Status = $status, Reason = $reason")
                                    }
                                } else {
                                    Log.e(TAG, "Download query returned no results")
                                    updateCallback?.onUpdateDownloadFailed("Download information not found")
                                }
                                cursor.close()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error processing download: ${e.message}", e)
                                updateCallback?.onUpdateDownloadFailed("Error processing download: ${e.message}")
                            }
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
                Log.d(TAG, "Started download with ID: $downloadID for URL: ${info.downloadUrl}")
                updateCallback?.onUpdateDownloadStarted()
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading update: ${e.message}", e)
                updateCallback?.onUpdateDownloadFailed("Error downloading update: ${e.message}")
            }
        } ?: run {
            Log.e(TAG, "No update info available")
            updateCallback?.onUpdateDownloadFailed("No update info available")
        }
    }
    
    /**
     * Launch the DownloadCompleteActivity to handle APK installation
     */
    private fun launchInstallActivity(file: File, versionName: String) {
        try {
            Log.d(TAG, "Launching DownloadCompleteActivity for file: ${file.absolutePath}")
            
            val intent = Intent(context, Class.forName("com.substituemanagment.managment.DownloadCompleteActivity")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("FILE_PATH", file.absolutePath)
                putExtra("VERSION_NAME", versionName)
            }
            
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching install activity: ${e.message}", e)
            
            // Fallback to direct installation if activity launch fails
            installUpdate(file)
        }
    }
    
    /**
     * Install the downloaded APK (fallback method)
     */
    private fun installUpdate(file: File) {
        try {
            Log.d(TAG, "Initiating APK installation for: ${file.absolutePath}")
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                
                // For Android N and above, we need to use FileProvider
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val authority = "${context.packageName}.fileprovider"
                    Log.d(TAG, "Using FileProvider with authority: $authority")
                    
                    val uri = FileProvider.getUriForFile(
                        context,
                        authority,
                        file
                    )
                    Log.d(TAG, "FileProvider URI: $uri")
                    
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    val uri = Uri.fromFile(file)
                    Log.d(TAG, "Direct file URI: $uri")
                    setDataAndType(uri, "application/vnd.android.package-archive")
                }
            }
            
            Log.d(TAG, "Starting installation activity")
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error installing update: ${e.message}", e)
            
            // Try alternative installation method if the first one fails
            try {
                Log.d(TAG, "Trying alternative installation method")
                val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                    } else {
                        Uri.fromFile(file)
                    }
                    data = uri
                    putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, context.applicationInfo.packageName)
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                Log.e(TAG, "Alternative installation also failed: ${e2.message}", e2)
            }
        }
    }
} 