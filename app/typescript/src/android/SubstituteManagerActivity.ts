import { SubstituteManager } from '../substitute-manager.js';
import { logger } from '../platform/Logger.js';
import { fileSystem } from '../platform/FileSystem.js';

// Android paths
const ANDROID_BASE_PATH = '/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data';
const ANDROID_RAW_PATH = `${ANDROID_BASE_PATH}/raw`;
const ANDROID_PROCESSED_PATH = `${ANDROID_BASE_PATH}/processed`;

// File paths
const TIMETABLE_PATH = `${ANDROID_RAW_PATH}/timetable.csv`;
const SUBSTITUTES_PATH = `${ANDROID_RAW_PATH}/substitute.csv`;

/**
 * Android activity for managing substitutes
 */
export class SubstituteManagerActivity {
  private manager: SubstituteManager;
  private isInitialized: boolean = false;

  constructor() {
    this.manager = new SubstituteManager();
  }

  /**
   * Initialize the activity
   */
  async initialize(): Promise<void> {
    try {
      logger.info('Initializing SubstituteManagerActivity');
      
      // Check if the required files exist
      if (!fileSystem.exists(TIMETABLE_PATH)) {
        logger.warn(`Timetable file not found at ${TIMETABLE_PATH}`);
        // You might want to prompt the user to upload the file
      }
      
      if (!fileSystem.exists(SUBSTITUTES_PATH)) {
        logger.warn(`Substitutes file not found at ${SUBSTITUTES_PATH}`);
        // You might want to prompt the user to upload the file
      }
      
      // Load data if files exist
      if (fileSystem.exists(TIMETABLE_PATH) && fileSystem.exists(SUBSTITUTES_PATH)) {
        await this.manager.loadData(TIMETABLE_PATH, SUBSTITUTES_PATH);
        logger.info('Data loaded successfully');
      }
      
      this.isInitialized = true;
      logger.info('SubstituteManagerActivity initialized');
    } catch (error) {
      logger.error('Error initializing SubstituteManagerActivity', { error: error instanceof Error ? error.message : String(error) });
      throw error;
    }
  }

  /**
   * Process substitutes for a specific date
   */
  async processSubstitutes(date: string, absentTeachers: string[]): Promise<any> {
    try {
      if (!this.isInitialized) {
        await this.initialize();
      }
      
      logger.info(`Processing substitutes for date: ${date}`, { absentTeachers });
      
      const result = await this.manager.autoAssignSubstitutes(date, absentTeachers);
      
      logger.info(`Processed ${result.assignments.length} assignments`, { 
        warnings: result.warnings.length,
        logs: result.logs.length
      });
      
      return result;
    } catch (error) {
      logger.error('Error processing substitutes', { error: error instanceof Error ? error.message : String(error) });
      throw error;
    }
  }

  /**
   * Get all substitute assignments
   */
  getSubstituteAssignments(): any {
    try {
      if (!this.isInitialized) {
        logger.warn('SubstituteManagerActivity not initialized');
        return { assignments: [] };
      }
      
      return this.manager.getSubstituteAssignments();
    } catch (error) {
      logger.error('Error getting substitute assignments', { error: error instanceof Error ? error.message : String(error) });
      throw error;
    }
  }

  /**
   * Verify assignments
   */
  verifyAssignments(): any {
    try {
      if (!this.isInitialized) {
        logger.warn('SubstituteManagerActivity not initialized');
        return [];
      }
      
      return this.manager.verifyAssignments();
    } catch (error) {
      logger.error('Error verifying assignments', { error: error instanceof Error ? error.message : String(error) });
      throw error;
    }
  }

  /**
   * Clear all assignments
   */
  clearAssignments(): void {
    try {
      if (!this.isInitialized) {
        logger.warn('SubstituteManagerActivity not initialized');
        return;
      }
      
      this.manager.clearAssignments();
      logger.info('All assignments cleared');
    } catch (error) {
      logger.error('Error clearing assignments', { error: error instanceof Error ? error.message : String(error) });
      throw error;
    }
  }
} 