import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

// Function to get the sample teacher data
fun getSampleTeacherData(): List<TeacherWithConstraints> {
    return listOf(
        TeacherWithConstraints(
            name = "Mushtaque Ahmed",
            phone = "+923001111111",
            variations = listOf("Sir Mushtaque", "Mushtaque"),
            maxPeriodsPerDay = 4,
            preferredClasses = listOf("10"),
            qualifiedClasses = listOf("10A", "10B", "9A"),
            unavailablePeriods = emptyList()
        ),
        TeacherWithConstraints(
            name = "Bakir Shah",
            phone = "+923002222222",
            variations = listOf("Sir Bakir", "Bakir"),
            maxPeriodsPerDay = 3,
            preferredClasses = listOf("9"),
            qualifiedClasses = listOf("9A", "9B", "10A"),
            unavailablePeriods = listOf(1)
        )
    )
}

// Function to save teacher data to JSON file
suspend fun saveTeacherDataToFile(
    teachers: List<TeacherWithConstraints> = getSampleTeacherData(),
    filePath: String = "teachers.json"
) {
    val jsonString = Json.encodeToString(teachers)
    File(filePath).writeText(jsonString)
}

// Updated auto-assign algorithm with integrated teacher data
suspend fun assignSubstitutesWithTeacherData(
    day: String,
    absentTeachers: List<String>,
    timetablePath: String,
    substitutesPath: String,
    classesInfoPath: String,
    assignedTeachersPath: String,
    outputPath: String,
    teacherData: List<TeacherWithConstraints> = getSampleTeacherData() // Using the sample data by default
): SubstituteAssignmentResult = withContext(Dispatchers.IO) {
    // Load other data files
    val timetableData = File(timetablePath).readText()
    val substitutesData = File(substitutesPath).readText()
    val classesInfoData = File(classesInfoPath).readText()
    val assignedTeachersData = File(assignedTeachersPath).readText()

    // Parse other data
    val classInfoMap = Json.decodeFromString<Map<String, ClassInfo>>(classesInfoData)
    val existingAssignments = Json.decodeFromString<AssignedTeachersFile>(assignedTeachersData).assignments
    val substitutes = parseSubstitutes(substitutesData, teacherData)
    val timetable = parseEnhancedTimetable(timetableData, teacherData, existingAssignments)

    val warnings = mutableListOf<String>()
    val unassignedPeriods = mutableListOf<SubstituteAssignmentResult.UnassignedPeriod>()
    val newAssignments = mutableListOf<FullAssignment>()

    // Process each absent teacher
    for (absentTeacher in absentTeachers) {
        val normalizedAbsentTeacher = normalizeName(absentTeacher, teacherData)
        val absentPeriods = findAbsentPeriods(normalizedAbsentTeacher, day, timetable)

        if (absentPeriods.isEmpty()) {
            warnings.add("No periods found for $absentTeacher on $day")
            continue
        }

        for ((period, className) in absentPeriods) {
            // Skip if already assigned
            if (existingAssignments.any { it.originalTeacher == normalizedAbsentTeacher && it.period == period }) {
                continue
            }

            try {
                val classInfo = classInfoMap[className] ?: ClassInfo(
                    className,
                    guessGradeLevel(className),
                    "General"
                )

                val substitute = findOptimalSubstituteWithConstraints(
                    day = day,
                    period = period,
                    originalTeacher = normalizedAbsentTeacher,
                    className = className,
                    classInfo = classInfo,
                    timetable = timetable,
                    substitutes = substitutes,
                    allTeachers = teacherData, // Using the provided teacher data
                    existingAssignments = existingAssignments
                )

                val substitutePhone = substitutes.firstOrNull { it.name == substitute }?.phone ?: ""
                newAssignments.add(
                    FullAssignment(
                        originalTeacher = normalizedAbsentTeacher,
                        period = period,
                        className = className,
                        substitute = substitute,
                        substitutePhone = substitutePhone
                    )
                )
            } catch (e: Exception) {
                unassignedPeriods.add(
                    SubstituteAssignmentResult.UnassignedPeriod(
                        originalTeacher = normalizedAbsentTeacher,
                        period = period,
                        className = className,
                        reason = e.message ?: "Unknown error"
                    )
                )
            }
        }
    }

    // Validate assignments
    val (validAssignments, assignmentWarnings) = validateNewAssignments(
        day = day,
        newAssignments = newAssignments,
        existingAssignments = existingAssignments,
        timetable = timetable,
        allTeachers = teacherData // Using the provided teacher data
    )

    warnings.addAll(assignmentWarnings)

    // Create and return result
    SubstituteAssignmentResult(
        assignments = existingAssignments + validAssignments,
        warnings = warnings,
        unassignedPeriods = unassignedPeriods
    )
}

// Helper function to get teacher data as JSON string
fun getTeacherDataJsonString(prettyPrint: Boolean = true): String {
    val teachers = getSampleTeacherData()
    return if (prettyPrint) {
        Json { prettyPrint = true }.encodeToString(teachers)
    } else {
        Json.encodeToString(teachers)
    }
}

// Example usage
suspend fun main() {
    // 1. Get the teacher data as JSON
    val teacherJson = getTeacherDataJsonString()
    println("Teacher Data:\n$teacherJson")

    // 2. Save to file if needed
    saveTeacherDataToFile(filePath = "teachers_config.json")

    // 3. Use in auto-assign algorithm
    val result = assignSubstitutesWithTeacherData(
        day = "Monday",
        absentTeachers = listOf("Mushtaque Ahmed"),
        timetablePath = "timetable.csv",
        substitutesPath = "substitutes.csv",
        classesInfoPath = "classes.json",
        assignedTeachersPath = "existing_assignments.json",
        outputPath = "new_assignments.json",
        // Can pass custom teacher data or use default sample
        teacherData = getSampleTeacherData()
    )

    println("\nAssignment Results:")
    println(Json { prettyPrint = true }.encodeToString(result))
}