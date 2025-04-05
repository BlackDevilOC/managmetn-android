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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.substituemanagment.managment.algorithm.PROCESSED_ASSIGNED_SUBSTITUTES_PATH
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.material3.HorizontalDivider

data class SubstituteAssignmentUI(
    val originalTeacher: String,
    val period: Int,
    val className: String,
    val substitute: String,
    val substitutePhone: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubstitutionsScreen(navController: NavController) {
    val TAG = "SubstitutionsScreen"
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var assignmentsByTeacher by remember { mutableStateOf<Map<String, List<SubstituteAssignmentUI>>>(emptyMap()) }
    var expandedTeachers by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    // Function to load assignments from the file
    LaunchedEffect(Unit) {
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
                    }
                } else {
                    errorMessage = "Assignments file not found"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading assignments: ${e.message}", e)
            errorMessage = "Error loading assignments: ${e.message}"
        } finally {
            isLoading = false
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Substitutions") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isLoading = true }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading assignments...")
                    }
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "Unknown error",
                            textAlign = TextAlign.Center
                        )
                    }
                }
                assignmentsByTeacher.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No substitutions found",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
                    ) {
                        // Show summary at the top
                        item {
                            SummaryCard(
                                teacherCount = assignmentsByTeacher.size,
                                assignmentCount = assignmentsByTeacher.values.sumOf { it.size }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        // Show each absent teacher with their substitutes
                        items(assignmentsByTeacher.entries.toList().sortedBy { it.key }) { (teacher, assignments) ->
                            val isExpanded = expandedTeachers.contains(teacher)
                            TeacherCard(
                                teacherName = teacher,
                                assignments = assignments,
                                isExpanded = isExpanded,
                                onToggleExpand = { toggleTeacherExpanded(teacher) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(teacherCount: Int, assignmentCount: Int) {
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
            }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Period indicator
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${assignment.period}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Row {
                Text(
                    text = "Class: ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = assignment.className,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Row {
                Text(
                    text = "Substitute: ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = assignment.substitute,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (assignment.substitutePhone.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "Phone",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = assignment.substitutePhone,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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