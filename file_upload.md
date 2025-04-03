# Substitute Management System - Implementation Guide

## 1. Overview

The Substitute Management System is designed to:
- Process teacher timetables and substitute information
- Manage teacher schedules and substitutions
- Handle teacher data normalization and consolidation
- Generate and manage substitute assignments

## 2. System Architecture

### 2.1 Core Components
- File Processing Module
- Data Storage Module
- Teacher Management Module
- Schedule Management Module
- UI Components

### 2.2 Data Flow
1. File Upload → Processing → Storage → UI Update
2. Teacher Data → Normalization → Consolidation → Storage
3. Schedule Data → Processing → Assignment → Storage

## 3. Implementation Details

### 3.1 File Processing Module

#### 3.1.1 File Upload Handler
```typescript
// React/TypeScript Implementation
interface FileUploadProps {
  onFileSelect: (file: File) => void;
  accept: string;
}

const FileUpload: React.FC<FileUploadProps> = ({ onFileSelect, accept }) => {
  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (e) => {
        const content = e.target?.result as string;
        onFileSelect(file);
        processFileContent(content, file.type);
      };
      reader.readAsText(file);
    }
  };

  return (
    <input
      type="file"
      accept={accept}
      onChange={handleFileChange}
      className="file-input"
    />
  );
};
```

#### 3.1.2 File Content Processor
```typescript
// TypeScript Implementation
class FileProcessor {
  private static instance: FileProcessor;
  private teacherMap: Map<string, Teacher> = new Map();

  private constructor() {}

  static getInstance(): FileProcessor {
    if (!FileProcessor.instance) {
      FileProcessor.instance = new FileProcessor();
    }
    return FileProcessor.instance;
  }

  async processTimetableFile(content: string): Promise<ProcessedData> {
    try {
      const records = this.parseCSV(content);
      const schedules: Schedule[] = [];
      const teachers: Teacher[] = [];

      // Process each row
      for (let i = 1; i < records.length; i++) {
        const row = records[i];
        const day = this.normalizeDay(row[0]);
        const period = parseInt(row[1]);

        // Process teacher assignments
        for (let j = 2; j < row.length; j++) {
          const teacherName = row[j].trim();
          if (teacherName && teacherName.toLowerCase() !== 'empty') {
            const teacher = await this.findOrCreateTeacher(teacherName);
            schedules.push({
              id: 0,
              day,
              period,
              teacherId: teacher.id,
              className: this.validClasses[j - 2]
            });
          }
        }
      }

      return { schedules, teachers: Array.from(this.teacherMap.values()) };
    } catch (error) {
      throw new ProcessingError('Failed to process timetable file', error);
    }
  }
}
```

### 3.2 Data Storage Module

#### 3.2.1 Local Storage Implementation
```kotlin
// Kotlin Implementation
class LocalStorageManager(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences(
        "substitute_data",
        Context.MODE_PRIVATE
    )

    fun saveTeachers(teachers: List<Teacher>) {
        val json = Gson().toJson(teachers)
        sharedPreferences.edit().putString("teachers", json).apply()
    }

    fun getTeachers(): List<Teacher> {
        val json = sharedPreferences.getString("teachers", "[]")
        return Gson().fromJson(json, Array<Teacher>::class.java).toList()
    }

    fun saveSchedules(schedules: List<Schedule>) {
        val json = Gson().toJson(schedules)
        sharedPreferences.edit().putString("schedules", json).apply()
    }

    fun getSchedules(): List<Schedule> {
        val json = sharedPreferences.getString("schedules", "[]")
        return Gson().fromJson(json, Array<Schedule>::class.java).toList()
    }
}
```

#### 3.2.2 Room Database Implementation
```kotlin
// Kotlin Implementation
@Entity(tableName = "teachers")
data class TeacherEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val phoneNumber: String?,
    val isSubstitute: Boolean,
    val variations: String
)

@Dao
interface TeacherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: TeacherEntity)

    @Query("SELECT * FROM teachers")
    suspend fun getAllTeachers(): List<TeacherEntity>

    @Query("SELECT * FROM teachers WHERE isSubstitute = 1")
    suspend fun getSubstituteTeachers(): List<TeacherEntity>
}

@Database(entities = [TeacherEntity::class, ScheduleEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun teacherDao(): TeacherDao
    abstract fun scheduleDao(): ScheduleDao
}
```

### 3.3 Teacher Management Module

#### 3.3.1 Teacher Data Processing
```kotlin
// Kotlin Implementation
class TeacherManager(private val storage: LocalStorageManager) {
    private val teacherNormalizer = TeacherNormalizer()

    suspend fun processTeacherData(
        timetableContent: String,
        substituteContent: String
    ): Result<ProcessedData> = withContext(Dispatchers.IO) {
        try {
            // Process timetable teachers
            val timetableTeachers = processTimetableTeachers(timetableContent)
            
            // Process substitute teachers
            val substituteTeachers = processSubstituteTeachers(substituteContent)
            
            // Consolidate teacher data
            val consolidatedTeachers = consolidateTeachers(
                timetableTeachers,
                substituteTeachers
            )
            
            // Save processed data
            storage.saveTeachers(consolidatedTeachers)
            
            Result.success(ProcessedData(consolidatedTeachers))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun normalizeTeacherName(name: String): String {
        return name
            .lowercase()
            .trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[^a-z\\s]"), "")
    }
}
```

### 3.4 Schedule Management Module

#### 3.4.1 Schedule Processing
```kotlin
// Kotlin Implementation
class ScheduleManager(private val storage: LocalStorageManager) {
    suspend fun processSchedules(
        timetableContent: String,
        teachers: List<Teacher>
    ): Result<List<Schedule>> = withContext(Dispatchers.IO) {
        try {
            val schedules = mutableListOf<Schedule>()
            val records = parseCSV(timetableContent)
            
            for (i in 1 until records.size) {
                val row = records[i]
                val day = normalizeDay(row[0])
                val period = row[1].toInt()
                
                for (j in 2 until row.size) {
                    val teacherName = row[j].trim()
                    if (teacherName.isNotEmpty() && teacherName.lowercase() != "empty") {
                        val teacher = findTeacherByName(teachers, teacherName)
                        teacher?.let {
                            schedules.add(Schedule(
                                id = 0,
                                day = day,
                                period = period,
                                teacherId = it.id,
                                className = validClasses[j - 2]
                            ))
                        }
                    }
                }
            }
            
            storage.saveSchedules(schedules)
            Result.success(schedules)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

## 4. Error Handling

### 4.1 Error Types
```kotlin
sealed class ProcessingError : Exception() {
    data class FileError(val message: String) : ProcessingError()
    data class DataError(val message: String) : ProcessingError()
    data class StorageError(val message: String) : ProcessingError()
    data class ValidationError(val message: String) : ProcessingError()
}
```

### 4.2 Error Handling Implementation
```kotlin
class ErrorHandler {
    fun handleError(error: ProcessingError) {
        when (error) {
            is ProcessingError.FileError -> {
                // Handle file-related errors
                Log.e("FileError", error.message)
            }
            is ProcessingError.DataError -> {
                // Handle data processing errors
                Log.e("DataError", error.message)
            }
            is ProcessingError.StorageError -> {
                // Handle storage errors
                Log.e("StorageError", error.message)
            }
            is ProcessingError.ValidationError -> {
                // Handle validation errors
                Log.e("ValidationError", error.message)
            }
        }
    }
}
```

## 5. UI Implementation

### 5.1 Main Activity
```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: MainViewModel
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.processButton.setOnClickListener {
            viewModel.processFiles()
        }
    }

    private fun observeViewModel() {
        viewModel.state.observe(this) { state ->
            updateUI(state)
        }
    }
}
```

### 5.2 ViewModel
```kotlin
class MainViewModel : ViewModel() {
    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val fileProcessor = FileProcessor()
    private val storage = LocalStorageManager()

    fun processFiles() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            
            try {
                val result = fileProcessor.processFiles(
                    timetableContent = getTimetableContent(),
                    substituteContent = getSubstituteContent()
                )
                
                result.fold(
                    onSuccess = { data ->
                        storage.saveProcessedData(data)
                        _state.value = UiState.Success(data)
                    },
                    onFailure = { error ->
                        _state.value = UiState.Error(error.message ?: "Unknown error")
                    }
                )
            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
```

## 6. Required Dependencies

### 6.1 Gradle Dependencies
```gradle
dependencies {
    // Room Database
    implementation "androidx.room:room-runtime:2.5.0"
    implementation "androidx.room:room-ktx:2.5.0"
    kapt "androidx.room:room-compiler:2.5.0"
    
    // Coroutines
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4"
    
    // ViewModel
    implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1"
    
    // Gson
    implementation "com.google.code.gson:gson:2.9.0"
}
```

## 7. Testing

### 7.1 Unit Tests
```kotlin
class FileProcessorTest {
    private lateinit var fileProcessor: FileProcessor
    
    @Before
    fun setup() {
        fileProcessor = FileProcessor()
    }
    
    @Test
    fun `process timetable file successfully`() = runTest {
        val content = """
            Day,Period,6a,6b
            Monday,1,John Doe,Jane Smith
        """.trimIndent()
        
        val result = fileProcessor.processTimetableFile(content)
        
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.schedules?.size)
    }
}
```

## 8. Best Practices

1. **Data Processing**
   - Always validate input data
   - Use appropriate data structures
   - Handle edge cases

2. **Storage**
   - Implement proper data migration
   - Use transactions for multiple operations
   - Handle concurrent access

3. **Error Handling**
   - Use sealed classes for error types
   - Provide meaningful error messages
   - Log errors appropriately

4. **Performance**
   - Use coroutines for background tasks
   - Implement proper caching
   - Optimize database queries 

## 9. Offline Storage and File Management

### 9.1 File Storage Structure

The app uses a dedicated directory structure for offline storage:

```
Android/data/com.your.app.package/
└── files/
    └── substitute_data/
        ├── raw/
        │   ├── timetable_file.csv
        │   └── substitute_file.csv
        ├── processed/
        │   ├── teachers.json
        │   ├── schedules.json
        │   └── teacher_schedules.json
        ├── backups/
        │   └── YYYY-MM-DD/
        │       ├── teachers_backup.json
        │       └── schedules_backup.json
        └── logs/
            └── processing_logs.txt
```

### 9.2 File Storage Implementation

```kotlin
class FileStorageManager(private val context: Context) {
    private val baseDir = File(context.getExternalFilesDir(null), "substitute_data")
    private val rawDir = File(baseDir, "raw")
    private val processedDir = File(baseDir, "processed")
    private val backupsDir = File(baseDir, "backups")
    private val logsDir = File(baseDir, "logs")

    init {
        // Create necessary directories
        listOf(baseDir, rawDir, processedDir, backupsDir, logsDir).forEach { dir ->
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
    }

    // Save raw files
    fun saveRawFile(fileType: FileType, content: String) {
        val file = when (fileType) {
            FileType.TIMETABLE -> File(rawDir, "timetable_file.csv")
            FileType.SUBSTITUTE -> File(rawDir, "substitute_file.csv")
        }
        file.writeText(content)
    }

    // Save processed data
    fun saveProcessedData(data: ProcessedData) {
        // Save teachers
        File(processedDir, "teachers.json").writeText(
            Gson().toJson(data.teachers)
        )
        
        // Save schedules
        File(processedDir, "schedules.json").writeText(
            Gson().toJson(data.schedules)
        )
        
        // Save teacher schedules
        File(processedDir, "teacher_schedules.json").writeText(
            Gson().toJson(data.teacherSchedules)
        )
    }

    // Create backup
    fun createBackup() {
        val backupDir = File(backupsDir, LocalDate.now().toString())
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }

        // Copy all processed files to backup
        processedDir.listFiles()?.forEach { file ->
            file.copyTo(File(backupDir, file.name))
        }
    }

    // Log processing
    fun logProcessing(message: String) {
        val logFile = File(logsDir, "processing_logs.txt")
        logFile.appendText("${LocalDateTime.now()}: $message\n")
    }
}

enum class FileType {
    TIMETABLE,
    SUBSTITUTE
}
```

### 9.3 Offline Data Access

```kotlin
class OfflineDataManager(private val fileStorage: FileStorageManager) {
    // Load all processed data
    fun loadProcessedData(): ProcessedData? {
        return try {
            val teachers = loadTeachers()
            val schedules = loadSchedules()
            val teacherSchedules = loadTeacherSchedules()
            
            if (teachers != null && schedules != null && teacherSchedules != null) {
                ProcessedData(teachers, schedules, teacherSchedules)
            } else {
                null
            }
        } catch (e: Exception) {
            fileStorage.logProcessing("Error loading processed data: ${e.message}")
            null
        }
    }

    private fun loadTeachers(): List<Teacher>? {
        return try {
            val file = File(fileStorage.processedDir, "teachers.json")
            if (file.exists()) {
                Gson().fromJson(file.readText(), Array<Teacher>::class.java).toList()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Similar methods for schedules and teacher schedules
}
```

### 9.4 Data Synchronization

```kotlin
class DataSynchronizer(private val fileStorage: FileStorageManager) {
    private val syncLock = Mutex()

    suspend fun syncData() = withContext(Dispatchers.IO) {
        syncLock.withLock {
            try {
                // 1. Check for new raw files
                val timetableFile = File(fileStorage.rawDir, "timetable_file.csv")
                val substituteFile = File(fileStorage.rawDir, "substitute_file.csv")

                if (timetableFile.exists() && substituteFile.exists()) {
                    // 2. Process files
                    val processor = FileProcessor()
                    val result = processor.processFiles(
                        timetableFile.readText(),
                        substituteFile.readText()
                    )

                    // 3. Save processed data
                    result.fold(
                        onSuccess = { data ->
                            fileStorage.saveProcessedData(data)
                            fileStorage.createBackup()
                            fileStorage.logProcessing("Data processed and saved successfully")
                        },
                        onFailure = { error ->
                            fileStorage.logProcessing("Error processing data: ${error.message}")
                        }
                    )
                }
            } catch (e: Exception) {
                fileStorage.logProcessing("Sync error: ${e.message}")
            }
        }
    }
}
```

### 9.5 File Management Best Practices

1. **Directory Structure**
   - Use clear, hierarchical directory structure
   - Separate raw and processed data
   - Maintain backup copies
   - Keep processing logs

2. **File Operations**
   - Use atomic file operations
   - Implement proper error handling
   - Maintain file consistency
   - Handle concurrent access

3. **Storage Management**
   - Implement cleanup routines
   - Manage backup retention
   - Monitor storage usage
   - Handle storage permissions

4. **Data Integrity**
   - Validate file contents
   - Implement checksums
   - Maintain data consistency
   - Handle file corruption

### 9.6 Implementation Example

```kotlin
class MainViewModel : ViewModel() {
    private val fileStorage = FileStorageManager(context)
    private val offlineData = OfflineDataManager(fileStorage)
    private val dataSynchronizer = DataSynchronizer(fileStorage)

    fun processFiles(timetableContent: String, substituteContent: String) {
        viewModelScope.launch {
            try {
                // 1. Save raw files
                fileStorage.saveRawFile(FileType.TIMETABLE, timetableContent)
                fileStorage.saveRawFile(FileType.SUBSTITUTE, substituteContent)

                // 2. Process and sync data
                dataSynchronizer.syncData()

                // 3. Load processed data
                val processedData = offlineData.loadProcessedData()
                if (processedData != null) {
                    _state.value = UiState.Success(processedData)
                } else {
                    _state.value = UiState.Error("Failed to process data")
                }
            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
```

### 9.7 Error Handling for Offline Storage

```kotlin
sealed class StorageError : Exception() {
    data class FileNotFound(val path: String) : StorageError()
    data class PermissionDenied(val message: String) : StorageError()
    data class StorageFull(val availableSpace: Long) : StorageError()
    data class FileCorrupted(val path: String) : StorageError()
}

class StorageErrorHandler {
    fun handleError(error: StorageError) {
        when (error) {
            is StorageError.FileNotFound -> {
                // Handle missing file
                Log.e("Storage", "File not found: ${error.path}")
            }
            is StorageError.PermissionDenied -> {
                // Handle permission issues
                Log.e("Storage", "Permission denied: ${error.message}")
            }
            is StorageError.StorageFull -> {
                // Handle storage space issues
                Log.e("Storage", "Storage full. Available: ${error.availableSpace}")
            }
            is StorageError.FileCorrupted -> {
                // Handle corrupted files
                Log.e("Storage", "File corrupted: ${error.path}")
            }
        }
    }
} 