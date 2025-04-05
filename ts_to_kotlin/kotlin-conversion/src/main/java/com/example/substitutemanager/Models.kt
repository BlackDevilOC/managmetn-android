package com.example.substitutemanager

/**
 * Represents a teacher or substitute teacher
 * 
 * @property name The name of the teacher
 * @property phone The phone number of the teacher
 * @property isSubstitute Whether this teacher is available as a substitute
 * @property variations Alternate spellings or variations of the teacher's name
 * @property gradeLevel The grade level the teacher is qualified for (if applicable)
 */
data class Teacher(
    val name: String,
    val phone: String,
    val isSubstitute: Boolean = false,
    val variations: List<String> = listOf(),
    val gradeLevel: Int? = null
)

/**
 * Represents a teaching assignment
 * 
 * @property day The day of the week
 * @property period The period number
 * @property className The name/identifier of the class
 * @property originalTeacher The regular teacher for this class
 * @property substitute The substitute teacher assigned (if any)
 */
data class Assignment(
    val day: String,
    val period: Int,
    val className: String,
    val originalTeacher: String,
    val substitute: String = ""
)

/**
 * Represents a substitute teacher assignment
 * 
 * @property originalTeacher The teacher being substituted
 * @property period The period number
 * @property className The name/identifier of the class
 * @property substitute The substitute teacher assigned
 * @property substitutePhone The phone number of the substitute teacher
 */
data class SubstituteAssignment(
    val originalTeacher: String,
    val period: Int,
    val className: String,
    val substitute: String,
    val substitutePhone: String
)

/**
 * Represents the result of a verification check
 * 
 * @property success Whether the verification passed
 * @property issues List of issues found during verification
 * @property validAssignments Number of valid assignments
 * @property invalidAssignments Number of invalid assignments
 */
data class VerificationReport(
    val success: Boolean = true,
    val issues: List<String> = listOf(),
    val validAssignments: Int = 0,
    val invalidAssignments: Int = 0
)

/**
 * Represents a log entry for algorithm processing
 * 
 * @property timestamp When the log entry was created
 * @property action The action being performed
 * @property details Additional details about the action
 * @property status The status level of the log entry
 * @property data Optional data associated with the log entry
 * @property durationMs Time taken to perform the action (milliseconds)
 */
data class ProcessLog(
    val timestamp: String,
    val action: String, 
    val details: String,
    val status: String = "info",  // "info", "warning", or "error"
    val data: Any? = null,
    val durationMs: Long = 0
)

/**
 * Represents constraints for assignment algorithm
 * 
 * @property maxAssignmentsPerSubstitute Maximum assignments allowed per substitute
 * @property maxAssignmentsPerRegularTeacher Maximum assignments as substitute for regular teachers
 * @property preferSameGradeLevel Whether to prefer substitutes with matching grade level
 * @property allowNonSubstituteTeachers Whether regular teachers can be used as substitutes
 */
data class AssignmentConstraints(
    val maxAssignmentsPerSubstitute: Int = 3,
    val maxAssignmentsPerRegularTeacher: Int = 2,
    val preferSameGradeLevel: Boolean = true,
    val allowNonSubstituteTeachers: Boolean = false
)

/**
 * Constants used throughout the application
 */
object Constants {
    // Shared preferences
    const val PREFS_NAME = "SubstituteManagerPrefs"
    const val KEY_SUBSTITUTE_ASSIGNMENTS = "substitute_assignments"
    
    // Default file paths
    const val DEFAULT_TIMETABLE_PATH = "data/timetable_file.csv"
    const val DEFAULT_SUBSTITUTES_PATH = "data/Substitude_file.csv"
    const val DEFAULT_TEACHERS_PATH = "data/total_teacher.json"
    const val DEFAULT_SCHEDULES_PATH = "data/teacher_schedules.json"
    const val DEFAULT_ASSIGNMENTS_PATH = "data/assigned_teacher.json"
    
    // Logging tag
    const val TAG = "SubstituteManager"
}
