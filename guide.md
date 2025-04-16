Substitute Teacher Assignment System
Overview
This system automatically assigns substitute teachers while considering:

Teacher availability and qualifications

Class grade levels (6-8 vs 9-10)

Existing assignments

Workload constraints

Period scheduling requirements

Data Structures
1. Teacher Information (TeacherWithConstraints)
kotlin
Copy
@Serializable
data class TeacherWithConstraints(
    val name: String,
    val phone: String,
    val variations: List<String>,
    val maxPeriodsPerDay: Int = 4,
    val preferredClasses: List<String> = emptyList(),
    val qualifiedClasses: List<String> = emptyList(),
    val unavailablePeriods: List<Int> = emptyList(),
    val minPeriodsGap: Int = 1
)
2. Class Information (ClassInfo)
kotlin
Copy
@Serializable
data class ClassInfo(
    val className: String,
    val gradeLevel: Int,
    val subject: String,
    val difficulty: String = "normal",
    val specialRequirements: List<String> = emptyList()
)
3. Assignment Records (FullAssignment)
kotlin
Copy
@Serializable
data class FullAssignment(
    val originalTeacher: String,
    val period: Int,
    val className: String,
    val substitute: String,
    val substitutePhone: String
)
Core Functions
1. Main Assignment Function
kotlin
Copy
suspend fun assignSubstitutesWithFullContext(
    day: String,
    absentTeachers: List<String>,
    timetablePath: String,
    substitutesPath: String,
    teachersDetailsPath: String,
    classesInfoPath: String,
    assignedTeachersPath: String,
    outputPath: String
): SubstituteAssignmentResult
Parameters:

Parameter	Type	Description
day	String	School day (e.g., "Monday")
absentTeachers	List<String>	Names of absent teachers
timetablePath	String	Path to timetable CSV file
substitutesPath	String	Path to substitutes CSV file
teachersDetailsPath	String	Path to teachers JSON file
classesInfoPath	String	Path to class info JSON file
assignedTeachersPath	String	Path to existing assignments JSON file
outputPath	String	Path to save output JSON file
2. Sample Data Generation
kotlin
Copy
fun getSampleTeacherData(): List<TeacherWithConstraints>
Returns sample teacher data with all constraints.

3. Data Export
kotlin
Copy
suspend fun saveTeacherDataToFile(
    teachers: List<TeacherWithConstraints> = getSampleTeacherData(),
    filePath: String = "teachers.json"
)
File Formats
1. Timetable CSV Format
Copy
Day,Period,Teacher1,Teacher2
Monday,1,Mushtaque Ahmed,Faisal Ali
Monday,2,Mushtaque Ahmed,Waqar Ali
...
2. Substitutes CSV Format
Copy
Name,Phone
Waqar Ali,+923001234567
Faisal Ali,+923451234567
...
3. Teachers JSON Format
json
Copy
[
  {
    "name": "Mushtaque Ahmed",
    "phone": "+923001111111",
    "variations": ["Sir Mushtaque", "Mushtaque"],
    "maxPeriodsPerDay": 4,
    "preferredClasses": ["10"],
    "qualifiedClasses": ["10A", "10B", "9A"],
    "unavailablePeriods": []
  }
]
Business Rules
Grade Level Restrictions:

9th/10th grade teachers won't be assigned to 6th-8th grades unless:

No other substitutes available

Teacher is explicitly qualified

Workload Management:

Default max periods/day: 4

Configurable per teacher

Period Constraints:

Minimum gap between classes configurable

Unavailable periods respected

Qualification Checks:

Subject-specific qualifications

Class-level qualifications

Implementation Steps
Set up data files:

Create teachers.json with all teacher constraints

Prepare timetable.csv with schedule

Create substitutes.csv with available substitutes

Initialize the system:

kotlin
Copy
// Get sample data
val teachers = getSampleTeacherData()

// Or load from file
val teachers = Json.decodeFromString<List<TeacherWithConstraints>>(
    File("teachers.json").readText()
)
Run assignments:

kotlin
Copy
val result = assignSubstitutesWithFullContext(
    day = "Monday",
    absentTeachers = listOf("Mushtaque Ahmed"),
    timetablePath = "timetable.csv",
    substitutesPath = "substitutes.csv",
    teachersDetailsPath = "teachers.json",
    classesInfoPath = "classes.json",
    assignedTeachersPath = "existing_assignments.json",
    outputPath = "new_assignments.json"
)
Expected Output
json
Copy
{
  "assignments": [
    {
      "originalTeacher": "Mushtaque Ahmed",
      "period": 1,
      "className": "10B",
      "substitute": "Waqar Ali",
      "substitutePhone": "+923001234567"
    }
  ],
  "warnings": [],
  "unassignedPeriods": []
}
Error Handling
The system will:

Return warnings for non-critical issues

List unassigned periods with reasons

Throw exceptions for fatal errors

Test Cases
Normal Assignment:

Input: 1 absent teacher with 2 periods

Expect: 2 assignments with qualified substitutes

Overload Prevention:

Input: Substitute already at max periods

Expect: Different substitute assigned

Grade Level Conflict:

Input: 10th grade teacher absent for 7th grade class

Expect: Only qualified substitutes assigned

This documentation provides all the necessary information for your AI developer to implement the system. It includes:

Data structure definitions

Core function specifications

File format requirements

Business rules

Implementation examples

Expected outputs

The AI can use this to generate the complete implementation in your preferred framework/language while maintaining all the business logic and constraints.

