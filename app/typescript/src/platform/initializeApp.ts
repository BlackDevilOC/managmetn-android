import { fileSystem } from './FileSystem.js';
import { logger } from './Logger.js';

// Android paths
const ANDROID_BASE_PATH = '/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data';
const ANDROID_RAW_PATH = `${ANDROID_BASE_PATH}/raw`;
const ANDROID_PROCESSED_PATH = `${ANDROID_BASE_PATH}/processed`;
const ANDROID_DATA_PATH = `${ANDROID_BASE_PATH}/data`;
const ANDROID_OLD_LOGS_DIR = `${ANDROID_DATA_PATH}/old_logs`;

// File paths
const TIMETABLE_PATH = `${ANDROID_RAW_PATH}/timetable.csv`;
const SUBSTITUTES_PATH = `${ANDROID_RAW_PATH}/substitute.csv`;
const TEACHERS_PATH = `${ANDROID_PROCESSED_PATH}/total_teacher.json`;
const SCHEDULES_PATH = `${ANDROID_PROCESSED_PATH}/teacher_schedules.json`;
const ASSIGNED_TEACHERS_PATH = `${ANDROID_PROCESSED_PATH}/assigned_teacher.json`;
const LOGS_PATH = `${ANDROID_DATA_PATH}/substitute_logs.json`;
const WARNINGS_PATH = `${ANDROID_DATA_PATH}/substitute_warnings.json`;

/**
 * Initialize the app by creating required directories and files
 */
export async function initializeApp(): Promise<void> {
  try {
    logger.info('Initializing app directories and files');
    
    // Create base directories
    createDirectoryIfNotExists(ANDROID_BASE_PATH);
    createDirectoryIfNotExists(ANDROID_RAW_PATH);
    createDirectoryIfNotExists(ANDROID_PROCESSED_PATH);
    createDirectoryIfNotExists(ANDROID_DATA_PATH);
    createDirectoryIfNotExists(ANDROID_OLD_LOGS_DIR);
    
    // Initialize empty files if they don't exist
    initializeEmptyFileIfNotExists(TEACHERS_PATH, '[]');
    initializeEmptyFileIfNotExists(SCHEDULES_PATH, '{}');
    initializeEmptyFileIfNotExists(ASSIGNED_TEACHERS_PATH, '[]');
    initializeEmptyFileIfNotExists(LOGS_PATH, '{}');
    initializeEmptyFileIfNotExists(WARNINGS_PATH, '{}');
    
    logger.info('App initialization completed successfully');
  } catch (error) {
    logger.error('Error initializing app', { error: error instanceof Error ? error.message : String(error) });
    throw error;
  }
}

/**
 * Create a directory if it doesn't exist
 */
function createDirectoryIfNotExists(dirPath: string): void {
  if (!fileSystem.exists(dirPath)) {
    logger.info(`Creating directory: ${dirPath}`);
    fileSystem.mkdir(dirPath, { recursive: true });
  }
}

/**
 * Initialize an empty file if it doesn't exist
 */
function initializeEmptyFileIfNotExists(filePath: string, defaultContent: string): void {
  if (!fileSystem.exists(filePath)) {
    logger.info(`Creating empty file: ${filePath}`);
    fileSystem.writeFile(filePath, defaultContent);
  }
} 