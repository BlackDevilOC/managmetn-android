# Teacher Substitution Management System Documentation

## AI Recreation Context
This documentation is structured to enable AI-assisted recreation of the complete application. Key implementation details are organized in a logical flow:

1. System Setup: Dependencies and initial configuration
2. Core Components: Server, client, and mobile implementations
3. Data Structures: Schemas and file formats
4. Integration Points: API endpoints and data flow
5. Business Logic: Substitution algorithms and processing

# Teacher Substitution Management System - Technical Documentation

## System Overview
A comprehensive teacher substitution management system with web and mobile interfaces that handles teacher absences, substitute assignments, and notifications through an automated process.

## Core Data Flow
1. CSV File Processing
   - System reads two primary CSV files:
     - timetable_file.csv: Contains regular class schedules
     - Substitude_file.csv: Contains substitute teacher information
   - Files are processed using CSV parser with error handling
   - Data is normalized and stored in JSON format

2. Teacher Data Processing
```typescript
interface Teacher {
  id: number;
  name: string;
  phoneNumber: string | null;
  isSubstitute: boolean;
  variations?: string[]; // Different name spellings
}
```
- Name normalization removes titles (Sir, Miss, etc.)
- Handles multiple name variations using Levenshtein distance
- Deduplicates teacher entries
- Stores in total_teacher.json

3. Schedule Management
```typescript
interface Schedule {
  id: number;
  day: string;
  period: number;
  teacherId: number;
  className: string;
}
```
- Validates day names and period numbers
- Stores class schedules by day
- Handles period configurations
- Maintains teacher-class assignments

4. Absence Handling
```typescript
interface Absence {
  id: number;
  teacherId: number;
  date: string;
  substituteId: number | null;
  reason?: string;
  status: 'pending' | 'approved' | 'completed';
}
```
- Records teacher absences
- Triggers substitute assignment process
- Maintains absence history

## File Structure and Purpose

### Data Files (data/)
- absent_teachers.json: Currently absent teachers
- assigned_teacher.json: Substitute assignments
- class_schedules.json: Class timetables
- total_teacher.json: All teacher records
- period_config.json: Period timing settings
- sms_history.json: Notification logs

### Server Components (server/)
1. Main Server (index.ts)
   - Express.js setup
   - Route registration
   - Authentication setup
   - Initial data loading

2. CSV Handler (csv-handler.ts)
   - Processes timetable_file.csv
   - Processes Substitude_file.csv
   - Validates data format
   - Normalizes teacher names

3. Storage System (storage.ts)
   - In-memory data management
   - File-based persistence
   - CRUD operations for all entities

4. Substitute Manager (substitute-manager.ts)
   - Auto-assignment algorithm
   - Workload balancing
   - Conflict prevention
   - Assignment history tracking

### Client Components (client/src/)
1. Dashboard
   - Displays absent teachers
   - Shows today's substitutions
   - Quick action buttons
   - Real-time updates

2. Teacher Management
   - Teacher profiles
   - Attendance tracking
   - Schedule viewing
   - Contact management

3. Substitution Interface
   - Manual assignment
   - Auto-assignment triggers
   - Assignment review
   - Schedule conflicts

### Mobile App (mobile/src/)
1. Core Features
   - Offline capability
   - Push notifications
   - Quick attendance marking
   - Schedule viewing

2. Data Sync
   - Local SQLite storage
   - Periodic server sync
   - Conflict resolution
   - Cache management

## Database Schema

### Teacher Table
```sql
CREATE TABLE teachers (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL,
  phone_number TEXT,
  is_substitute BOOLEAN,
  variations TEXT
);
```

### Schedule Table
```sql
CREATE TABLE schedules (
  id INTEGER PRIMARY KEY,
  day TEXT NOT NULL,
  period INTEGER NOT NULL,
  teacher_id INTEGER,
  class_name TEXT,
  FOREIGN KEY (teacher_id) REFERENCES teachers(id)
);
```

### Absence Table
```sql
CREATE TABLE absences (
  id INTEGER PRIMARY KEY,
  teacher_id INTEGER,
  date TEXT NOT NULL,
  substitute_id INTEGER,
  status TEXT,
  FOREIGN KEY (teacher_id) REFERENCES teachers(id),
  FOREIGN KEY (substitute_id) REFERENCES teachers(id)
);
```

## API Endpoints

### Authentication
```typescript
POST /api/login
Body: { username: string, password: string }
Response: { token: string, user: User }

GET /api/user
Headers: { Authorization: string }
Response: User
```

### Teacher Management
```typescript
GET /api/teachers
Response: Teacher[]

POST /api/teachers
Body: { name: string, phoneNumber?: string, isSubstitute: boolean }
Response: Teacher

GET /api/teacher-schedule/:teacherId
Response: Schedule[]
```

### Absence Management
```typescript
POST /api/absences
Body: { teacherId: number, date: string }
Response: Absence

GET /api/absences
Response: Absence[]

POST /api/auto-assign-substitutes
Response: { assignments: Assignment[], warnings: string[] }
```

## Data Processing Algorithms

### Teacher Name Normalization
```typescript
function normalizeTeacherName(name: string): string {
  return name
    .toLowerCase()
    .replace(/(sir|miss|mr|ms|mrs|sr|dr)\.?\s*/gi, '')
    .replace(/[^a-z\s-]/g, '')
    .trim()
    .split(/\s+/)
    .filter(part => part.length > 1)
    .sort()
    .join(' ');
}
```

### Substitute Assignment Logic
1. Get absent teachers for the day
2. Find available substitutes
3. Check substitute workload
4. Consider class compatibility
5. Avoid period conflicts
6. Assign and notify

### Schedule Processing
1. Read CSV timetable
2. Validate day and period
3. Map teachers to classes
4. Store in structured format
5. Handle schedule conflicts

## Implementation Steps

1. Setup Project Structure
```bash
mkdir -p server/src client/src mobile/src data
npm init -y
```

2. Install Dependencies
```json
{
  "dependencies": {
    "express": "^4.18.2",
    "csv-parser": "^3.0.0",
    "date-fns": "^2.30.0",
    "react": "^18.2.0",
    "react-native": "^0.72.0",
    "typescript": "^5.0.0"
  }
}
```

3. Initialize Database Files
```bash
touch data/total_teacher.json
touch data/absent_teachers.json
touch data/assigned_teacher.json
```

4. Configure TypeScript
```json
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "ESNext",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true
  }
}
```

5. Start Implementation
- Set up Express server
- Create React frontend
- Implement data processing
- Add authentication
- Build mobile app

## Testing Instructions

1. Data Validation
```bash
npm run test:data
```
Validates CSV files and data structures

2. Substitute Assignment
```bash
npm run test:assign
```
Tests auto-assignment algorithm

3. API Testing
```bash
npm run test:api
```
Validates API endpoints

## Deployment Guide

1. Server Setup
- Port: 5000
- Host: 0.0.0.0
- Environment: Production

2. Data Migration
- Backup CSV files
- Validate JSON storage
- Check file permissions

3. Mobile Build
- Configure environment
- Build APK/IPA
- Test offline mode

## Security Considerations

1. Data Protection
- File permissions
- Data encryption
- Secure storage

2. Authentication
- JWT tokens
- Session management
- Password hashing

3. API Security
- Rate limiting
- Input validation
- Error handling

## Table of Contents
1. [System Overview](#system-overview)
2. [Core Data Flow](#core-data-flow)
3. [File Structure and Purpose](#file-structure-and-purpose)
4. [Database Schema](#database-schema)
5. [API Endpoints](#api-endpoints)
6. [Data Processing Algorithms](#data-processing-algorithms)
7. [Implementation Steps](#implementation-steps)
8. [Testing Instructions](#testing-instructions)
9. [Deployment Guide](#deployment-guide)
10. [Security Considerations](#security-considerations)
11. [Introduction](#introduction)
12. [System Architecture](#system-architecture)
13. [Features](#features)
14. [Setup Instructions](#setup-instructions)
15. [Detailed Component Guide](#detailed-component-guide)
16. [API Documentation](#api-documentation)
17. [Mobile App](#mobile-app)
18. [Data Structures](#data-structures)
19. [File Organization](#file-organization)
20. [Development Guide](#development-guide)
21. [Troubleshooting](#troubleshooting)


## Introduction
The Teacher Substitution Management System is a comprehensive solution for educational institutions to manage teacher absences and substitute assignments efficiently. It provides real-time tracking, automated assignments, and instant notifications.

### Core Purpose
- Automate substitute teacher assignments
- Reduce administrative workload
- Ensure minimal disruption to classes
- Track attendance and substitution history
- Enable real-time schedule management
- Provide mobile access for teachers

### Key Benefits
- Reduced manual coordination
- Improved response time
- Better resource utilization
- Enhanced communication
- Data-driven decision making

## System Architecture

### Tech Stack
- Frontend: React 18 with TypeScript
- Backend: Node.js 16+ with Express
- Mobile: React Native with Expo
- Database: File-based JSON storage
- SMS Integration: Multiple provider support
- State Management: TanStack Query
- Styling: Tailwind CSS

### Key Components
1. Web Application (client/)
   - React SPA
   - Real-time updates
   - Responsive design
   - Role-based access

2. Server (server/)
   - Express API
   - File-based storage
   - Authentication system
   - SMS integration

3. Mobile App (mobile/)
   - React Native
   - Offline support
   - Push notifications
   - Quick actions

4. Shared Types (shared/)
   - TypeScript interfaces
   - Validation schemas
   - Utility types

## Features

### 1. Teacher Management
- Teacher profiles with contact info
- Availability tracking
- Schedule management
- Specialization tracking
- History maintenance
- Contact preferences

### 2. Absence Management
- Quick absence recording
- Multiple day handling
- Absence categories
- Documentation upload
- History tracking
- Pattern analysis

### 3. Substitute Assignment
- Intelligent matching algorithm
- Workload balancing
- Conflict prevention
- Preference consideration
- Real-time assignment
- Manual override options

### 4. Notification System
- SMS alerts
- Custom templates
- Delivery tracking
- Bulk messaging
- Read receipts
- History logging

### 5. Schedule Management
- Visual timetable
- Period configuration
- Conflict detection
- Schedule export
- Calendar integration
- Quick adjustments

### 6. Analytics & Reporting
- Absence patterns
- Substitution stats
- Teacher workload
- System usage
- SMS delivery
- Performance metrics

## Setup Instructions

### Prerequisites
```bash
Node.js 16+
npm or yarn
Git
```

### Installation Steps
1. Clone the repository
2. Install dependencies:
```bash
npm install
```

3. Configure environment:
   - SMS provider setup
   - Server port (default: 5000)
   - Admin account creation
   - File permissions
   - Logging setup

### Data Files Setup
Required CSV files:
- timetable_file.csv: Class schedules
  - Format: Day,Period,Teacher,Class
  - Example: Monday,1,John Doe,10A

- Substitude_file.csv: Substitute details
  - Format: Name,Phone,Availability
  - Example: Jane Smith,+1234567890,M/W/F

### Initial Configuration
1. Admin Setup
   - Create admin account
   - Configure SMS templates
   - Set up period timings
   - Define school schedule

2. System Checks
   - Verify file permissions
   - Test SMS delivery
   - Check file paths
   - Validate schedules

## Detailed Component Guide

### 1. Login Page (/login)
Features:
- Secure authentication
- Role-based access
- Password recovery
- Session management
- Activity logging
- Security measures

### 2. Dashboard (/dashboard)
Components:
- Absent teacher count
- Today's assignments
- Quick actions menu
- Notification center
- System status
- Recent activities

### 3. Teacher Management (/teachers)
Features:
- Profile management
- Schedule viewing
- Contact updates
- Availability setting
- History tracking
- Document upload

### 4. Absence Recording (/absences)
Features:
- Quick recording
- Duration setting
- Document attachment
- History viewing
- Pattern analysis
- Bulk operations

### 5. Substitute Assignment (/substitutes)
Features:
- Auto assignment
- Manual override
- History tracking
- Preference setting
- Conflict checking
- Workload balancing

### 6. Schedule Management (/schedule)
Features:
- Timetable view
- Period management
- Conflict handling
- Export options
- Quick edits
- Calendar sync

### 7. Settings (/settings)
Features:
- SMS configuration
- Period timing
- System preferences
- User management
- Backup options
- Log viewing

## API Documentation

### Authentication
```
POST /api/login
  Body: { username, password }
  Response: { token, user }

GET /api/user
  Headers: { Authorization }
  Response: { user }

POST /api/logout
  Headers: { Authorization }
  Response: { success }
```

### Teacher Management
```
GET /api/teachers
  Response: Teacher[]

POST /api/teachers
  Body: { name, phone, isSubstitute }
  Response: Teacher

GET /api/teacher-schedule/:teacherName
  Response: Schedule[]
```

### Absence Management
```
GET /api/absences
  Response: Absence[]

POST /api/absences
  Body: { teacherId, date }
  Response: Absence

GET /api/get-absent-teachers
  Response: AbsentTeacher[]
```

### Substitute Assignment
```
POST /api/auto-assign-substitutes
  Response: Assignment[]

GET /api/substitute-assignments
  Response: Assignment[]

POST /api/reset-assignments
  Response: { success }
```

### Schedule Management
```
GET /api/schedule/:day
  Response: Schedule[]

POST /api/schedule
  Body: { day, periods }
  Response: Schedule

GET /api/period-config
  Response: PeriodConfig[]
```

## Mobile App

### Setup
```bash
cd mobile
npm install
```

### Key Features
1. Attendance Marking
   - Quick mark
   - Offline support
   - Batch upload
   - History view

2. Schedule Viewing
   - Daily view
   - Weekly view
   - Quick search
   - Offline access

3. Substitute Assignment
   - Accept/reject
   - View details
   - Contact info
   - Directions

4. Push Notifications
   - Assignment alerts
   - Schedule changes
   - System updates
   - Reminders

## Data Structures

### Teacher
```typescript
interface Teacher {
  id: number;
  name: string;
  phoneNumber: string;
  isSubstitute: boolean;
  specializations?: string[];
  availability?: string[];
  preferences?: {
    maxClassesPerDay: number;
    preferredClasses: string[];
  };
}
```

### Schedule
```typescript
interface Schedule {
  id: number;
  day: string;
  period: number;
  teacherId: number;
  className: string;
  subject?: string;
  room?: string;
}
```

### Absence
```typescript
interface Absence {
  id: number;
  teacherId: number;
  date: string;
  substituteId: number | null;
  reason?: string;
  documentation?: string[];
  status: 'pending' | 'approved' | 'completed';
}
```

### SMS Configuration
```typescript
interface SMSConfig {
  provider: string;
  apiKey: string;
  senderId: string;
  templates: {
    assignment: string;
    reminder: string;
    urgent: string;
  };
  retryConfig: {
    attempts: number;
    delay: number;
  };
}
```

## File Storage Structure
```
data/
├── absent_teachers.json     # Current absences
├── assigned_teacher.json    # Substitute assignments
├── class_schedules.json    # Class-wise schedules
├── day_schedules.json      # Day-wise schedules
├── period_config.json      # Period timings
├── teacher_schedules.json  # Teacher timetables
├── total_teacher.json      # All teachers
├── users.json             # User accounts
└── sms_history.json       # Message logs
```

## Development Guide

### Best Practices
1. Code Style
   - Follow TypeScript guidelines
   - Use async/await
   - Implement error handling
   - Add proper logging

2. File Organization
   - Group by feature
   - Separate concerns
   - Use clear naming
   - Maintain hierarchy

3. Error Handling
   - Graceful failures
   - User feedback
   - Error logging
   - Recovery options

4. Performance
   - Optimize queries
   - Cache results
   - Batch operations
   - Lazy loading

## Security Considerations

### Data Protection
- Secure file storage
- Access control
- Data encryption
- Regular backups

### Authentication
- Token-based auth
- Session management
- Password policies
- Role enforcement

### API Security
- Rate limiting
- Input validation
- Error masking
- CORS policy

## Troubleshooting

### Common Issues
1. File Access
   - Check permissions
   - Verify paths
   - Monitor storage
   - Handle locks

2. SMS Delivery
   - Verify credentials
   - Check balance
   - Monitor status
   - Handle failures

3. Assignment Failures
   - Validate data
   - Check conflicts
   - Review logs
   - Manual override

4. Mobile Sync
   - Check connectivity
   - Verify cache
   - Handle conflicts
   - Force refresh

For detailed implementation information, refer to the source code in the respective directories.

# Teacher Substitution Management System - Android App

## Overview
A fully offline-capable Android application for managing teacher substitutions. The app processes local CSV files containing timetable and substitute information to handle teacher absences and substitute assignments.

## Core Features
1. 100% Offline Functionality
   - Local CSV file processing
   - SQLite database for data storage
   - No internet connection required

2. Data Management
   - Processes timetable_file.csv
   - Processes Substitude_file.csv
   - Local data persistence
   - Efficient data structures

3. User Interface
   - Material Design 3 components
   - Bottom Navigation Bar
   - Home Dashboard
   - Teacher Management
   - Substitution View
   - Settings

## Project Structure
```
app/
├── src/
│   ├── main/
│   │   ├── java/com/example/substitution/
│   │   │   ├── data/
│   │   │   │   ├── models/
│   │   │   │   ├── database/
│   │   │   │   └── repository/
│   │   │   ├── ui/
│   │   │   │   ├── home/
│   │   │   │   ├── teachers/
│   │   │   │   ├── substitutions/
│   │   │   │   └── settings/
│   │   │   ├── utils/
│   │   │   └── MainActivity.kt
│   │   └── res/
│   │       ├── layout/
│   │       ├── values/
│   │       └── raw/
│   └── test/
└── build.gradle
```

## Data Models

### Teacher
```kotlin
data class Teacher(
    val id: Int,
    val name: String,
    val phoneNumber: String?,
    val isSubstitute: Boolean,
    val variations: List<String> = emptyList()
)
```

### Schedule
```kotlin
data class Schedule(
    val id: Int,
    val day: String,
    val period: Int,
    val teacherId: Int,
    val className: String
)
```

### Absence
```kotlin
data class Absence(
    val id: Int,
    val teacherId: Int,
    val date: String,
    val substituteId: Int?,
    val reason: String?,
    val status: AbsenceStatus
)

enum class AbsenceStatus {
    PENDING, APPROVED, COMPLETED
}
```

## Database Schema (SQLite)
```sql
CREATE TABLE teachers (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    phone_number TEXT,
    is_substitute BOOLEAN,
    variations TEXT
);

CREATE TABLE schedules (
    id INTEGER PRIMARY KEY,
    day TEXT NOT NULL,
    period INTEGER NOT NULL,
    teacher_id INTEGER,
    class_name TEXT,
    FOREIGN KEY (teacher_id) REFERENCES teachers(id)
);

CREATE TABLE absences (
    id INTEGER PRIMARY KEY,
    teacher_id INTEGER,
    date TEXT NOT NULL,
    substitute_id INTEGER,
    status TEXT,
    FOREIGN KEY (teacher_id) REFERENCES teachers(id),
    FOREIGN KEY (substitute_id) REFERENCES teachers(id)
);
```

## Implementation Steps

1. Project Setup
   - Create Android Studio project
   - Configure build.gradle with dependencies
   - Set up project structure

2. Database Implementation
   - Create Room database
   - Implement DAOs
   - Set up repositories

3. CSV Processing
   - Implement CSV parser
   - Create data models
   - Set up data loading

4. UI Implementation
   - Create bottom navigation
   - Implement home screen
   - Add teacher management
   - Create substitution view
   - Add settings screen

5. Business Logic
   - Implement substitute assignment algorithm
   - Add schedule conflict detection
   - Create notification system

## Dependencies
```gradle
dependencies {
    // Room
    implementation "androidx.room:room-runtime:2.6.1"
    kapt "androidx.room:room-compiler:2.6.1"
    implementation "androidx.room:room-ktx:2.6.1"
    
    // Coroutines
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
    
    // ViewModel
    implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0"
    implementation "androidx.lifecycle:lifecycle-livedata-ktx:2.7.0"
    
    // Navigation
    implementation "androidx.navigation:navigation-fragment-ktx:2.7.7"
    implementation "androidx.navigation:navigation-ui-ktx:2.7.7"
    
    // Material Design
    implementation "com.google.android.material:material:1.11.0"
}
```

## Getting Started
1. Clone the repository
2. Open project in Android Studio
3. Place CSV files in app/src/main/res/raw/
4. Build and run the application

## Offline Data Processing
1. CSV Files
   - Located in res/raw/
   - Processed on first app launch
   - Stored in SQLite database

2. Data Updates
   - Manual refresh option
   - Background processing
   - Error handling

3. Storage
   - SQLite for structured data
   - SharedPreferences for settings
   - File storage for logs
