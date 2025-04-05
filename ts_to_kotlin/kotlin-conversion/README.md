# Substitute Teacher Manager - Kotlin Implementation

This is a Kotlin implementation of the Substitute Teacher Manager algorithm, directly ported from the TypeScript version. It maintains the same functionality and business logic.

## Overview

The Substitute Teacher Manager handles the assignment of substitute teachers when regular teachers are absent. It takes into account various constraints such as:

- Teacher availability
- Previous assignments
- Workload distribution
- Period conflicts

## File Structure

- `Models.kt` - Data classes representing teachers, assignments, etc.
- `SubstituteManager.kt` - The main algorithm implementation
- `SubstituteManagerTest.kt` - Unit tests for the algorithm

## Usage

### Setup

1. Copy the Kotlin files into your Android project
2. Make sure the necessary data files are available in your Android assets:
   - `data/timetable_file.csv` - Class schedule
   - `data/Substitude_file.csv` - List of substitute teachers
   - `data/total_teacher.json` - Complete list of teachers
   - `data/teacher_schedules.json` - Teacher schedules by name
   - `data/assigned_teacher.json` - Previously assigned substitutes

### Basic Usage

```kotlin
// Create an instance
val substituteManager = SubstituteManager(context)

// Load data (typically called once at startup)
lifecycleScope.launch {
    substituteManager.loadData()
}

// Assign substitutes for an absent teacher
val assignments = substituteManager.assignSubstitutes("Teacher Name", "Monday")

// Get all current assignments
val allAssignments = substituteManager.getSubstituteAssignments()

// Verify assignments are valid
val verificationReports = substituteManager.verifyAssignments()

// Clear all assignments
substituteManager.clearAssignments()
```

## Data Storage

This implementation uses Android's SharedPreferences to persist assignment data. This replaces the localStorage used in the TypeScript web implementation.

## Implementation Notes

1. The Kotlin version uses coroutines for asynchronous operations
2. Data is loaded from Android assets rather than via HTTP requests
3. The class structure is very similar to the TypeScript version to maintain compatibility
4. Error handling follows Android conventions with proper logging

## Android Integration

To integrate this into your Android application:

1. Ensure the necessary dependencies are in your `build.gradle`:
   - `implementation 'com.google.code.gson:gson:2.10.1'`
   - Other standard Android libraries

2. Add the data files to your assets directory

3. Initialize the SubstituteManager in your Activity or ViewModel:

```kotlin
class SubstitutionActivity : AppCompatActivity() {
    private lateinit var substituteManager: SubstituteManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_substitution)
        
        substituteManager = SubstituteManager(this)
        
        lifecycleScope.launch {
            try {
                substituteManager.loadData()
                // UI is ready to use
            } catch (e: Exception) {
                // Handle data loading error
            }
        }
    }
}
```
