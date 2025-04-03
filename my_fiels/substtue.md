# Substitute File Upload Documentation

## Overview
This document provides detailed information about the substitute file upload functionality in the Substitute Management System. The substitute file contains information about substitute teachers, including their names and contact details.

## File Format
The substitute file should be in CSV format with the following structure:
```
Teacher Name,Phone Number
John Doe,1234567890
Jane Smith,0987654321
```

## Implementation Details

### 1. Substitute File Upload Component

```typescript
// components/SubstituteFileUpload.tsx
import React, { useState } from 'react';
import { useSubstituteProcessor } from '../hooks/useSubstituteProcessor';
import { SubstituteTeacher } from '../types';

interface SubstituteFileUploadProps {
  onUploadComplete: (teachers: SubstituteTeacher[]) => void;
  onError: (error: string) => void;
}

export const SubstituteFileUpload: React.FC<SubstituteFileUploadProps> = ({
  onUploadComplete,
  onError
}) => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const { processSubstituteFile, isLoading } = useSubstituteProcessor();

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      setSelectedFile(file);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) return;

    try {
      const teachers = await processSubstituteFile(selectedFile);
      onUploadComplete(teachers);
    } catch (error) {
      onError(error instanceof Error ? error.message : 'An error occurred');
    }
  };

  return (
    <div className="substitute-upload-container">
      <h2>Upload Substitute Teachers</h2>
      
      <div className="file-input-container">
        <input
          type="file"
          accept=".csv"
          onChange={handleFileChange}
          className="file-input"
        />
      </div>

      {selectedFile && (
        <div className="file-info">
          <p>Selected file: {selectedFile.name}</p>
          <button 
            onClick={handleUpload} 
            disabled={isLoading}
            className="upload-button"
          >
            {isLoading ? 'Processing...' : 'Upload Substitute File'}
          </button>
        </div>
      )}
    </div>
  );
};
```

### 2. Substitute Processor Hook

```typescript
// hooks/useSubstituteProcessor.ts
import { useState } from 'react';
import { SubstituteProcessor } from '../services/SubstituteProcessor';
import { SubstituteTeacher } from '../types';

export const useSubstituteProcessor = () => {
  const [isLoading, setIsLoading] = useState(false);
  const processor = new SubstituteProcessor();

  const processSubstituteFile = async (
    file: File
  ): Promise<SubstituteTeacher[]> => {
    setIsLoading(true);
    try {
      const content = await readFileContent(file);
      const teachers = await processor.processFile(content);
      return teachers;
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

  return { processSubstituteFile, isLoading };
};
```

### 3. Substitute Processor Service

```typescript
// services/SubstituteProcessor.ts
import { SubstituteTeacher } from '../types';
import { TeacherNormalizer } from './TeacherNormalizer';

export class SubstituteProcessor {
  private teacherNormalizer: TeacherNormalizer;

  constructor() {
    this.teacherNormalizer = new TeacherNormalizer();
  }

  async processFile(content: string): Promise<SubstituteTeacher[]> {
    try {
      const records = this.parseCSV(content);
      return this.processRecords(records);
    } catch (error) {
      throw new Error(`Failed to process substitute file: ${error.message}`);
    }
  }

  private parseCSV(content: string): string[][] {
    return content
      .split('\n')
      .map(line => line.split(',').map(cell => cell.trim()));
  }

  private processRecords(records: string[][]): SubstituteTeacher[] {
    const teachers: SubstituteTeacher[] = [];
    
    // Skip header row
    for (let i = 1; i < records.length; i++) {
      const row = records[i];
      if (row.length < 1) continue;

      const teacherName = row[0].trim();
      const phoneNumber = row[1]?.trim() || '';

      if (teacherName) {
        const normalizedName = this.teacherNormalizer.normalize(teacherName);
        const variations = this.teacherNormalizer.generateVariations(teacherName);

        teachers.push({
          id: teachers.length + 1,
          name: teacherName,
          normalizedName,
          phoneNumber,
          variations,
          isSubstitute: true
        });
      }
    }

    return teachers;
  }
}
```

### 4. Types

```typescript
// types/substitute.ts
export interface SubstituteTeacher {
  id: number;
  name: string;
  normalizedName: string;
  phoneNumber: string;
  variations: string[];
  isSubstitute: boolean;
}
```

### 5. Error Handling

```typescript
// utils/substituteErrors.ts
export class SubstituteProcessingError extends Error {
  constructor(message: string, public readonly originalError?: Error) {
    super(message);
    this.name = 'SubstituteProcessingError';
  }
}

export class SubstituteValidationError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'SubstituteValidationError';
  }
}
```

### 6. Usage Example

```typescript
// SubstituteManagementScreen.tsx
import React, { useState } from 'react';
import { SubstituteFileUpload } from './components/SubstituteFileUpload';
import { SubstituteTeacher } from './types';

const SubstituteManagementScreen: React.FC = () => {
  const [substituteTeachers, setSubstituteTeachers] = useState<SubstituteTeacher[]>([]);

  const handleUploadComplete = (teachers: SubstituteTeacher[]) => {
    setSubstituteTeachers(teachers);
    // Additional logic after successful upload
  };

  const handleError = (error: string) => {
    // Handle error (e.g., show error message)
    console.error('Error uploading substitute file:', error);
  };

  return (
    <div className="substitute-management-screen">
      <SubstituteFileUpload
        onUploadComplete={handleUploadComplete}
        onError={handleError}
      />
      
      {substituteTeachers.length > 0 && (
        <div className="substitute-list">
          <h3>Uploaded Substitute Teachers</h3>
          <ul>
            {substituteTeachers.map(teacher => (
              <li key={teacher.id}>
                {teacher.name} - {teacher.phoneNumber}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
};
```

### 7. CSS Styles

```css
/* substitute-styles.css */
.substitute-upload-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px;
  background-color: #f8f9fa;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.substitute-upload-container h2 {
  color: #333;
  margin-bottom: 20px;
}

.file-input-container {
  margin-bottom: 20px;
}

.file-input {
  display: block;
  width: 100%;
  padding: 10px;
  border: 2px dashed #ccc;
  border-radius: 4px;
  cursor: pointer;
}

.file-info {
  margin-top: 20px;
  padding: 15px;
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.upload-button {
  display: block;
  width: 100%;
  padding: 12px;
  background-color: #28a745;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  transition: background-color 0.2s;
}

.upload-button:hover {
  background-color: #218838;
}

.upload-button:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
}

.substitute-list {
  margin-top: 30px;
  padding: 20px;
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.substitute-list h3 {
  color: #333;
  margin-bottom: 15px;
}

.substitute-list ul {
  list-style: none;
  padding: 0;
}

.substitute-list li {
  padding: 10px;
  border-bottom: 1px solid #eee;
}

.substitute-list li:last-child {
  border-bottom: none;
}
```

## Best Practices

1. **File Validation**
   - Validate file format before processing
   - Check for required columns
   - Ensure phone numbers are in correct format

2. **Error Handling**
   - Provide clear error messages
   - Handle file reading errors
   - Validate data before processing

3. **Data Processing**
   - Process files in background
   - Show loading indicators
   - Handle large files efficiently

4. **User Experience**
   - Provide clear instructions
   - Show upload progress
   - Display success/error messages
   - Allow file selection feedback

## Integration Steps

1. Import the `SubstituteFileUpload` component
2. Provide required props (onUploadComplete, onError)
3. Handle the uploaded data in your application
4. Style the component according to your application's theme

## Error Scenarios

1. **Invalid File Format**
   - Show error message for non-CSV files
   - Provide format requirements

2. **Missing Data**
   - Handle missing teacher names
   - Handle missing phone numbers

3. **Processing Errors**
   - Handle file reading errors
   - Handle data processing errors

4. **Validation Errors**
   - Show validation error messages
   - Provide guidance for correction 