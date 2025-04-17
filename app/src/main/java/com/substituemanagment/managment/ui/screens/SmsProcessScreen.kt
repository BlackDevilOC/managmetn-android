package com.substituemanagment.managment.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.substituemanagment.managment.ui.viewmodel.SmsViewModel
import com.substituemanagment.managment.utils.SmsSender
import com.substituemanagment.managment.data.SmsDataManager
import com.substituemanagment.managment.data.SmsHistoryDto
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

data class TeacherDetail(
    val isPresent: Boolean,
    val name: String,
    val phoneNumber: String,
    val variations: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsProcessScreen(
    navController: NavController,
    selectedTeachers: String?
) {
    val viewModel: SmsViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val teachersList = selectedTeachers?.split(",") ?: emptyList()
    val phoneNumbers = remember { mutableStateMapOf<String, String>() }
    val sortedTeachers = remember { mutableStateListOf<String>() }
    
    // Dialog states
    var showSendingDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var total by remember { mutableStateOf(0) }
    var isSending by remember { mutableStateOf(false) }

    // Function to get teacher phone number
    fun getTeacherPhoneNumber(teacherName: String): String? {
        try {
            val file = File("/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data/teacher_details.json")
            if (file.exists()) {
                val jsonString = file.readText()
                val type = object : TypeToken<List<TeacherDetail>>() {}.type
                val details = Gson().fromJson<List<TeacherDetail>>(jsonString, type)
                
                // Search for teacher in details
                val teacherDetail = details.find { detail ->
                    detail.name.equals(teacherName, ignoreCase = true) || 
                    detail.variations.any { it.equals(teacherName, ignoreCase = true) }
                }
                
                return teacherDetail?.phoneNumber
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    // Initialize SMS directory and files
    LaunchedEffect(Unit) {
        try {
            val baseDir = File(context.getExternalFilesDir(null), "substitute_data")
            val smsDir = File(baseDir, "sms")
            if (!smsDir.exists()) {
                smsDir.mkdirs()
            }

            // Initialize history file if it doesn't exist
            val historyFile = File(smsDir, "sms_history.json")
            if (!historyFile.exists()) {
                historyFile.createNewFile()
                // Initialize with empty array
                historyFile.writeText("[]")
            }

            // Initialize templates file if it doesn't exist
            val templatesFile = File(smsDir, "sms_templates.json")
            if (!templatesFile.exists()) {
                templatesFile.createNewFile()
                templatesFile.writeText("[]")
            }

            // Initialize contacts file if it doesn't exist
            val contactsFile = File(smsDir, "teacher_contacts.json")
            if (!contactsFile.exists()) {
                contactsFile.createNewFile()
                contactsFile.writeText("[]")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            scope.launch {
                snackbarHostState.showSnackbar("Error initializing SMS storage: ${e.message}")
            }
        }

        // Load phone numbers for all teachers
        sortedTeachers.addAll(teachersList)
        
        // Initialize phone numbers
        teachersList.forEach { teacher ->
            val phone = getTeacherPhoneNumber(teacher)
            if (!phone.isNullOrEmpty()) {
                phoneNumbers[teacher] = phone
            }
        }
    }

    // Sending Dialog
    if (showSendingDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    "Sending SMS",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Infinite animation progress indicator
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    // Progress text
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Sending messages...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "$progress of $total sent",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isSending = false
                        showSendingDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = {
                Text(
                    "SMS Sent Successfully",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("All messages have been sent successfully.")
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            showSuccessDialog = false
                            navController.navigate("home")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Home")
                    }
                    Button(
                        onClick = {
                            showSuccessDialog = false
                            navController.navigate("sms_history")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("View History")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Send SMS") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isSending) return@FloatingActionButton
                    
                    scope.launch {
                        try {
                            // Verify SMS directory exists before sending
                            val baseDir = File(context.getExternalFilesDir(null), "substitute_data")
                            val smsDir = File(baseDir, "sms")
                            if (!smsDir.exists()) {
                                smsDir.mkdirs()
                            }

                            val historyFile = File(smsDir, "sms_history.json")
                            if (!historyFile.exists()) {
                                historyFile.createNewFile()
                                historyFile.writeText("[]")
                            }

                            isSending = true
                            
                            // Get teachers with valid phone numbers
                            val teachersWithPhone = sortedTeachers.filter { teacher ->
                                val phone = phoneNumbers[teacher]
                                !phone.isNullOrEmpty()
                            }

                            if (teachersWithPhone.isEmpty()) {
                                snackbarHostState.showSnackbar("No valid phone numbers found")
                                isSending = false
                                return@launch
                            }

                            // Initialize progress
                            progress = 0
                            total = teachersWithPhone.size
                            showSendingDialog = true

                            var successCount = 0
                            var failureCount = 0

                            // Process teachers in smaller batches
                            val batchSize = 3
                            teachersWithPhone.chunked(batchSize).forEach { batch ->
                                if (!isSending) return@forEach
                                
                                batch.forEach { teacher ->
                                    if (!isSending) return@forEach
                                    
                                    val phone = phoneNumbers[teacher] ?: return@forEach
                                    val assignments = viewModel.teacherAssignments[teacher] ?: emptyList()
                                    if (assignments.isEmpty()) return@forEach

                                    val sortedAssignments = assignments.sortedBy { it.period }
                                    val dateStr = sortedAssignments.first().date
                                    val dayStr = java.time.LocalDate.parse(dateStr).dayOfWeek.toString()
                                        .lowercase().replaceFirstChar { it.uppercase() }

                                    val message = if (sortedAssignments.size == 1) {
                                        val assignment = sortedAssignments.first()
                                        """
                                        Dear ${teacher.split(" ").firstOrNull() ?: teacher},
                                        You have been assigned to:
                                        Period ${assignment.period}: ${assignment.className}
                                        Date: $dateStr ($dayStr)
                                        Original Teacher: ${assignment.originalTeacher}
                                        Please confirm your availability.
                                        """.trimIndent()
                                    } else {
                                        """
                                        Dear ${teacher.split(" ").firstOrNull() ?: teacher},
                                        You have been assigned to the following classes on $dateStr ($dayStr):
                                        
                                        ${sortedAssignments.joinToString("\n") { assignment ->
                                            "• Period ${assignment.period}: ${assignment.className} (for ${assignment.originalTeacher})"
                                        }}
                                        
                                        Please confirm your availability.
                                        """.trimIndent()
                                    }

                                    try {
                                        val success = SmsSender.sendSingleSms(context, phone, message)
                                        val smsId = UUID.randomUUID().toString()
                                        val timestamp = System.currentTimeMillis()
                                        
                                        if (success) {
                                            successCount++
                                            // Save successful SMS to history
                                            val historyEntry = SmsHistoryDto(
                                                id = smsId,
                                                recipients = listOf(teacher),
                                                message = message,
                                                timestamp = timestamp,
                                                status = "Sent"
                                            )
                                            SmsDataManager.addSmsToHistory(context, historyEntry)
                                            
                                            // Add to view model's history
                                            viewModel.smsHistory.add(0, SmsViewModel.SmsMessage(
                                                id = smsId,
                                                teacherName = teacher,
                                                teacherPhone = phone,
                                                message = message,
                                                timestamp = timestamp,
                                                status = "Sent"
                                            ))
                                        } else {
                                            failureCount++
                                            // Save failed SMS to history
                                            val historyEntry = SmsHistoryDto(
                                                id = smsId,
                                                recipients = listOf(teacher),
                                                message = message,
                                                timestamp = timestamp,
                                                status = "Failed to send"
                                            )
                                            SmsDataManager.addSmsToHistory(context, historyEntry)
                                            
                                            // Add to view model's history
                                            viewModel.smsHistory.add(0, SmsViewModel.SmsMessage(
                                                id = smsId,
                                                teacherName = teacher,
                                                teacherPhone = phone,
                                                message = message,
                                                timestamp = timestamp,
                                                status = "Failed to send"
                                            ))
                                        }
                                    } catch (e: Exception) {
                                        failureCount++
                                        val smsId = UUID.randomUUID().toString()
                                        val timestamp = System.currentTimeMillis()
                                        
                                        // Save error to history
                                        val historyEntry = SmsHistoryDto(
                                            id = smsId,
                                            recipients = listOf(teacher),
                                            message = message,
                                            timestamp = timestamp,
                                            status = "Error: ${e.message}"
                                        )
                                        SmsDataManager.addSmsToHistory(context, historyEntry)
                                        
                                        // Add to view model's history
                                        viewModel.smsHistory.add(0, SmsViewModel.SmsMessage(
                                            id = smsId,
                                            teacherName = teacher,
                                            teacherPhone = phone,
                                            message = message,
                                            timestamp = timestamp,
                                            status = "Error: ${e.message}"
                                        ))
                                    }

                                    progress++
                                    delay(500) // Delay between messages
                                }
                                delay(1000) // Delay between batches
                            }

                            // Show completion dialog with results
                            showSendingDialog = false
                            if (isSending) {
                                snackbarHostState.showSnackbar(
                                    "Completed: $successCount sent, $failureCount failed"
                                )
                                showSuccessDialog = successCount > 0
                            }
                            isSending = false
                        } catch (e: Exception) {
                            showSendingDialog = false
                            isSending = false
                            snackbarHostState.showSnackbar("Error: ${e.message}")
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send SMS",
                        tint = if (isSending) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = "Send SMS",
                        color = if (isSending) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (teachersList.isEmpty()) {
                Text(
                    text = "No teachers selected. Please go back and select teachers.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = "Selected Teachers (${teachersList.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sortedTeachers) { teacher ->
                        val hasPhoneNumber = !phoneNumbers[teacher].isNullOrEmpty()
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Teacher",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = teacher,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedTextField(
                                    value = phoneNumbers[teacher] ?: "",
                                    onValueChange = { newValue ->
                                        phoneNumbers[teacher] = newValue
                                    },
                                    label = { Text("Phone Number") },
                                    placeholder = { Text("Enter phone number") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = if (hasPhoneNumber) {
                                            Color.Green
                                        } else {
                                            Color.Red
                                        },
                                        unfocusedBorderColor = if (hasPhoneNumber) {
                                            Color.Green
                                        } else {
                                            Color.Red
                                        }
                                    ),
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = "Phone",
                                            tint = if (hasPhoneNumber) {
                                                Color.Green
                                            } else {
                                                Color.Red
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
} 