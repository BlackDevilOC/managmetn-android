# Process Button Implementation Guide

## Overview
The process button in the Substitute Management System is responsible for consolidating and processing both timetable and substitute data to generate a complete schedule and teacher list. This document explains the implementation details and workflow.

## 1. Process Button Component

```typescript
// components/ProcessButton.tsx
import React from 'react';
import { useProcessData } from '../hooks/useProcessData';
import { ProcessButtonState } from '../types';

interface ProcessButtonProps {
  onProcessingComplete: (data: ProcessedData) => void;
  onError: (error: string) => void;
}

export const ProcessButton: React.FC<ProcessButtonProps> = ({
  onProcessingComplete,
  onError
}) => {
  const {
    processData,
    state,
    progress,
    error
  } = useProcessData();

  const handleProcess = async () => {
    try {
      const result = await processData();
      onProcessingComplete(result);
    } catch (err) {
      onError(err instanceof Error ? err.message : 'Processing failed');
    }
  };

  return (
    <div className="process-button-container">
      <button
        onClick={handleProcess}
        disabled={state === ProcessButtonState.PROCESSING}
        className={`process-button ${state}`}
      >
        {getButtonText(state)}
      </button>
      
      {state === ProcessButtonState.PROCESSING && (
        <div className="progress-container">
          <div className="progress-bar">
            <div 
              className="progress-fill"
              style={{ width: `${progress}%` }}
            />
          </div>
          <span className="progress-text">{progress}%</span>
        </div>
      )}

      {error && (
        <div className="error-message">
          {error}
        </div>
      )}
    </div>
  );
};

const getButtonText = (state: ProcessButtonState): string => {
  switch (state) {
    case ProcessButtonState.READY:
      return 'Process Data';
    case ProcessButtonState.PROCESSING:
      return 'Processing...';
    case ProcessButtonState.COMPLETED:
      return 'Process Complete';
    case ProcessButtonState.ERROR:
      return 'Retry Processing';
    default:
      return 'Process Data';
  }
};
```

## 2. Process Data Hook

```typescript
// hooks/useProcessData.ts
import { useState, useCallback } from 'react';
import { DataProcessor } from '../services/DataProcessor';
import { ProcessedData, ProcessButtonState } from '../types';

export const useProcessData = () => {
  const [state, setState] = useState<ProcessButtonState>(ProcessButtonState.READY);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const processor = new DataProcessor();

  const processData = useCallback(async (): Promise<ProcessedData> => {
    setState(ProcessButtonState.PROCESSING);
    setError(null);
    setProgress(0);

    try {
      // Step 1: Load and validate files
      setProgress(10);
      const { timetableData, substituteData } = await processor.loadFiles();
      
      // Step 2: Process timetable data
      setProgress(30);
      const teachers = await processor.processTimetable(timetableData);
      
      // Step 3: Process substitute data
      setProgress(50);
      const substituteTeachers = await processor.processSubstitutes(substituteData);
      
      // Step 4: Merge teacher data
      setProgress(70);
      const mergedTeachers = processor.mergeTeachers(teachers, substituteTeachers);
      
      // Step 5: Generate schedules
      setProgress(90);
      const schedules = processor.generateSchedules(mergedTeachers);
      
      setProgress(100);
      setState(ProcessButtonState.COMPLETED);
      
      return {
        teachers: mergedTeachers,
        schedules,
        teacherSchedules: processor.generateTeacherSchedules(schedules, mergedTeachers)
      };
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Processing failed');
      setState(ProcessButtonState.ERROR);
      throw err;
    }
  }, []);

  return {
    processData,
    state,
    progress,
    error
  };
};
```

## 3. Data Processor Service

```typescript
// services/DataProcessor.ts
import { Teacher, Schedule, ProcessedData } from '../types';
import { FileStorage } from './FileStorage';
import { TeacherNormalizer } from './TeacherNormalizer';

export class DataProcessor {
  private fileStorage: FileStorage;
  private teacherNormalizer: TeacherNormalizer;
  private teacherMap: Map<string, Teacher>;

  constructor() {
    this.fileStorage = new FileStorage();
    this.teacherNormalizer = new TeacherNormalizer();
    this.teacherMap = new Map();
  }

  async loadFiles(): Promise<{ timetableData: string; substituteData: string }> {
    const [timetableData, substituteData] = await Promise.all([
      this.fileStorage.readFile('timetable_file.csv'),
      this.fileStorage.readFile('substitute_file.csv')
    ]);

    if (!timetableData || !substituteData) {
      throw new Error('Required files are missing');
    }

    return { timetableData, substituteData };
  }

  async processTimetable(content: string): Promise<Teacher[]> {
    const records = this.parseCSV(content);
    const teachers: Teacher[] = [];

    // Process header
    const header = records[0];
    const validClasses = header.slice(2); // Skip day and period columns

    // Process each row
    for (let i = 1; i < records.length; i++) {
      const row = records[i];
      const day = this.normalizeDay(row[0]);
      const period = parseInt(row[1]);

      // Process teacher assignments
      for (let j = 2; j < row.length; j++) {
        const teacherName = row[j].trim();
        if (teacherName && teacherName.toLowerCase() !== 'empty') {
          const normalizedName = this.teacherNormalizer.normalize(teacherName);
          const teacher = await this.findOrCreateTeacher(normalizedName);
          teachers.push(teacher);
        }
      }
    }

    return Array.from(new Set(teachers));
  }

  async processSubstitutes(content: string): Promise<Teacher[]> {
    const records = this.parseCSV(content);
    const teachers: Teacher[] = [];

    for (const row of records) {
      const teacherName = row[0].trim();
      const phoneNumber = row[1]?.trim() || '';
      
      if (teacherName) {
        const normalizedName = this.teacherNormalizer.normalize(teacherName);
        const teacher = await this.findOrCreateTeacher(normalizedName);
        teacher.phoneNumber = phoneNumber;
        teacher.isSubstitute = true;
        teachers.push(teacher);
      }
    }

    return teachers;
  }

  mergeTeachers(teachers: Teacher[], substituteTeachers: Teacher[]): Teacher[] {
    const mergedMap = new Map<string, Teacher>();

    // Add regular teachers
    teachers.forEach(teacher => {
      mergedMap.set(teacher.normalizedName, teacher);
    });

    // Merge substitute teachers
    substituteTeachers.forEach(substitute => {
      const existing = mergedMap.get(substitute.normalizedName);
      if (existing) {
        // Update existing teacher with substitute info
        existing.phoneNumber = substitute.phoneNumber;
        existing.isSubstitute = true;
      } else {
        mergedMap.set(substitute.normalizedName, substitute);
      }
    });

    return Array.from(mergedMap.values());
  }

  generateSchedules(teachers: Teacher[]): Schedule[] {
    const schedules: Schedule[] = [];
    const timetableData = this.fileStorage.readFileSync('timetable_file.csv');
    const records = this.parseCSV(timetableData);

    // Process each row
    for (let i = 1; i < records.length; i++) {
      const row = records[i];
      const day = this.normalizeDay(row[0]);
      const period = parseInt(row[1]);

      // Process teacher assignments
      for (let j = 2; j < row.length; j++) {
        const teacherName = row[j].trim();
        if (teacherName && teacherName.toLowerCase() !== 'empty') {
          const normalizedName = this.teacherNormalizer.normalize(teacherName);
          const teacher = teachers.find(t => t.normalizedName === normalizedName);
          
          if (teacher) {
            schedules.push({
              id: schedules.length + 1,
              day,
              period,
              teacherId: teacher.id,
              className: records[0][j] // Class name from header
            });
          }
        }
      }
    }

    return schedules;
  }

  generateTeacherSchedules(schedules: Schedule[], teachers: Teacher[]): TeacherSchedule[] {
    const teacherSchedulesMap = new Map<number, TeacherSchedule>();

    schedules.forEach(schedule => {
      const teacher = teachers.find(t => t.id === schedule.teacherId);
      if (!teacher) return;

      if (!teacherSchedulesMap.has(teacher.id)) {
        teacherSchedulesMap.set(teacher.id, {
          teacherId: teacher.id,
          teacherName: teacher.name,
          schedules: []
        });
      }

      teacherSchedulesMap.get(teacher.id)?.schedules.push(schedule);
    });

    return Array.from(teacherSchedulesMap.values());
  }

  private parseCSV(content: string): string[][] {
    return content
      .split('\n')
      .map(line => line.split(',').map(cell => cell.trim()));
  }

  private normalizeDay(day: string): string {
    return day.toLowerCase().trim();
  }

  private async findOrCreateTeacher(name: string): Promise<Teacher> {
    if (this.teacherMap.has(name)) {
      return this.teacherMap.get(name)!;
    }

    const teacher: Teacher = {
      id: this.teacherMap.size + 1,
      name,
      normalizedName: name,
      phoneNumber: '',
      isSubstitute: false,
      variations: this.teacherNormalizer.generateVariations(name)
    };

    this.teacherMap.set(name, teacher);
    return teacher;
  }
}
```

## 4. Types

```typescript
// types/index.ts
export enum ProcessButtonState {
  READY = 'ready',
  PROCESSING = 'processing',
  COMPLETED = 'completed',
  ERROR = 'error'
}

export interface Teacher {
  id: number;
  name: string;
  normalizedName: string;
  phoneNumber: string;
  isSubstitute: boolean;
  variations: string[];
}

export interface Schedule {
  id: number;
  day: string;
  period: number;
  teacherId: number;
  className: string;
}

export interface TeacherSchedule {
  teacherId: number;
  teacherName: string;
  schedules: Schedule[];
}

export interface ProcessedData {
  teachers: Teacher[];
  schedules: Schedule[];
  teacherSchedules: TeacherSchedule[];
}
```

## 5. File Storage Service

```typescript
// services/FileStorage.ts
export class FileStorage {
  private readonly STORAGE_KEY = 'substitute_data';

  async readFile(filename: string): Promise<string | null> {
    try {
      const response = await fetch(`/data/${filename}`);
      if (!response.ok) {
        throw new Error(`Failed to read file: ${filename}`);
      }
      return await response.text();
    } catch (error) {
      console.error('Error reading file:', error);
      return null;
    }
  }

  readFileSync(filename: string): string {
    // Implementation for synchronous file reading
    // This is a placeholder - actual implementation depends on your environment
    return '';
  }

  async saveFile(filename: string, content: string): Promise<void> {
    try {
      const response = await fetch(`/data/${filename}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'text/plain'
        },
        body: content
      });

      if (!response.ok) {
        throw new Error(`Failed to save file: ${filename}`);
      }
    } catch (error) {
      console.error('Error saving file:', error);
      throw error;
    }
  }
}
```

## 6. CSS Styles

```css
/* process-button.css */
.process-button-container {
  margin: 20px 0;
  padding: 20px;
  background-color: #f8f9fa;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.process-button {
  display: block;
  width: 100%;
  padding: 12px;
  font-size: 16px;
  font-weight: bold;
  color: white;
  background-color: #007bff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.process-button:hover {
  background-color: #0056b3;
}

.process-button:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
}

.process-button.processing {
  background-color: #ffc107;
}

.process-button.completed {
  background-color: #28a745;
}

.process-button.error {
  background-color: #dc3545;
}

.progress-container {
  margin-top: 15px;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background-color: #e9ecef;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background-color: #007bff;
  transition: width 0.3s ease;
}

.progress-text {
  display: block;
  margin-top: 5px;
  text-align: center;
  font-size: 14px;
  color: #6c757d;
}

.error-message {
  margin-top: 10px;
  padding: 10px;
  background-color: #f8d7da;
  border: 1px solid #f5c6cb;
  border-radius: 4px;
  color: #721c24;
  font-size: 14px;
}
```

## Workflow Explanation

1. **Initialization**
   - Process button starts in READY state
   - User clicks button to begin processing

2. **File Loading**
   - Loads both timetable and substitute files
   - Validates file existence and format
   - Progress: 0-10%

3. **Timetable Processing**
   - Parses timetable CSV
   - Extracts teacher information
   - Creates initial teacher records
   - Progress: 10-30%

4. **Substitute Processing**
   - Parses substitute CSV
   - Extracts substitute teacher information
   - Updates teacher records with substitute data
   - Progress: 30-50%

5. **Data Merging**
   - Combines regular and substitute teacher data
   - Updates phone numbers and substitute status
   - Progress: 50-70%

6. **Schedule Generation**
   - Creates schedule entries for each teacher
   - Links teachers to their classes and periods
   - Progress: 70-90%

7. **Final Processing**
   - Generates teacher-specific schedules
   - Completes data processing
   - Progress: 90-100%

8. **Completion**
   - Updates UI to show completion
   - Provides processed data to parent component
   - Enables data export or further actions

## Error Handling

1. **File Loading Errors**
   - Missing files
   - Invalid file formats
   - File read permissions

2. **Processing Errors**
   - Invalid data formats
   - Missing required fields
   - Data consistency issues

3. **UI Feedback**
   - Progress indicators
   - Error messages
   - Retry options

## Best Practices

1. **State Management**
   - Clear state transitions
   - Progress tracking
   - Error handling

2. **Data Processing**
   - Efficient file parsing
   - Data validation
   - Proper error handling

3. **User Experience**
   - Clear progress indication
   - Informative error messages
   - Responsive UI updates

4. **Performance**
   - Efficient data structures
   - Optimized processing
   - Memory management

## 7. Teacher Name Normalization Algorithm

The system includes a sophisticated algorithm for normalizing and matching teacher names to handle variations, typos, and different formats. This is implemented in the `TeacherNormalizer` class.

### 7.1 Name Normalization Implementation

```typescript
// services/TeacherNormalizer.ts
export class TeacherNormalizer {
  private readonly SIMILARITY_THRESHOLD = 0.90;
  private readonly TEACHER_MAP = new Map<string, Teacher>();

  normalizeName(name: string): string {
    if (!name || typeof name !== 'string') return '';
    
    return name
      .toLowerCase()
      .replace(/(sir|miss|mr|ms|mrs|sr|dr)\.?\s*/gi, '') // Remove titles
      .replace(/[^a-z\s-]/g, '') // Remove special characters
      .replace(/\s+/g, ' ') // Normalize spaces
      .trim()
      .split(/\s+/)
      .filter(part => part.length > 1) // Filter out single letters
      .sort() // Sort parts for consistent comparison
      .join(' ');
  }

  simplifiedMetaphone(str: string): string {
    if (!str || typeof str !== 'string') return '';
    
    return str
      .toLowerCase()
      .replace(/[aeiou]/g, 'a') // Replace all vowels with 'a'
      .replace(/[^a-z]/g, '')  // Remove non-letters
      .replace(/(.)\1+/g, '$1') // Remove consecutive duplicates
      .slice(0, 8); // Take first 8 characters for comparison
  }

  levenshtein(a: string, b: string): number {
    if (!a || !b) return 0;
    if (a.length === 0) return b.length;
    if (b.length === 0) return a.length;

    const matrix = Array(b.length + 1).fill(null).map(() => Array(a.length + 1).fill(null));

    for (let i = 0; i <= a.length; i++) matrix[0][i] = i;
    for (let j = 0; j <= b.length; j++) matrix[j][0] = j;

    for (let j = 1; j <= b.length; j++) {
      for (let i = 1; i <= a.length; i++) {
        const indicator = a[i - 1] === b[j - 1] ? 0 : 1;
        matrix[j][i] = Math.min(
          matrix[j][i - 1] + 1, // deletion
          matrix[j - 1][i] + 1, // insertion
          matrix[j - 1][i - 1] + indicator // substitution
        );
      }
    }

    return matrix[b.length][a.length];
  }

  nameSimilarity(a: string, b: string): number {
    if (!a || !b) return 0;

    const aMeta = this.simplifiedMetaphone(a);
    const bMeta = this.simplifiedMetaphone(b);
    const distance = this.levenshtein(aMeta, bMeta);
    const similarity = 1 - distance / Math.max(aMeta.length, bMeta.length, 1);

    // Additional checks for very similar names
    if (a.includes(b) || b.includes(a)) {
      return Math.max(similarity, 0.95);
    }

    // Check for name parts match
    const aParts = a.split(' ');
    const bParts = b.split(' ');
    const commonParts = aParts.filter(part => bParts.includes(part));
    if (commonParts.length > 0) {
      return Math.max(similarity, 0.85 + (0.05 * commonParts.length));
    }

    return similarity;
  }

  generateVariations(name: string): string[] {
    const variations = new Set<string>();
    
    // Add original name
    variations.add(name);
    
    // Add lowercase version
    variations.add(name.toLowerCase());
    
    // Add version without honorific
    const withoutHonorific = name.replace(/^(sir|miss|mr|ms|mrs|sr|dr)\s+/i, '').trim();
    variations.add(withoutHonorific);
    variations.add(withoutHonorific.toLowerCase());
    
    // Add version with different honorific
    if (name.toLowerCase().startsWith('sir')) {
      variations.add(name.replace(/^sir/i, 'miss'));
    } else if (name.toLowerCase().startsWith('miss')) {
      variations.add(name.replace(/^miss/i, 'sir'));
    }
    
    return Array.from(variations);
  }

  registerTeacher(rawName: string, phone: string = ''): boolean {
    if (!rawName || rawName.toLowerCase() === 'empty' || rawName.trim().length < 2) {
      return false;
    }

    const normalized = this.normalizeName(rawName);
    if (!normalized) return false;

    // Check for exact matches first
    if (this.TEACHER_MAP.has(normalized)) {
      const existing = this.TEACHER_MAP.get(normalized)!;
      existing.variations.add(rawName);
      if (phone && !existing.phone) existing.phone = phone;
      return true;
    }

    // Check for similar names
    for (const [, teacher] of this.TEACHER_MAP) {
      const similarity = this.nameSimilarity(normalized, this.normalizeName(teacher.canonicalName));
      if (similarity > this.SIMILARITY_THRESHOLD) {
        teacher.variations.add(rawName);
        if (phone && !teacher.phone) teacher.phone = phone;
        return true;
      }
    }

    // Add new teacher
    this.TEACHER_MAP.set(normalized, {
      canonicalName: rawName.trim().replace(/\s+/g, ' '),
      phone,
      variations: new Set([rawName])
    });

    return true;
  }
}
```

### 7.2 Algorithm Features

1. **Name Normalization**
   - Removes titles (Sir, Miss, Mr, etc.)
   - Standardizes case and spacing
   - Removes special characters
   - Sorts name parts for consistent comparison

2. **Phonetic Matching**
   - Uses a simplified metaphone algorithm
   - Normalizes vowels to handle pronunciation variations
   - Removes consecutive duplicate characters
   - Limits comparison to first 8 characters

3. **Similarity Calculation**
   - Uses Levenshtein distance for string comparison
   - Considers phonetic similarity
   - Checks for substring matches
   - Evaluates common name parts
   - Uses a 90% similarity threshold

4. **Variation Generation**
   - Creates multiple name variations
   - Handles different honorifics
   - Maintains case variations
   - Tracks all known variations of a name

### 7.3 Usage in Data Processing

The algorithm is used in several key processes:

1. **Teacher Registration**
   - Normalizes new teacher names
   - Checks for existing matches
   - Creates new entries for unique names
   - Updates variations for existing teachers

2. **Name Matching**
   - Compares teacher names across different files
   - Identifies potential matches
   - Handles typos and variations
   - Maintains consistency in teacher records

3. **Data Validation**
   - Ensures consistent name formatting
   - Prevents duplicate entries
   - Validates name variations
   - Maintains data integrity

### 7.4 Error Handling

1. **Input Validation**
   - Checks for empty or invalid names
   - Validates name length
   - Handles special characters
   - Processes different name formats

2. **Matching Logic**
   - Handles edge cases in name comparison
   - Manages partial matches
   - Processes different name orderings
   - Deals with missing or incomplete data

3. **Data Consistency**
   - Maintains unique teacher records
   - Updates variations consistently
   - Preserves original name formats
   - Tracks all name variations 