package com.substituemanagment.managment.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.substituemanagment.managment.algorithm.PROCESSED_ASSIGNED_SUBSTITUTES_PATH
import com.substituemanagment.managment.algorithm.SubstituteManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.FloatingActionButton
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import com.substituemanagment.managment.navigation.Screen
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.text.style.TextOverflow
import com.substituemanagment.managment.utils.capitalizeWords

data class SubstituteAssignmentUI(
    val originalTeacher: String,
    val period: Int,
    val className: String,
    val substitute: String,
    val substitutePhone: String
)

// Add this new class to track unassigned classes
data class UnassignedClassUI(
    val teacher: String,
    val period: Int,
    val className: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubstitutionsScreen(navController: NavController) {
    val TAG = "SubstitutionsScreen"
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var assignmentsByTeacher by remember { mutableStateOf<Map<String, List<SubstituteAssignmentUI>>>(emptyMap()) }
    var expandedTeachers by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Track unassigned classes
    var unassignedClasses by remember { mutableStateOf<List<UnassignedClassUI>>(emptyList()) }
    
    // Function to load assignments from the file
    fun loadAssignments() {
        coroutineScope.launch {
            isLoading = true
            try {
                withContext(Dispatchers.IO) {
                    val file = File(PROCESSED_ASSIGNED_SUBSTITUTES_PATH)
                    if (file.exists()) {
                        val content = file.readText()
                        val type = object : TypeToken<Map<String, Any>>() {}.type
                        val data: Map<String, Any> = Gson().fromJson(content, type)
                        
                        if (data.containsKey("assignments")) {
                            val assignments = data["assignments"] as? List<*> ?: emptyList<Any>()
                            val uiAssignments = assignments.mapNotNull { assignment ->
                                if (assignment is Map<*, *>) {
                                    SubstituteAssignmentUI(
                                        originalTeacher = (assignment["originalTeacher"] as? String ?: "").capitalizeWords(),
                                        period = (assignment["period"] as? Double)?.toInt() 
                                            ?: (assignment["period"] as? Int) ?: 0,
                                        className = assignment["className"] as? String ?: "",
                                        substitute = (assignment["substitute"] as? String ?: "").capitalizeWords(),
                                        substitutePhone = assignment["substitutePhone"] as? String ?: ""
                                    )
                                } else null
                            }
                            
                            // Group assignments by originalTeacher
                            assignmentsByTeacher = uiAssignments.groupBy { it.originalTeacher }
                        } else {
                            errorMessage = "No assignments found in the file"
                            assignmentsByTeacher = emptyMap()
                        }
                        
                        // Load unassigned classes if they exist
                        unassignedClasses = if (data.containsKey("unassignedClasses")) {
                            val unassigned = data["unassignedClasses"] as? List<*> ?: emptyList<Any>()
                            unassigned.mapNotNull { item ->
                                if (item is Map<*, *>) {
                                    UnassignedClassUI(
                                        teacher = (item["teacher"] as? String ?: "").capitalizeWords(),
                                        period = (item["period"] as? Double)?.toInt() 
                                            ?: (item["period"] as? Int) ?: 0,
                                        className = item["className"] as? String ?: ""
                                    )
                                } else null
                            }
                        } else {
                            emptyList()
                        }
                    } else {
                        errorMessage = "Assignments file not found"
                        assignmentsByTeacher = emptyMap()
                        unassignedClasses = emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading assignments: ${e.message}", e)
                errorMessage = "Error loading assignments: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    // Function to clear all assignments
    fun clearAllAssignments() {
        coroutineScope.launch {
            isLoading = true
            try {
                withContext(Dispatchers.IO) {
                    // Create a new SubstituteManager instance and clear assignments
                    val substituteManager = SubstituteManager(context)
                    substituteManager.loadData()
                    substituteManager.clearAssignments()
                    
                    // Instead of deleting the file, write an empty assignments structure
                    val file = File(PROCESSED_ASSIGNED_SUBSTITUTES_PATH)
                    // Create proper empty structure with assignments array
                    val emptyAssignments = mapOf(
                        "assignments" to emptyList<Any>(),
                        "unassignedClasses" to emptyList<Any>(),
                        "warnings" to emptyList<String>()
                    )
                    val gson = GsonBuilder().setPrettyPrinting().create()
                    
                    // Create directory if it doesn't exist
                    file.parentFile?.mkdirs()
                    
                    // Write the empty structure to file
                    file.writeText(gson.toJson(emptyAssignments))
                    Log.d(TAG, "Successfully cleared assignments file with empty structure")
                    
                    // Clear the UI state
                    assignmentsByTeacher = emptyMap()
                    unassignedClasses = emptyList()
                    expandedTeachers = emptySet()
                    errorMessage = null
                }
                snackbarHostState.showSnackbar(
                    message = "All assignments cleared successfully",
                    duration = SnackbarDuration.Short
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing assignments: ${e.message}", e)
                errorMessage = "Error clearing assignments: ${e.message}"
                snackbarHostState.showSnackbar(
                    message = "Error: ${e.message}",
                    duration = SnackbarDuration.Long
                )
            } finally {
                isLoading = false
            }
        }
    }
    
    // Function to toggle teacher expansion
    fun toggleTeacherExpanded(teacher: String) {
        expandedTeachers = if (expandedTeachers.contains(teacher)) {
            expandedTeachers.minus(teacher)
        } else {
            expandedTeachers.plus(teacher)
        }
    }
    
    // Load assignments when the screen is first displayed
    LaunchedEffect(Unit) {
        loadAssignments()
    }
    
    // Confirmation dialog for clearing assignments
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear All Assignments") },
            text = { Text("Are you sure you want to clear all substitute assignments? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmDialog = false
                        clearAllAssignments()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showClearConfirmDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AssignmentTurnedIn,
                            contentDescription = "Assigned Substitutes",
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Assigned Substitutes") 
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate(Screen.Assign.route) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { loadAssignments() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    
                    // Badge indicating unassigned classes
                    if (unassignedClasses.isNotEmpty()) {
                        BadgedBox(
                            badge = {
                                Badge {
                                    Text(
                                        text = "${unassignedClasses.size}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        ) {
                            IconButton(onClick = { showClearConfirmDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear Assignments")
                            }
                        }
                    } else {
                        IconButton(onClick = { showClearConfirmDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear Assignments")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // Enhanced floating action button to navigate to SMS screen
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Navigating to SMS notification screen",
                            duration = SnackbarDuration.Short
                        )
                        navController.navigate(Screen.SmsSend.route)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.NotificationsActive, 
                        contentDescription = "Send SMS",
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Notify Teachers", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(56.dp),
                        strokeWidth = 4.dp
                    )
                    Text(
                        "Loading assignments...", 
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        } else if (errorMessage != null && assignmentsByTeacher.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(72.dp)
                    )
                    Text(
                        errorMessage ?: "Unknown error",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { loadAssignments() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry")
                    }
                }
            }
        } else {
            @OptIn(ExperimentalFoundationApi::class)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    AnimatedContent(
                        targetState = assignmentsByTeacher.isNotEmpty(),
                        label = "Header Animation"
                    ) { hasAssignments ->
                        if (hasAssignments) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Substitute Assignments Dashboard",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        // Teachers with substitutes count
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "${assignmentsByTeacher.size}",
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Teachers",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        
                                        // Total assignments count
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "${assignmentsByTeacher.values.flatten().size}",
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                text = "Classes",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        
                                        // Unassigned classes count
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "${unassignedClasses.size}",
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (unassignedClasses.isEmpty()) 
                                                    MaterialTheme.colorScheme.tertiary 
                                                else 
                                                    MaterialTheme.colorScheme.error
                                            )
                                            Text(
                                                text = "Unassigned",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Text(
                                        "Tap on a teacher to view assigned substitutes",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        "Use the notification button to send SMS alerts",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Assignment,
                                        contentDescription = "No Assignments",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "No Assignments Found",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Generate substitute assignments from the Attendance Management screen first",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Button(
                                        onClick = { navController.navigateUp() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Text("Go to Attendance Page")
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (unassignedClasses.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    "Unassigned Classes (${unassignedClasses.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                unassignedClasses.forEach { unassigned ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "•",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text(
                                            "${unassigned.className} (Period ${unassigned.period}) - ${unassigned.teacher}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (assignmentsByTeacher.isNotEmpty()) {
                    items(assignmentsByTeacher.keys.toList()) { teacher ->
                        val isExpanded = expandedTeachers.contains(teacher)
                        val assignments = assignmentsByTeacher[teacher] ?: emptyList()
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { toggleTeacherExpanded(teacher) }
                                .animateItemPlacement(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 2.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = teacher.firstOrNull()?.toString() ?: "?",
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = teacher,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${assignments.size} ${if (assignments.size == 1) "class" else "classes"} requiring substitutes",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                    
                                    Icon(
                                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    ) + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        // Sort assignments by period
                                        val sortedAssignments = assignments.sortedBy { it.period }
                                        
                                        sortedAssignments.forEachIndexed { index, assignment ->
                                            AssignmentRow(assignment = assignment)
                                            
                                            if (index < sortedAssignments.size - 1) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    teacherCount: Int, 
    assignmentCount: Int,
    unassignedCount: Int = 0
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Substitution Summary",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$teacherCount",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Absent Teachers",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$assignmentCount",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Total Assignments",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                // Add unassigned classes count if there are any
                if (unassignedCount > 0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$unassignedCount",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Unassigned",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // Warning about high absenteeism if relevant
            if (teacherCount > 10) {  // Customize this threshold as needed
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Warning: High number of absent teachers may lead to insufficient substitutes. Consider prioritizing higher-grade classes.",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
fun UnassignedClassesWarning(unassignedClasses: List<UnassignedClassUI>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Classes Without Substitutes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "The following classes could not be assigned substitutes due to insufficient available teachers:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Group by teacher for better organization
            val byTeacher = unassignedClasses.groupBy { it.teacher }
            
            byTeacher.forEach { (teacher, classes) ->
                Text(
                    text = teacher,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                
                classes.forEach { unassigned ->
                    Text(
                        text = "• Period ${unassigned.period}: ${unassigned.className}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Recommendation
            Text(
                text = "Recommendation: Consider manually assigning teachers to these classes or prioritizing them based on grade level.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun TeacherCard(
    teacherName: String,
    assignments: List<SubstituteAssignmentUI>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Teacher header (always visible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = teacherName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${assignments.size} assignments",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }
            
            // Assignments dropdown (visible when expanded)
            val transitionState = remember {
                MutableTransitionState(isExpanded).apply {
                    targetState = isExpanded
                }
            }
            transitionState.targetState = isExpanded
            
            AnimatedVisibility(
                visibleState = transitionState,
                enter = fadeIn(animationSpec = tween(durationMillis = 300)) +
                        expandVertically(animationSpec = tween(durationMillis = 300)),
                exit = fadeOut(animationSpec = tween(durationMillis = 300)) +
                        shrinkVertically(animationSpec = tween(durationMillis = 300))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Sort assignments by period
                    val sortedAssignments = assignments.sortedBy { it.period }
                    
                    sortedAssignments.forEachIndexed { index, assignment ->
                        AssignmentRow(assignment = assignment)
                        
                        if (index < sortedAssignments.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AssignmentRow(assignment: SubstituteAssignmentUI) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Period ${assignment.period}: ${assignment.className}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Substitute",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Substitute: ${assignment.substitute}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = "Phone",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = assignment.substitutePhone.ifEmpty { "No phone number" },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// Helper function to capitalize first letter of each word
fun String.capitalizeWords(): String {
    return this.split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
} 