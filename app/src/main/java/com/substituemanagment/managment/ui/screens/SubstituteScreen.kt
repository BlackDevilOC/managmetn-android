package com.substituemanagment.managment.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.substituemanagment.managment.data.AbsentTeacherData
import com.substituemanagment.managment.data.TeacherDataManager
import com.substituemanagment.managment.navigation.Screen
import com.substituemanagment.managment.algorithm.SubstituteManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubstituteScreen(
    navController: NavController = rememberNavController()
) {
    val TAG = "SubstituteScreen"
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var absentTeachers by remember { mutableStateOf<List<AbsentTeacherData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var assignmentResults by remember { mutableStateOf<Map<String, List<Any>>>(emptyMap()) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Lazy list state for scroll position
    val lazyListState = rememberLazyListState()
    
    // Detect when user has scrolled
    val isScrolled = remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0
        }
    }
    
    // Get today's day of the week
    val today = remember {
        LocalDate.now().dayOfWeek.name.lowercase()
    }
    
    // Format today's date for display
    val formattedDate = remember {
        val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
        LocalDate.now().format(formatter)
    }

    // Function to load absent teachers from file
    val loadAbsentTeachers = {
        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val baseDir = File("/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data")
                    val absentTeachersFile = File(baseDir, "absent_teachers.json")
                    
                    if (absentTeachersFile.exists()) {
                        val type = object : TypeToken<Set<AbsentTeacherData>>() {}.type
                        FileReader(absentTeachersFile).use { reader ->
                            val teachers: Set<AbsentTeacherData> = Gson().fromJson(reader, type)
                            // Filter to only show unassigned teachers
                            absentTeachers = teachers.filter { !it.assignedSubstitute }.toList()
                        }
                    } else {
                        absentTeachers = emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading absent teachers", e)
                snackbarHostState.showSnackbar(
                    message = "Error: ${e.message}",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    // Function to assign substitute to a specific teacher
    val assignSubstituteToTeacher = { teacherName: String ->
        coroutineScope.launch {
            isLoading = true
            try {
                withContext(Dispatchers.IO) {
                    val substituteManager = SubstituteManager(context)
                    substituteManager.loadData()
                    
                    val assignments = substituteManager.assignSubstitutes(teacherName, today)
                    
                    if (assignments.isNotEmpty()) {
                        // Update the teacher's assigned status
                        val teacherDataManager = TeacherDataManager(context)
                        teacherDataManager.setTeacherAssigned(teacherName, true)
                        
                        // Refresh the list
                        loadAbsentTeachers()
                        successMessage = "Substitutes assigned for $teacherName"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error assigning substitute", e)
                snackbarHostState.showSnackbar(
                    message = "Error assigning substitute: ${e.message}",
                    duration = SnackbarDuration.Long
                )
            } finally {
                isLoading = false
            }
        }
    }

    // Function to assign substitutes to all unassigned teachers
    val assignAllSubstitutes = {
        coroutineScope.launch {
            isLoading = true
            try {
                withContext(Dispatchers.IO) {
                    val substituteManager = SubstituteManager(context)
                    substituteManager.loadData()
                    
                    val teacherDataManager = TeacherDataManager(context)
                    
                    // Process each unassigned teacher
                    absentTeachers.forEach { teacher ->
                        val assignments = substituteManager.assignSubstitutes(teacher.name, today)
                        if (assignments.isNotEmpty()) {
                            // Update the teacher's assigned status
                            teacherDataManager.setTeacherAssigned(teacher.name, true)
                        }
                    }
                    
                    // Refresh the list
                    loadAbsentTeachers()
                }
                snackbarHostState.showSnackbar(
                    message = "All substitutes assigned successfully",
                    duration = SnackbarDuration.Short
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error assigning all substitutes", e)
                snackbarHostState.showSnackbar(
                    message = "Error: ${e.message}",
                    duration = SnackbarDuration.Long
                )
            } finally {
                isLoading = false
            }
        }
    }

    // Initial load of absent teachers
    LaunchedEffect(Unit) {
        loadAbsentTeachers()
    }

    // Show success message if there is one
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            snackbarHostState.showSnackbar(
                message = successMessage!!,
                duration = SnackbarDuration.Short
            )
            successMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PersonOff,
                            contentDescription = "Absent Teachers",
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Attendance Management") 
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 2.dp,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = { navController.navigate(Screen.ViewSubstitutions.route) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    Icon(
                        Icons.Default.List,
                        contentDescription = "View Assignments",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Assigned Substitutes", fontWeight = FontWeight.Bold)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(56.dp),
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Assigning substitutes...",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Main content with LazyColumn
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = 80.dp, // Leave space for collapsed header
                            bottom = 16.dp,
                            start = 16.dp,
                            end = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Main content list
                        if (absentTeachers.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "All Assigned",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(72.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "All Teachers Covered",
                                            style = MaterialTheme.typography.titleLarge,
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "All absent teachers have been assigned substitutes.",
                                            style = MaterialTheme.typography.bodyLarge,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Teacher cards
                            itemsIndexed(absentTeachers) { index, teacher ->
                                AbsentTeacherCard(
                                    teacher = teacher,
                                    onAssignClick = { assignSubstituteToTeacher(teacher.name) },
                                    modifier = Modifier
                                        .animateContentSize()
                                )
                            }
                        }
                        
                        // Add bottom space to avoid FAB overlap
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                    
                    // Floating header - collapses on scroll
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shadowElevation = if (isScrolled.value) 4.dp else 0.dp
                    ) {
                        Column {
                            // Compact header that's always visible
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${absentTeachers.size}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    if (isScrolled.value) {
                                        // Compact title when scrolled
                                        Text(
                                            text = "${absentTeachers.size} ${if (absentTeachers.size == 1) "Teacher" else "Teachers"} Need Subs",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    } else {
                                        // Full title when at top
                                        Column {
                                            Text(
                                                text = "Absent Teachers",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                text = formattedDate,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                                
                                // "Assign All" button - always visible
                                if (absentTeachers.isNotEmpty()) {
                                    Button(
                                        onClick = { assignAllSubstitutes() },
                                        enabled = absentTeachers.isNotEmpty(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        contentPadding = PaddingValues(
                                            horizontal = 12.dp,
                                            vertical = 8.dp
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.PersonAdd,
                                            contentDescription = "Assign All",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        if (!isScrolled.value) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Assign All", style = MaterialTheme.typography.labelMedium)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbsentTeacherCard(
    teacher: AbsentTeacherData,
    onAssignClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timestamp = remember(teacher.timestamp) {
        Instant.parse(teacher.timestamp)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("hh:mm a"))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Teacher avatar
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = teacher.name.first().toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = teacher.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = "Time",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Marked absent at: $timestamp",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                FilledTonalButton(
                    onClick = onAssignClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        Icons.Default.AssignmentInd,
                        contentDescription = "Assign",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Assign", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AssignmentStatusChip(
    text: String,
    isAssigned: Boolean = true
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (isAssigned) MaterialTheme.colorScheme.primaryContainer 
               else MaterialTheme.colorScheme.errorContainer,
        contentColor = if (isAssigned) MaterialTheme.colorScheme.onPrimaryContainer 
                      else MaterialTheme.colorScheme.onErrorContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
} 