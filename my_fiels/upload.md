# TypeScript Source Code for File Processing

## 1. File Upload Component

```typescript
// components/FileUpload.tsx
import React, { useState } from 'react';
import { useFileProcessor } from '../hooks/useFileProcessor';

interface FileUploadProps {
  onUploadComplete: (data: ProcessedData) => void;
  onError: (error: string) => void;
}

export const FileUpload: React.FC<FileUploadProps> = ({ onUploadComplete, onError }) => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [fileType, setFileType] = useState<'timetable' | 'substitute'>('timetable');
  const { processFile, isLoading } = useFileProcessor();

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      setSelectedFile(file);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) return;

    try {
      const result = await processFile(selectedFile, fileType);
      onUploadComplete(result);
    } catch (error) {
      onError(error instanceof Error ? error.message : 'An error occurred');
    }
  };

  return (
    <div className="file-upload-container">
      <div className="file-type-selector">
        <button
          className={fileType === 'timetable' ? 'active' : ''}
          onClick={() => setFileType('timetable')}
        >
          Timetable
        </button>
        <button
          className={fileType === 'substitute' ? 'active' : ''}
          onClick={() => setFileType('substitute')}
        >
          Substitute
        </button>
      </div>

      <input
        type="file"
        accept=".csv"
        onChange={handleFileChange}
        className="file-input"
      />

      {selectedFile && (
        <div className="file-info">
          <p>Selected file: {selectedFile.name}</p>
          <button onClick={handleUpload} disabled={isLoading}>
            {isLoading ? 'Processing...' : 'Upload'}
          </button>
        </div>
      )}
    </div>
  );
};
```

## 2. File Processing Hook

```typescript
// hooks/useFileProcessor.ts
import { useState } from 'react';
import { FileProcessor } from '../services/FileProcessor';
import { ProcessedData } from '../types';

export const useFileProcessor = () => {
  const [isLoading, setIsLoading] = useState(false);
  const fileProcessor = new FileProcessor();

  const processFile = async (
    file: File,
    type: 'timetable' | 'substitute'
  ): Promise<ProcessedData> => {
    setIsLoading(true);
    try {
      const content = await readFileContent(file);
      const result = await fileProcessor.processFile(content, type);
      return result;
    } finally {
      setIsLoading(false);
    }
  };

  const readFileContent = (file: File): Promise<string> => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (event) => {
        if (event.target?.result) {
          resolve(event.target.result as string);
        } else {
          reject(new Error('Failed to read file'));
        }
      };
      reader.onerror = () => reject(new Error('Error reading file'));
      reader.readAsText(file);
    });
  };

  return { processFile, isLoading };
};
```

## 3. File Processor Service

```typescript
// services/FileProcessor.ts
import { ProcessedData, Teacher, Schedule } from '../types';
import { TeacherNormalizer } from './TeacherNormalizer';

export class FileProcessor {
  private teacherNormalizer: TeacherNormalizer;
  private teacherMap: Map<string, Teacher>;

  constructor() {
    this.teacherNormalizer = new TeacherNormalizer();
    this.teacherMap = new Map();
  }

  async processFile(
    content: string,
    type: 'timetable' | 'substitute'
  ): Promise<ProcessedData> {
    try {
      if (type === 'timetable') {
        return this.processTimetableFile(content);
      } else {
        return this.processSubstituteFile(content);
      }
    } catch (error) {
      throw new Error(`Failed to process ${type} file: ${error.message}`);
    }
  }

  private async processTimetableFile(content: string): Promise<ProcessedData> {
    const records = this.parseCSV(content);
    const schedules: Schedule[] = [];
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
          schedules.push({
            id: 0,
            day,
            period,
            teacherId: teacher.id,
            className: validClasses[j - 2]
          });
        }
      }
    }

    return {
      teachers: Array.from(this.teacherMap.values()),
      schedules,
      teacherSchedules: this.generateTeacherSchedules(schedules)
    };
  }

  private async processSubstituteFile(content: string): Promise<ProcessedData> {
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
        this.teacherMap.set(normalizedName, teacher);
      }
    }

    return {
      teachers: Array.from(this.teacherMap.values()),
      schedules: [],
      teacherSchedules: []
    };
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
      variations: []
    };

    this.teacherMap.set(name, teacher);
    return teacher;
  }

  private generateTeacherSchedules(schedules: Schedule[]): TeacherSchedule[] {
    const teacherSchedulesMap = new Map<number, TeacherSchedule>();

    for (const schedule of schedules) {
      if (!teacherSchedulesMap.has(schedule.teacherId)) {
        const teacher = this.teacherMap.get(
          Array.from(this.teacherMap.values())
            .find(t => t.id === schedule.teacherId)?.normalizedName || ''
        );

        if (teacher) {
          teacherSchedulesMap.set(schedule.teacherId, {
            teacherId: schedule.teacherId,
            teacherName: teacher.name,
            schedules: []
          });
        }
      }

      const teacherSchedule = teacherSchedulesMap.get(schedule.teacherId);
      if (teacherSchedule) {
        teacherSchedule.schedules.push(schedule);
      }
    }

    return Array.from(teacherSchedulesMap.values());
  }
}
```

## 4. Teacher Normalizer Service

```typescript
// services/TeacherNormalizer.ts
export class TeacherNormalizer {
  normalize(name: string): string {
    return name
      .toLowerCase()
      .trim()
      .replace(/\s+/g, ' ')
      .replace(/[^a-z\s]/g, '');
  }

  generateVariations(name: string): string[] {
    const variations = new Set<string>();
    const parts = name.split(' ');

    // Add full name
    variations.add(name);

    // Add first name + last name
    if (parts.length > 1) {
      variations.add(`${parts[0]} ${parts[parts.length - 1]}`);
    }

    // Add initials
    const initials = parts.map(p => p[0]).join('');
    variations.add(initials);

    return Array.from(variations);
  }
}
```

## 5. Types

```typescript
// types/index.ts
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

## 6. Error Handling

```typescript
// utils/errors.ts
export class ProcessingError extends Error {
  constructor(message: string, public readonly originalError?: Error) {
    super(message);
    this.name = 'ProcessingError';
  }
}

export class ValidationError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'ValidationError';
  }
}

export class FileError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'FileError';
  }
}
```

## 7. Usage Example

```typescript
// App.tsx
import React from 'react';
import { FileUpload } from './components/FileUpload';
import { ProcessedData } from './types';

const App: React.FC = () => {
  const handleUploadComplete = (data: ProcessedData) => {
    console.log('Processed data:', data);
    // Handle the processed data (e.g., save to state, display in UI)
  };

  const handleError = (error: string) => {
    console.error('Error:', error);
    // Handle the error (e.g., show error message to user)
  };

  return (
    <div className="app">
      <h1>Substitute Management System</h1>
      <FileUpload
        onUploadComplete={handleUploadComplete}
        onError={handleError}
      />
    </div>
  );
};

export default App;
```

## 8. CSS Styles

```css
/* styles.css */
.file-upload-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px;
}

.file-type-selector {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.file-type-selector button {
  padding: 8px 16px;
  border: 1px solid #ccc;
  background: white;
  cursor: pointer;
}

.file-type-selector button.active {
  background: #007bff;
  color: white;
  border-color: #007bff;
}

.file-input {
  display: block;
  margin-bottom: 20px;
}

.file-info {
  margin-top: 20px;
  padding: 10px;
  border: 1px solid #eee;
  border-radius: 4px;
}

.file-info button {
  margin-top: 10px;
  padding: 8px 16px;
  background: #28a745;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.file-info button:disabled {
  background: #ccc;
  cursor: not-allowed;
}
```

This TypeScript implementation provides a complete solution for file upload and processing in the Substitute Management System. It includes:

1. A reusable file upload component
2. File processing logic with proper error handling
3. Teacher name normalization
4. Data structure definitions
5. Type safety throughout the codebase
6. CSS styles for the UI components

The code is organized into separate modules for better maintainability and follows TypeScript best practices. It can be easily integrated into a React application and extended as needed. 