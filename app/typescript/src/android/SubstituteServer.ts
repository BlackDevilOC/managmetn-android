import { SubstituteManagerActivity } from './SubstituteManagerActivity.js';
import { logger } from '../platform/Logger.js';
import { fileSystem } from '../platform/FileSystem.js';

// Android paths
const ANDROID_BASE_PATH = '/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data';
const ANDROID_RAW_PATH = `${ANDROID_BASE_PATH}/raw`;

// File paths
const TIMETABLE_PATH = `${ANDROID_RAW_PATH}/timetable.csv`;
const SUBSTITUTES_PATH = `${ANDROID_RAW_PATH}/substitute.csv`;

/**
 * Simple server to handle API requests from the Android app
 */
export class SubstituteServer {
  private activity: SubstituteManagerActivity;
  private isRunning: boolean = false;
  private port: number;

  constructor(port: number = 3000) {
    this.activity = new SubstituteManagerActivity();
    this.port = port;
  }

  /**
   * Start the server
   */
  async start(): Promise<void> {
    try {
      logger.info(`Starting SubstituteServer on port ${this.port}`);
      
      // Initialize the activity
      await this.activity.initialize();
      
      // Start the server
      this.isRunning = true;
      
      // In a real implementation, you would start an HTTP server here
      // For now, we'll just log that the server is running
      logger.info('SubstituteServer is running');
      
      // Example of how to handle requests:
      // this.handleRequest('/api/process', this.handleProcessRequest);
      // this.handleRequest('/api/assignments', this.handleAssignmentsRequest);
      // this.handleRequest('/api/verify', this.handleVerifyRequest);
      // this.handleRequest('/api/clear', this.handleClearRequest);
      
    } catch (error) {
      logger.error('Error starting SubstituteServer', { error: error instanceof Error ? error.message : String(error) });
      throw error;
    }
  }

  /**
   * Stop the server
   */
  stop(): void {
    try {
      logger.info('Stopping SubstituteServer');
      this.isRunning = false;
      logger.info('SubstituteServer stopped');
    } catch (error) {
      logger.error('Error stopping SubstituteServer', { error: error instanceof Error ? error.message : String(error) });
      throw error;
    }
  }

  /**
   * Handle a process request
   */
  async handleProcessRequest(request: any): Promise<any> {
    try {
      const { date, absentTeachers } = request;
      
      if (!date) {
        return { error: 'Date is required' };
      }
      
      if (!absentTeachers || !Array.isArray(absentTeachers) || absentTeachers.length === 0) {
        return { error: 'Absent teachers are required' };
      }
      
      const result = await this.activity.processSubstitutes(date, absentTeachers);
      
      return { success: true, result };
    } catch (error) {
      logger.error('Error handling process request', { error: error instanceof Error ? error.message : String(error) });
      return { error: 'Internal server error' };
    }
  }

  /**
   * Handle an assignments request
   */
  handleAssignmentsRequest(): any {
    try {
      const assignments = this.activity.getSubstituteAssignments();
      return { success: true, assignments };
    } catch (error) {
      logger.error('Error handling assignments request', { error: error instanceof Error ? error.message : String(error) });
      return { error: 'Internal server error' };
    }
  }

  /**
   * Handle a verify request
   */
  handleVerifyRequest(): any {
    try {
      const reports = this.activity.verifyAssignments();
      return { success: true, reports };
    } catch (error) {
      logger.error('Error handling verify request', { error: error instanceof Error ? error.message : String(error) });
      return { error: 'Internal server error' };
    }
  }

  /**
   * Handle a clear request
   */
  handleClearRequest(): any {
    try {
      this.activity.clearAssignments();
      return { success: true };
    } catch (error) {
      logger.error('Error handling clear request', { error: error instanceof Error ? error.message : String(error) });
      return { error: 'Internal server error' };
    }
  }

  /**
   * Handle a file upload request
   */
  async handleFileUploadRequest(request: any): Promise<any> {
    try {
      const { fileType, fileContent } = request;
      
      if (!fileType || !fileContent) {
        return { error: 'File type and content are required' };
      }
      
      let filePath = '';
      
      if (fileType === 'timetable') {
        filePath = TIMETABLE_PATH;
      } else if (fileType === 'substitutes') {
        filePath = SUBSTITUTES_PATH;
      } else {
        return { error: 'Invalid file type' };
      }
      
      // Ensure the directory exists
      const dirPath = filePath.substring(0, filePath.lastIndexOf('/'));
      if (!fileSystem.exists(dirPath)) {
        fileSystem.mkdir(dirPath, { recursive: true });
      }
      
      // Write the file
      fileSystem.writeFile(filePath, fileContent);
      
      // Reload data if both files exist
      if (fileSystem.exists(TIMETABLE_PATH) && fileSystem.exists(SUBSTITUTES_PATH)) {
        await this.activity.initialize();
      }
      
      return { success: true, message: `${fileType} file uploaded successfully` };
    } catch (error) {
      logger.error('Error handling file upload request', { error: error instanceof Error ? error.message : String(error) });
      return { error: 'Internal server error' };
    }
  }
} 