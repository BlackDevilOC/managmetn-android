package com.substituemanagment.managment.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.substituemanagment.managment.data.SubstituteManager
import com.substituemanagment.managment.utils.SmsSender
import kotlinx.coroutines.launch

@Composable
fun TeacherReturnDialog(
    teacherName: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var showSmsDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "Teacher Return",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "$teacherName has returned. Would you like to send cancellation notifications to assigned substitutes?",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    isLoading = true
                                    SubstituteManager.handleTeacherReturn(
                                        context = context,
                                        teacherName = teacherName,
                                        onSendCancellationSms = { phone, message ->
                                            SmsSender.sendSms(context, phone, message)
                                        }
                                    )
                                    onSuccess()
                                } catch (e: Exception) {
                                    // Show error message
                                } finally {
                                    isLoading = false
                                    onDismiss()
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Send Notifications")
                        }
                    }
                    
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                try {
                                    isLoading = true
                                    // Just remove from lists without sending notifications
                                    val assignedSubstitutes = SubstituteManager.loadAssignedSubstitutes()
                                    val updatedAssignments = assignedSubstitutes.filter { it.absentTeacher != teacherName }
                                    SubstituteManager.saveAssignedSubstitutes(updatedAssignments)
                                    
                                    val absentTeachers = SubstituteManager.loadAbsentTeachers()
                                    val updatedAbsentTeachers = absentTeachers.filter { it.name != teacherName }
                                    SubstituteManager.saveAbsentTeachers(updatedAbsentTeachers)
                                    
                                    onSuccess()
                                } catch (e: Exception) {
                                    // Show error message
                                } finally {
                                    isLoading = false
                                    onDismiss()
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text("Don't Send")
                        }
                    }
                }
            }
        }
    }
} 