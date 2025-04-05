package substitutemanager.models

/**
 * Represents a teacher in the system.
 *
 * @property name The name of the teacher
 * @property phone The phone number of the teacher
 * @property isSubstitute Whether the teacher is a substitute
 * @property variations Alternative names or spellings for the teacher
 * @property gradeLevel The highest grade level the teacher is qualified to teach
 */
data class Teacher(
    val name: String,
    val phone: String,
    val isSubstitute: Boolean = false,
    val variations: List<String> = listOf(),
    val gradeLevel: Int? = null
)

/**
 * Represents a teaching assignment.
 *
 * @property day The day of the week
 * @property period The period number
 * @property className The class name
 * @property originalTeacher The name of the original teacher
 * @property substitute The name of the substitute teacher (if any)
 */
data class Assignment(
    val day: String,
    val period: Int,
    val className: String,
    val originalTeacher: String,
    val substitute: String = ""
)

/**
 * Represents a substitute teacher assignment.
 *
 * @property originalTeacher The name of the absent teacher
 * @property period The period number
 * @property className The class name
 * @property substitute The name of the substitute teacher
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
 * Represents a verification report for substitute assignments.
 *
 * @property success Whether the verification was successful
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
 * Represents a log entry for the substitute assignment process.
 *
 * @property timestamp The timestamp of the log entry
 * @property action The action being performed
 * @property details Details about the action
 * @property status The status of the action (info, warning, error)
 * @property data Additional data associated with the log entry
 * @property durationMs Duration of the action in milliseconds
 */
data class ProcessLog(
    val timestamp: String,
    val action: String,
    val details: String,
    val status: String, // "info", "warning", or "error"
    val data: Any? = null,
    val durationMs: Long = 0
)