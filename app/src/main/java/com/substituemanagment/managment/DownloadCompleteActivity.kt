package com.substituemanagment.managment

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.substituemanagment.managment.ui.theme.SubstitutionManagementTheme
import java.io.File

/**
 * Activity to handle APK installation after download completion.
 * Provides explicit button for user to trigger installation if automatic installation fails.
 */
class DownloadCompleteActivity : ComponentActivity() {
    
    private val TAG = "DownloadCompleteActivity"
    private var filePath: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get file path from intent
        filePath = intent.getStringExtra("FILE_PATH")
        val versionName = intent.getStringExtra("VERSION_NAME") ?: "new version"
        
        Log.d(TAG, "Download complete activity started with file path: $filePath")
        
        if (filePath == null) {
            Log.e(TAG, "No file path provided in intent")
            finish()
            return
        }
        
        // Check if file exists
        val file = File(filePath!!)
        if (!file.exists()) {
            Log.e(TAG, "APK file does not exist at path: $filePath")
            // Try to find in default download location
            val fileName = file.name
            val downloadsDir = getExternalFilesDir(null)
            val alternativeFile = File(downloadsDir, fileName)
            
            if (!alternativeFile.exists()) {
                Log.e(TAG, "Alternative file also does not exist")
                finish()
                return
            }
        }
        
        setContent {
            SubstitutionManagementTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    InstallPromptScreen(
                        versionName = versionName,
                        onInstallNowClick = { initiateInstallation() },
                        onCancelClick = { finish() }
                    )
                }
            }
        }
        
        // Try to automatically initiate installation
        initiateInstallation()
    }
    
    private fun initiateInstallation() {
        try {
            val file = File(filePath!!)
            if (!file.exists()) {
                Log.e(TAG, "File doesn't exist when trying to install: $filePath")
                return
            }
            
            Log.d(TAG, "Initiating installation for file: ${file.absolutePath}")
            
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            
            Log.d(TAG, "Starting installation intent with URI: $uri")
            startActivity(intent)
            
            // Don't finish the activity immediately to allow the user to come back
            // if installation is canceled or fails
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating installation: ${e.message}", e)
        }
    }
}

@Composable
fun InstallPromptScreen(
    versionName: String,
    onInstallNowClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Update Downloaded",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Version $versionName has been downloaded and is ready to install.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "If installation didn't start automatically, click the button below.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onInstallNowClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Install Now")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onCancelClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
} 