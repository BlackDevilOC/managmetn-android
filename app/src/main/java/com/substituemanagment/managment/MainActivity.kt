package com.substituemanagment.managment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.substituemanagment.managment.data.services.ReportingService
import com.substituemanagment.managment.navigation.NavGraph
import com.substituemanagment.managment.navigation.Screen
import com.substituemanagment.managment.ui.components.BottomNav
import com.substituemanagment.managment.ui.theme.SubstitutionManagementTheme
import com.substituemanagment.managment.utils.FileChecker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    
    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Log.d(TAG, "All permissions granted")
            // Create directories after permissions granted
            FileChecker.createRequiredDirectories(this)
        } else {
            Log.e(TAG, "Not all permissions granted: $permissions")
            // Handle permission denial
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check and request permissions first
        checkAndRequestPermissions()
        
        // Create required directories (will also be called after permissions are granted)
        FileChecker.createRequiredDirectories(this)
        
        // Start the reporting service
        startReportingService()
        
        setContent {
            SubstitutionManagementTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                var showMissingFilesDialog by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                
                // Check for required files
                LaunchedEffect(Unit) {
                    delay(500) // Small delay to ensure UI is loaded
                    val hasRequiredFiles = FileChecker.checkRequiredFiles(context)
                    if (!hasRequiredFiles) {
                        showMissingFilesDialog = true
                    }
                }
                
                // Missing files dialog
                if (showMissingFilesDialog) {
                    FilesRequiredDialog(
                        onDismiss = { showMissingFilesDialog = false },
                        onNavigateToUpload = {
                            showMissingFilesDialog = false
                            navController.navigate(Screen.FileUpload.route)
                        }
                    )
                }
                
                Scaffold(
                    bottomBar = {
                        // Get current route
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route
                        
                        // Bottom Navigation Bar
                        if (currentRoute in listOf(
                                Screen.Home.route,
                                Screen.Schedule.route,
                                Screen.Teachers.route,
                                Screen.Assign.route,
                                Screen.Settings.route,
                                Screen.ViewSubstitutions.route,
                                Screen.SmsSend.route
                            )
                        ) {
                            BottomNav(navController = navController)
                        }
                    }
                ) { paddingValues ->
                    NavGraph(
                        navController = navController,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Stop the reporting service when the app is destroyed
        stopReportingService()
    }
    
    /**
     * Start the reporting service for automatic report generation
     */
    private fun startReportingService() {
        val intent = Intent(this, ReportingService::class.java).apply {
            action = ReportingService.ACTION_APP_START
        }
        startService(intent)
        Log.d(TAG, "Started ReportingService on app start")
    }
    
    /**
     * Stop the reporting service when the app is closed
     */
    private fun stopReportingService() {
        val intent = Intent(this, ReportingService::class.java).apply {
            action = ReportingService.ACTION_APP_END
        }
        startService(intent)
        Log.d(TAG, "Sent stop signal to ReportingService on app end")
    }
    
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        // Check storage permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11+ we check specifically for external storage
            Log.d(TAG, "Android 11+ - checking modern storage permissions")
        } else {
            // For older versions, we check for read/write external storage
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        
        // Add SMS permission check if needed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.SEND_SMS)
        }
        
        // Request permissions if needed
        if (permissionsToRequest.isNotEmpty()) {
            Log.d(TAG, "Requesting permissions: $permissionsToRequest")
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            Log.d(TAG, "All permissions already granted")
        }
    }
}

@Composable
fun FilesRequiredDialog(
    onDismiss: () -> Unit,
    onNavigateToUpload: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Files Required",
                fontWeight = FontWeight.Bold
            ) 
        },
        text = { 
            Column {
                Text(
                    "This appears to be your first time using the app. The following files are required:"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "• Timetable file (timetable.json or timetable.csv)",
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "• Substitute teachers file (substitutes.json or substitutes.csv)",
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "You will be redirected to the file upload page where you can upload these files.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Important: After uploading, click the 'Process' button to analyze the files.",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onNavigateToUpload) {
                Text("Go to Upload Page")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        }
    )
}