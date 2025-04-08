package com.substituemanagment.managment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.substituemanagment.managment.ui.components.DownloadingUpdateDialog
import com.substituemanagment.managment.ui.components.UpdateAvailableDialog
import com.substituemanagment.managment.ui.theme.SubstitutionManagementTheme
import com.substituemanagment.managment.utils.AppUpdateManager
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    private val TAG = "SplashActivity"
    private lateinit var appUpdateManager: AppUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize the update manager
        appUpdateManager = AppUpdateManager(this)
        
        setContent {
            SubstitutionManagementTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SplashScreen()
                }
            }
        }
        
        // Check for updates
        checkForUpdates()
    }
    
    private fun checkForUpdates() {
        // Set update callback
        appUpdateManager.setUpdateCallback(object : AppUpdateManager.UpdateCallback {
            override fun onUpdateAvailable(updateInfo: AppUpdateManager.UpdateInfo) {
                Log.d(TAG, "Update available: ${updateInfo.versionName}")
                
                // Show update dialog
                setContent {
                    SubstitutionManagementTheme {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            var showDownloadingDialog by remember { mutableStateOf(false) }
                            
                            SplashScreen()
                            
                            UpdateAvailableDialog(
                                updateInfo = updateInfo,
                                onDownloadClick = {
                                    showDownloadingDialog = true
                                    appUpdateManager.downloadUpdate()
                                },
                                onDismiss = {
                                    // Skip update and proceed with normal app launch
                                    proceedToMainActivity()
                                }
                            )
                            
                            if (showDownloadingDialog) {
                                DownloadingUpdateDialog(
                                    onDismiss = {
                                        showDownloadingDialog = false
                                        // Don't proceed to MainActivity, APK installation will take over
                                    }
                                )
                            }
                        }
                    }
                }
            }

            override fun onNoUpdateAvailable() {
                Log.d(TAG, "No update available")
                proceedToMainActivity()
            }

            override fun onUpdateDownloadStarted() {
                Log.d(TAG, "Update download started")
                // Handled by UI
            }

            override fun onUpdateDownloadCompleted(file: java.io.File) {
                Log.d(TAG, "Update download completed: ${file.absolutePath}")
                // The installation will be handled by AppUpdateManager
            }

            override fun onUpdateDownloadFailed(error: String) {
                Log.e(TAG, "Update download failed: $error")
                proceedToMainActivity()
            }

            override fun onUpdateCheckFailed(error: String) {
                Log.e(TAG, "Update check failed: $error")
                proceedToMainActivity()
            }
        })
        
        // Launch coroutine to check for updates
        lifecycleScope.launch {
            try {
                appUpdateManager.checkForUpdate()
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for updates", e)
                proceedToMainActivity()
            }
        }
    }
    
    private fun proceedToMainActivity() {
        // Delay for splash screen display (1.5 seconds)
        Handler(Looper.getMainLooper()).postDelayed({
            // Start MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            
            // Close this activity
            finish()
        }, 1500)
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "App Logo",
            modifier = Modifier.size(200.dp)
        )
    }
} 