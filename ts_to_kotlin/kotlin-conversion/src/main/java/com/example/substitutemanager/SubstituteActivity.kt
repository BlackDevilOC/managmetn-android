package com.example.substitutemanager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

/**
 * Example activity showing how to use the SubstituteManager in an Android activity
 */
class SubstituteActivity : AppCompatActivity() {
    private lateinit var substituteManager: SubstituteManager
    
    // UI elements
    private lateinit var teacherSpinner: Spinner
    private lateinit var daySpinner: Spinner
    private lateinit var assignButton: Button
    private lateinit var clearButton: Button
    private lateinit var assignmentsRecyclerView: RecyclerView
    private lateinit var statusTextView: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_substitute)
        
        // Initialize the SubstituteManager
        substituteManager = SubstituteManager(this)
        
        // Find views
        teacherSpinner = findViewById(R.id.teacher_spinner)
        daySpinner = findViewById(R.id.day_spinner)
        assignButton = findViewById(R.id.assign_button)
        clearButton = findViewById(R.id.clear_button)
        assignmentsRecyclerView = findViewById(R.id.assignments_recycler_view)
        statusTextView = findViewById(R.id.status_text_view)
        
        // Set up RecyclerView
        assignmentsRecyclerView.layoutManager = LinearLayoutManager(this)
        
        // Load data
        lifecycleScope.launch {
            try {
                statusTextView.text = "Loading data..."
                
                // Load data from assets
                substituteManager.loadData()
                
                // Update UI with loaded data
                updateUI()
                
                statusTextView.text = "Data loaded successfully"
            } catch (e: Exception) {
                statusTextView.text = "Error loading data: ${e.message}"
                Toast.makeText(this@SubstituteActivity, 
                    "Error loading data: ${e.message}", 
                    Toast.LENGTH_LONG).show()
            }
        }
        
        // Set up button click listeners
        assignButton.setOnClickListener {
            val selectedTeacher = teacherSpinner.selectedItem.toString()
            val selectedDay = daySpinner.selectedItem.toString()
            
            lifecycleScope.launch {
                try {
                    statusTextView.text = "Assigning substitutes..."
                    
                    // Assign substitutes
                    val assignments = substituteManager.assignSubstitutes(
                        selectedTeacher, selectedDay)
                    
                    // Update UI
                    updateUI()
                    
                    // Show result
                    val count = assignments.size
                    statusTextView.text = "Assigned $count substitute(s) for $selectedTeacher on $selectedDay"
                    Toast.makeText(this@SubstituteActivity, 
                        "Assigned $count substitute(s)", 
                        Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    statusTextView.text = "Error assigning substitutes: ${e.message}"
                    Toast.makeText(this@SubstituteActivity, 
                        "Error assigning substitutes: ${e.message}", 
                        Toast.LENGTH_LONG).show()
                }
            }
        }
        
        clearButton.setOnClickListener {
            lifecycleScope.launch {
                try {
                    statusTextView.text = "Clearing assignments..."
                    
                    // Clear assignments
                    substituteManager.clearAssignments()
                    
                    // Update UI
                    updateUI()
                    
                    statusTextView.text = "All assignments cleared"
                    Toast.makeText(this@SubstituteActivity, 
                        "All assignments cleared", 
                        Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    statusTextView.text = "Error clearing assignments: ${e.message}"
                    Toast.makeText(this@SubstituteActivity, 
                        "Error clearing assignments: ${e.message}", 
                        Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    /**
     * Update the UI with current data
     */
    private fun updateUI() {
        // Get assignments
        val assignments = substituteManager.getSubstituteAssignments()
        
        // Update assignments in the adapter
        val adapter = AssignmentAdapter(assignments)
        assignmentsRecyclerView.adapter = adapter
        
        // Verify assignments
        val verificationReports = substituteManager.verifyAssignments()
        
        // Check for issues
        val totalIssues = verificationReports.sumOf { it.issues.size }
        if (totalIssues > 0) {
            val issueMessage = verificationReports
                .flatMap { it.issues }
                .joinToString("\n")
            
            Toast.makeText(this, 
                "Found $totalIssues issues with assignments", 
                Toast.LENGTH_LONG).show()
        }
    }
}

/**
 * Adapter for displaying assignments in a RecyclerView
 */
class AssignmentAdapter(
    private val assignments: Map<String, List<SubstituteAssignment>>
) : RecyclerView.Adapter<AssignmentAdapter.ViewHolder>() {
    
    // Flatten the map into a list for the adapter
    private val flattenedAssignments = assignments.entries
        .flatMap { (teacher, assignmentList) -> 
            assignmentList.map { assignment -> teacher to assignment } 
        }
    
    class ViewHolder(val view: android.view.View) : RecyclerView.ViewHolder(view) {
        val teacherTextView: TextView = view.findViewById(R.id.teacher_text_view)
        val periodTextView: TextView = view.findViewById(R.id.period_text_view)
        val classNameTextView: TextView = view.findViewById(R.id.class_name_text_view)
        val substituteTextView: TextView = view.findViewById(R.id.substitute_text_view)
        val substitutePhoneTextView: TextView = view.findViewById(R.id.substitute_phone_text_view)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_assignment, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (teacher, assignment) = flattenedAssignments[position]
        
        holder.teacherTextView.text = "Teacher: ${assignment.originalTeacher}"
        holder.periodTextView.text = "Period: ${assignment.period}"
        holder.classNameTextView.text = "Class: ${assignment.className}"
        holder.substituteTextView.text = "Substitute: ${assignment.substitute}"
        holder.substitutePhoneTextView.text = "Phone: ${assignment.substitutePhone}"
    }
    
    override fun getItemCount() = flattenedAssignments.size
}
