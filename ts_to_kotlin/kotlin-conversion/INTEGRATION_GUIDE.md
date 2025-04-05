# Substitute Manager - Android Integration Guide

This guide explains how to integrate the Substitute Manager algorithm into your Android application.

## Prerequisites

- Android Studio 4.1 or later
- Android SDK 24 or higher
- Kotlin 1.6 or later
- Gradle 7.0 or later

## Step 1: Project Setup

1. Copy the Kotlin files from this project into your Android application:
   - `Models.kt`
   - `SubstituteManager.kt`
   - `CsvParser.kt`

2. Add the necessary dependencies to your app-level `build.gradle` file:

```gradle
dependencies {
    // Existing dependencies...
    
    // Required for SubstituteManager
    implementation 'com.google.code.gson:gson:2.10.1'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4'
}
```

## Step 2: Data Files Setup

1. Create an `assets` folder in your Android project if it doesn't exist already
2. Create a `data` subfolder inside the assets folder
3. Add the following data files to the `assets/data` folder:
   - `timetable_file.csv` - Class schedule data
   - `Substitude_file.csv` - Substitute teacher list
   - `total_teacher.json` - Complete teacher list
   - `teacher_schedules.json` - Teacher schedules
   - `assigned_teacher.json` - Previously assigned substitutes

The file formats should match the original TypeScript implementation:

### timetable_file.csv
```
Day,Period,Teacher1,Teacher2,Teacher3,...
Monday,1,John Smith,Jane Doe,Bob Johnson,...
Monday,2,Jane Doe,John Smith,Alice Brown,...
...
```

### Substitude_file.csv
```
Sarah Parker,+1234567890
Michael Lee,+2345678901
Lisa Taylor,+3456789012
...
```

### total_teacher.json
```json
[
  {"name":"John Smith","phone":"+1111111111","isSubstitute":false},
  {"name":"Jane Doe","phone":"+2222222222","isSubstitute":false},
  {"name":"Sarah Parker","phone":"+1234567890","isSubstitute":true},
  ...
]
```

### teacher_schedules.json
```json
{
  "john smith": [
    {"day":"monday","period":1,"className":"10A","originalTeacher":"john smith","substitute":""},
    {"day":"tuesday","period":1,"className":"9C","originalTeacher":"john smith","substitute":""}
  ],
  "jane doe": [
    {"day":"monday","period":2,"className":"10B","originalTeacher":"jane doe","substitute":""},
    ...
  ],
  ...
}
```

### assigned_teacher.json
```json
{
  "assignments": [
    {
      "originalTeacher": "John Smith",
      "period": 1,
      "className": "10A",
      "substitute": "Sarah Parker",
      "substitutePhone": "+1234567890"
    },
    ...
  ],
  "warnings": []
}
```

## Step 3: Usage in Your Activity or Fragment

Here's a basic example of using the SubstituteManager in an Activity:

```kotlin
class YourActivity : AppCompatActivity() {
    private lateinit var substituteManager: SubstituteManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.your_layout)
        
        // Initialize the SubstituteManager
        substituteManager = SubstituteManager(this)
        
        // Load data in a coroutine
        lifecycleScope.launch {
            try {
                // Show loading indicator
                showLoading(true)
                
                // Load data (this might take a moment)
                substituteManager.loadData()
                
                // Hide loading indicator
                showLoading(false)
                
                // Update UI with loaded data
                updateUI()
            } catch (e: Exception) {
                // Handle error
                showError("Error loading data: ${e.message}")
            }
        }
    }
    
    private fun assignSubstitute(teacherName: String, day: String) {
        lifecycleScope.launch {
            try {
                val assignments = substituteManager.assignSubstitutes(teacherName, day)
                
                // Display the assignments
                displayAssignments(assignments)
            } catch (e: Exception) {
                showError("Error assigning substitutes: ${e.message}")
            }
        }
    }
    
    private fun clearAllAssignments() {
        lifecycleScope.launch {
            substituteManager.clearAssignments()
            updateUI()
        }
    }
    
    private fun verifyAssignments() {
        val reports = substituteManager.verifyAssignments()
        
        // Process verification reports
        val hasIssues = reports.any { !it.success }
        if (hasIssues) {
            val issues = reports.flatMap { it.issues }
            displayIssues(issues)
        }
    }
}
```

## Step 4: Customization

If you need to customize the algorithm behavior:

1. Modify the constants in `SubstituteManager.kt`:
   - `maxSubstituteAssignments` - Maximum assignments per substitute
   - `maxRegularTeacherAssignments` - Maximum assignments for regular teachers

2. Extend the data models in `Models.kt` if needed

3. Customize file paths in the `loadData()` method if your files are in different locations

## Step 5: Testing

Refer to `SubstituteManagerTest.kt` for examples of how to test the algorithm. You can:

1. Test with mock data
2. Verify algorithm correctness
3. Test edge cases

## Troubleshooting

- **File Loading Issues**: Ensure your data files are correctly placed in the assets directory
- **JSON Parsing Errors**: Verify your JSON files match the expected format
- **Memory Issues**: The algorithm keeps data in memory, so very large datasets might require optimization

## Advanced Usage

### Custom Data Sources

If you're loading data from a different source (API, database), modify the `loadData()` method:

```kotlin
// Example of loading from Room database
suspend fun loadDataFromDatabase(database: YourDatabase) {
    val teachers = database.teacherDao().getAllTeachers()
    val schedules = database.scheduleDao().getAllSchedules()
    
    allTeachers.clear()
    allTeachers.addAll(teachers)
    
    // Transform data as needed...
    
    // Process schedules
    teacherClasses.clear()
    for (teacher in teachers) {
        val teacherSchedules = schedules
            .filter { it.teacherId == teacher.id }
            .map { schedule ->
                Assignment(
                    day = schedule.day.lowercase(),
                    period = schedule.period,
                    className = schedule.className,
                    originalTeacher = teacher.name.lowercase(),
                    substitute = ""
                )
            }
        
        if (teacherSchedules.isNotEmpty()) {
            teacherClasses[teacher.name.lowercase()] = teacherSchedules
        }
    }
    
    // Initialize other data structures...
}
```

### Integration with WorkManager

For background processing, consider using WorkManager:

```kotlin
class SubstituteAssignmentWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        val teacherName = inputData.getString("teacherName") ?: return Result.failure()
        val day = inputData.getString("day") ?: return Result.failure()
        
        return try {
            val substituteManager = SubstituteManager(applicationContext)
            substituteManager.loadData()
            val assignments = substituteManager.assignSubstitutes(teacherName, day)
            
            // Create output data
            val outputData = workDataOf(
                "assignmentCount" to assignments.size,
                "success" to true
            )
            
            Result.success(outputData)
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
```
