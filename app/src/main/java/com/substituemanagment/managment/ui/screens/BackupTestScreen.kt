package com.substituemanagment.managment.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.substituemanagment.managment.data.DriveBackupService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupTestScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var isLoading by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Backup Test Screen",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                isLoading = true
                                val backupService = DriveBackupService(context)
                                backupService.initialize()
                                backupService.backupData()
                                snackbarHostState.showSnackbar("Backup completed successfully")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Backup failed: ${e.message}")
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text("Start Backup")
                }
                
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                isLoading = true
                                val backupService = DriveBackupService(context)
                                backupService.initialize()
                                backupService.restoreData()
                                snackbarHostState.showSnackbar("Restore completed successfully")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Restore failed: ${e.message}")
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text("Restore Backup")
                }
                
                if (isLoading) {
                    CircularProgressIndicator()
                }
            }
        }
    }
} 