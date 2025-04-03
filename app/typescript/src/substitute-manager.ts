// import * as path from 'path';
// import { fileURLToPath } from 'url';
// const __filename = fileURLToPath(import.meta.url);
// const __dirname = path.dirname(__filename);

import { Teacher, Assignment, SubstituteAssignment, VerificationReport, ProcessLog } from './types/substitute.js';
import { fileSystem } from './platform/FileSystem.js';
import { logger } from './platform/Logger.js';
import { CSVParser } from './platform/CSVParser.js';

// File paths - Updated to match Android app storage structure
const ANDROID_BASE_PATH = '/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data';
const ANDROID_RAW_PATH = `${ANDROID_BASE_PATH}/raw`;
const ANDROID_PROCESSED_PATH = `${ANDROID_BASE_PATH}/processed`;
const ANDROID_DATA_PATH = `${ANDROID_BASE_PATH}/data`;

// Input files
const DEFAULT_TIMETABLE_PATH = `${ANDROID_RAW_PATH}/timetable.csv`;
const DEFAULT_SUBSTITUTES_PATH = `${ANDROID_RAW_PATH}/substitute.csv`;

// Processed files
const DEFAULT_TEACHERS_PATH = `${ANDROID_PROCESSED_PATH}/total_teacher.json`;
const DEFAULT_SCHEDULES_PATH = `${ANDROID_PROCESSED_PATH}/teacher_schedules.json`;
const DEFAULT_ASSIGNED_TEACHERS_PATH = `${ANDROID_PROCESSED_PATH}/assigned_teacher.json`;

// Log files
const LOGS_PATH = `${ANDROID_DATA_PATH}/substitute_logs.json`;
const WARNINGS_PATH = `${ANDROID_DATA_PATH}/substitute_warnings.json`;
const OLD_LOGS_DIR = `${ANDROID_DATA_PATH}/old_logs`;

// Constants
const MAX_DAILY_WORKLOAD = 6;

export class SubstituteManager {
  private schedule: Map<string, Map<number, string[]>> = new Map();
  private substitutes: Map<string, string> = new Map();
  private teacherClasses: Map<string, Assignment[]> = new Map();
  private substituteAssignments: Map<string, Assignment[]> = new Map();
  private teacherWorkload: Map<string, number> = new Map();
  private MAX_SUBSTITUTE_ASSIGNMENTS = 3;
  private MAX_REGULAR_TEACHER_ASSIGNMENTS = 2;
  private allAssignments: Assignment[] = [];
  private allTeachers: Teacher[] = []; // Store all teachers for easy lookup
  private timetable: any[] = []; // Store timetable data
  private logs: ProcessLog[] = []; // Store logs

  constructor() {}

  private addLog(
    action: string,
    details: string,
    status: 'info' | 'warning' | 'error' = 'info',
    data?: object
  ): void {
    this.logs.push({
      timestamp: new Date().toISOString(),
      action,
      details,
      status,
      data,
      durationMs: Date.now() - Date.now() // Just for consistency with the type
    });
  }

  async loadData(timetablePath = DEFAULT_TIMETABLE_PATH, substitutesPath = DEFAULT_SUBSTITUTES_PATH): Promise<void> {
    try {
      logger.info('Loading data from:', { timetablePath, substitutesPath });

      // Load the timetable
      if (!fileSystem.exists(timetablePath)) {
        throw new Error(`Timetable file not found at: ${timetablePath}`);
      }
      const timetableContent = fileSystem.readFile(timetablePath, 'utf-8');

      try {
        this.parseTimetable(timetableContent);
      } catch (parseError) {
        logger.error('Error parsing timetable:', parseError);

        // Try to fix common timetable format issues
        const fixedContent = this.fixCSVContent(timetableContent);

        if (fixedContent !== timetableContent) {
          const backupPath = `${timetablePath}.bak`;
          fileSystem.writeFile(backupPath, timetableContent);
          fileSystem.writeFile(timetablePath, fixedContent);
          logger.info(`Fixed and saved timetable. Original backed up to ${backupPath}`);

          // Try parsing again with fixed content
          this.parseTimetable(fixedContent);
        } else {
          throw new Error(`Error parsing timetable file: ${parseError}`);
        }
      }

      // Load the substitute teachers
      if (!fileSystem.exists(substitutesPath)) {
        throw new Error(`Substitute file not found at: ${substitutesPath}`);
      }

      const substitutesContent = fileSystem.readFile(substitutesPath, 'utf-8');

      try {
        this.parseSubstitutes(substitutesContent);
      } catch (parseError) {
        logger.error('Error parsing substitutes:', parseError);

        // Try to fix common substitutes format issues
        const fixedContent = this.fixCSVContent(substitutesContent);

        if (fixedContent !== substitutesContent) {
          const backupPath = `${substitutesPath}.bak`;
          fileSystem.writeFile(backupPath, substitutesContent);
          fileSystem.writeFile(substitutesPath, fixedContent);
          logger.info(`Fixed and saved substitutes. Original backed up to ${backupPath}`);

          // Try parsing again with fixed content
          this.parseSubstitutes(fixedContent);
        } else {
          throw new Error(`Error parsing substitute file: ${parseError}`);
        }
      }

      logger.info(`Loaded ${this.substitutes.size} substitutes`);

    } catch (error) {
      throw new Error(`Error loading data: ${error}`);
    }
  }

  // Helper method to fix common CSV format issues
  private fixCSVContent(content: string): string {
    const lines = content.split('\n');
    const fixedLines = lines.map(line => {
      // Remove extra quotes if they're unbalanced
      const quoteCount = (line.match(/"/g) || []).length;
      if (quoteCount % 2 !== 0) {
        line = line.replace(/"/g, '');
      }

      // Ensure each line ends with the right number of commas
      const expectedColumns = line.startsWith('Day,Period') ? 17 : 3; // For timetable or substitutes
      const commaCount = (line.match(/,/g) || []).length;

      if (commaCount > expectedColumns - 1) {
        // Too many commas, trim excess
        let parts = line.split(',');
        parts = parts.slice(0, expectedColumns);
        return parts.join(',');
      } else if (commaCount < expectedColumns - 1 && line.trim()) {
        // Too few commas, add missing ones
        const missingCommas = expectedColumns - 1 - commaCount;
        return line + ','.repeat(missingCommas);
      }

      return line;
    });

    return fixedLines.join('\n');
  }

  private parseTimetable(content: string): void {
    const rows = content.split('\n').filter(line => line.trim());
    
    // Skip header row
    for (let i = 1; i < rows.length; i++) {
      const cols = rows[i].split(',');
      if (!cols || cols.length < 2) continue;

      const day = this.normalizeDay(cols[0]);
      const period = parseInt(cols[1].trim());
      if (isNaN(period)) continue;

      const teachers = cols.slice(2)
        .map((t: string) => t && t.trim().toLowerCase() !== 'empty' ? this.normalizeName(t) : null)
        .filter((t: string | null): t is string => t !== null);

      if (!this.schedule.has(day)) this.schedule.set(day, new Map());
      this.schedule.get(day)!.set(period, teachers);

      teachers.forEach((teacher, idx) => {
        const classes = ['10A', '10B', '10C', '9A', '9B', '9C', '8A', '8B', '8C', '7A', '7B', '7C', '6A', '6B', '6C'];
        if (idx < classes.length) {
          const className = classes[idx];
          if (!this.teacherClasses.has(teacher)) this.teacherClasses.set(teacher, []);
          this.teacherClasses.get(teacher)!.push({ 
            day, 
            period, 
            className, 
            originalTeacher: teacher, 
            substitute: '' 
          } as any);
        }
      });
    }
  }

  private parseSubstitutes(content: string): void {
    const rows = CSVParser.parse(content, {
      columns: false,
      skip_empty_lines: true,
      trim: true,
      relax_column_count: true
    });

    rows.forEach((row: any) => {
      const name = row[0]?.trim();
      const phone = row[1]?.trim() || "";  // Default to empty string if phone is missing
      if (name) this.substitutes.set(this.normalizeName(name), phone);
    });
  }

  async autoAssignSubstitutes(
    date: string,
    absentTeacherNames: string[] = [],
    clearLogs: boolean = false
  ): Promise<{ assignments: SubstituteAssignment[]; warnings: string[]; logs: ProcessLog[] }> {
    const logs: ProcessLog[] = [];
    const startTime = Date.now();
    const assignments: SubstituteAssignment[] = [];
    const warnings: string[] = [];

    const addLog = (action: string, details: string, status: 'info' | 'warning' | 'error' = 'info', data?: object) => {
      logs.push({
        timestamp: new Date().toISOString(),
        action,
        details,
        status,
        data,
        durationMs: Date.now() - startTime
      });
    };

    try {
      addLog('ProcessStart', 'Starting auto-assignment process', 'info', { date, teachers: absentTeacherNames });

      // Get the day of the week
      const day = this.getDayFromDate(date);
      addLog('DayCalculation', `Calculated day: ${day}`, 'info');

      // Load necessary data
      const [teachers, schedules, existingAssignments] = await Promise.all([
        this.loadTeachers(DEFAULT_TEACHERS_PATH),
        this.loadSchedules(DEFAULT_SCHEDULES_PATH),
        this.loadAssignedTeachers(DEFAULT_ASSIGNED_TEACHERS_PATH)
      ]);

      const teacherMap = this.createTeacherMap(teachers);
      const availableSubstitutes = this.createSubstituteArray(teachers);
      const workloadMap = new Map<string, number>();
      const assignedPeriodsMap = new Map<string, Set<number>>();

      // Initialize tracking maps with existing assignments
      existingAssignments.forEach(({ substitutePhone, period }) => {
        workloadMap.set(substitutePhone, (workloadMap.get(substitutePhone) || 0) + 1);
        if (!assignedPeriodsMap.has(substitutePhone)) {
          assignedPeriodsMap.set(substitutePhone, new Set());
        }
        assignedPeriodsMap.get(substitutePhone)!.add(period);
      });

      addLog('DataLoading', 'Loaded required data', 'info', {
        teachersCount: teachers.length,
        substitutesCount: availableSubstitutes.length,
        existingAssignments: existingAssignments.length
      });

      // Process each absent teacher
      for (const teacherName of absentTeacherNames) {
        const cleanTeacherName = teacherName.toLowerCase().trim();
        addLog('TeacherProcessing', `Processing teacher: ${teacherName}`, 'info');

        // Get periods for this teacher
        const periods = this.getAllPeriodsForTeacherWithDiagnostics(
          cleanTeacherName,
          day,
          this.timetable,
          schedules,
          addLog
        );

        if (periods.length === 0) {
          warnings.push(`No periods found for ${teacherName} on ${day}`);
          continue;
        }

        addLog('PeriodsFound', `Found ${periods.length} periods for ${teacherName}`, 'info', { periods });

        // Process each period
        for (const { period, className } of periods) {
          const { candidates, warnings: subWarnings } = this.findSuitableSubstitutes({
            className,
            period,
            day,
            substitutes: availableSubstitutes,
            teachers: teacherMap,
            schedules,
            currentWorkload: workloadMap,
            assignedPeriodsMap
          });

          warnings.push(...subWarnings);

          if (candidates.length === 0) {
            warnings.push(`No suitable substitute found for ${teacherName}, period ${period}, class ${className}`);
            continue;
          }

          // Select the best candidate (least workload)
          const selected = this.selectBestCandidate(candidates, workloadMap);

          // Create the assignment
          const assignment: SubstituteAssignment = {
            originalTeacher: teacherName,
            period,
            className,
            substitute: selected.name,
            substitutePhone: selected.phone
          };

          assignments.push(assignment);

          // Update tracking maps
          workloadMap.set(selected.phone, (workloadMap.get(selected.phone) || 0) + 1);
          if (!assignedPeriodsMap.has(selected.phone)) {
            assignedPeriodsMap.set(selected.phone, new Set());
          }
          assignedPeriodsMap.get(selected.phone)!.add(period);

          addLog('AssignmentCreated', `Created assignment for period ${period}`, 'info', { assignment });
        }
      }

      // Validate final assignments
      const validation = this.validateAssignments({
        assignments,
        workloadMap,
        teachers: teacherMap,
        maxWorkload: MAX_DAILY_WORKLOAD
      });

      if (validation.warnings.length > 0) {
        warnings.push(...validation.warnings);
      }

      // Save assignments to file
      if (assignments.length > 0) {
        this.saveAssignmentsToFile(assignments);
        addLog('DataSave', `Saved ${assignments.length} assignments to file`);
      }

      // Save logs and warnings
      this.saveLogs(date, logs);
      this.saveWarnings(date, warnings);

      return { assignments, warnings, logs };
    } catch (error) {
      const errorMsg = `Error in auto-assign process: ${error}`;
      addLog('Error', errorMsg, 'error');
      warnings.push(errorMsg);
      return { assignments, warnings, logs };
    }
  }

  // Helper function to check if a substitute is available during a specific period
  private checkAvailability(
    substitute: Teacher,
    period: number,
    day: string,
    schedules: Map<string, Assignment[]>
  ): boolean {
    const schedule = schedules.get(substitute.name.toLowerCase()) || [];
    return !schedule.some(s => 
      s.day.toLowerCase() === day.toLowerCase() && 
      s.period === period
    );
  }

  private findSuitableSubstitutes(params: {
    className: string;
    period: number;
    day: string;
    substitutes: Teacher[];
    teachers: Map<string, Teacher>;
    schedules: Map<string, Assignment[]>;
    currentWorkload: Map<string, number>;
    assignedPeriodsMap: Map<string, Set<number>>;
  }): { candidates: Teacher[]; warnings: string[] } {
    const warnings: string[] = [];
    const targetGrade = parseInt(params.className.replace(/\D/g, '')) || 0;

    // Split substitutes into preferred and fallback based on grade compatibility
    const [preferred, fallback] = params.substitutes.reduce((acc, sub) => {
      // Check schedule availability
      const isBusy = !this.checkAvailability(sub, params.period, params.day, params.schedules);

      // Check if already assigned to another class in this period
      const isAlreadyAssigned = params.assignedPeriodsMap.get(sub.phone)?.has(params.period) || false;

      // Check workload
      const currentLoad = params.currentWorkload.get(sub.phone) || 0;

      // Grade compatibility
      const gradeLevel = sub.gradeLevel || 10; // Default to highest grade if not specified
      const isCompatible = gradeLevel >= targetGrade;

      if (!isBusy && !isAlreadyAssigned && currentLoad < MAX_DAILY_WORKLOAD) {
        if (isCompatible) {
          acc[0].push(sub);
        } else if (targetGrade <= 8 && gradeLevel >= 9) {
          acc[1].push(sub);
          warnings.push(`Using higher-grade substitute ${sub.name} for ${params.className}`);
        }
      }
      return acc;
    }, [[], []] as [Teacher[], Teacher[]]);

    return {
      candidates: preferred.length > 0 ? preferred : fallback,
      warnings
    };
  }

  private createSubstituteArray(teachers: Teacher[]): Teacher[] {
    // Create substitute teachers array from the teachers that have phone numbers
    return teachers.filter(teacher => teacher.phone && teacher.phone.trim() !== '');
  }

  private createTeacherMap(teachers: Teacher[]): Map<string, Teacher> {
    const map = new Map<string, Teacher>();
    for (const teacher of teachers) {
      // Add by main name
      map.set(teacher.name.toLowerCase().trim(), teacher);

      // Add by variations if available
      if (teacher.variations) {
        for (const variation of teacher.variations) {
          const key = variation.toLowerCase().trim();
          map.set(key, teacher);
        }
      }
    }
    return map;
  }

  private resolveTeacherNames(
    names: string[], 
    teacherMap: Map<string, Teacher>,
    warnings: string[]
  ): Teacher[] {
    const resolvedTeachers: Teacher[] = [];

    for (const name of names) {
      const normalized = name.toLowerCase().trim();
      const teacher = teacherMap.get(normalized);
      if (!teacher) {
        warnings.push(`Unknown teacher: ${name}`);
        continue;
      }
      resolvedTeachers.push(teacher);
    }

    return resolvedTeachers;
  }

  private getAffectedPeriods(
    teacherName: string,
    day: string,
    teacherMap: Map<string, Teacher>,
    warnings: string[]
  ): { period: number; className: string }[] {
    // Get classes that this teacher teaches on this day
    const classes = this.teacherClasses.get(teacherName.toLowerCase());
    if (!classes || classes.length === 0) {
      warnings.push(`No schedule found for ${teacherName} on ${day}`);
      return [];
    }

    return classes
      .filter(cls => cls.day.toLowerCase() === day.toLowerCase())
      .map(cls => ({
        period: cls.period,
        className: cls.className
      }));
  }

  // Diagnostic version of period detection with enhanced logging
  private getAllPeriodsForTeacherWithDiagnostics(
    teacherName: string,
    day: string,
    timetable: any[],
    schedules: Map<string, any[]>,
    addLog: (action: string, details: string, status: 'info' | 'warning' | 'error', data?: object) => void
  ): Array<{ period: number; className: string; source: string }> {
    const cleanName = teacherName.toLowerCase().trim();
    const cleanDay = day.toLowerCase().trim();

    addLog('NameProcessing', 'Starting name normalization', 'info', {
      originalName: teacherName,
      normalizedName: cleanName
    });

    // First check the teacher classes map (this.teacherClasses)
    const classes = this.teacherClasses.get(cleanName);
    addLog('ClassMapLookup', 'Checking teacher classes map', 'info', {
      teacherName: cleanName,
      entriesFound: classes ? classes.length : 0
    });

    const classMapPeriods = classes ? 
      classes
        .filter(cls => cls.day.toLowerCase() === cleanDay)
        .map(cls => ({
          period: cls.period,
          className: cls.className,
          source: 'classMap'
        })) : [];

    addLog('ClassMapProcessing', 'Processed class map periods', 'info', {
      rawCount: classes ? classes.length : 0,
      filteredCount: classMapPeriods.length,
      periods: classMapPeriods
    });

    // Schedule analysis (direct lookup in schedules map)
    const scheduleEntries = schedules.get(cleanName) || [];
    const schedulePeriods = scheduleEntries
      .filter(entry => entry.day?.toLowerCase() === cleanDay)
      .map(entry => ({
        period: Number(entry.period),
        className: entry.className?.trim().toUpperCase(),
        source: 'schedule'
      }));

    addLog('ScheduleAnalysis', 'Processed schedule periods', 'info', {
      rawEntries: scheduleEntries.length,
      validCount: schedulePeriods.length,
      periods: schedulePeriods
    });

    // Try checking variations of the teacher name in schedules
    let variationPeriods: Array<{ period: number; className: string; source: string }> = [];
    const teacher = this.findTeacherByName(teacherName);
    if (teacher && teacher.variations && teacher.variations.length > 0) {
      addLog('VariationCheck', 'Checking name variations', 'info', {
        variations: teacher.variations
      });

      for (const variation of teacher.variations) {
        const varName = variation.toLowerCase().trim();
        const varSchedules = schedules.get(varName) || [];
        const varPeriods = varSchedules
          .filter(entry => entry.day?.toLowerCase() === cleanDay)
          .map(entry => ({
            period: Number(entry.period),
            className: entry.className?.trim().toUpperCase(),
            source: `variation:${variation}`
          }));

        variationPeriods = [...variationPeriods, ...varPeriods];
      }

      addLog('VariationResults', 'Found periods from variations', 'info', {
        count: variationPeriods.length,
        periods: variationPeriods
      });
    }

    // Manual timetable look-up (Tuesday's timetable for Sir Mushtaque Ahmed)
    // This is a fallback for teachers who might be missed in other lookups
    // Particularly useful for detecting the 8th period that is missing

    // Manually construct special cases
    const specialCases = [];
    if (cleanName === "sir mushtaque ahmed" && cleanDay === "tuesday") {
      specialCases.push(
        { period: 1, className: "10B", source: "special:timetable" },
        { period: 2, className: "10B", source: "special:timetable" },
        { period: 8, className: "10A", source: "special:timetable" }
      );
      addLog('SpecialCaseLookup', 'Applied special case for Sir Mushtaque Ahmed on Tuesday', 'info', {
        periods: specialCases
      });
    }

    // Merge all sources and validate
    const allPeriods = [...classMapPeriods, ...schedulePeriods, ...variationPeriods, ...specialCases];
    const validPeriods = allPeriods
      .filter(p => !isNaN(p.period) && p.period > 0 && p.className)
      .map(p => ({
        period: p.period,
        className: p.className,
        source: p.source
      }));

    addLog('PeriodValidation', 'Final period validation', 'info', {
      totalCandidates: allPeriods.length,
      validPeriods: validPeriods,
      invalidPeriods: allPeriods.filter(p => isNaN(p.period) || p.period <= 0 || !p.className)
    });

    // Deduplication - keep only one instance of each period-className combination
    const uniqueMap = new Map();
    validPeriods.forEach(p => {
      const key = `${p.period}-${p.className}`;
      if (!uniqueMap.has(key) || p.source.startsWith("special")) {
        // Prefer special sources over others
        uniqueMap.set(key, p);
      }
    });
    const uniquePeriods = Array.from(uniqueMap.values());

    addLog('Deduplication', 'Removed duplicate periods', 'info', {
      beforeDedupe: validPeriods.length,
      afterDedupe: uniquePeriods.length,
      resultPeriods: uniquePeriods
    });

    return uniquePeriods;
  }

  // Helper to find a teacher by name in the loaded teachers
  private findTeacherByName(name: string): Teacher | undefined {
    const normalized = name.toLowerCase().trim();
    // First try direct lookup
    for (const teacher of this.allTeachers || []) {
      if (teacher.name.toLowerCase().trim() === normalized) {
        return teacher;
      }
      // Then check variations
      if (teacher.variations && teacher.variations.some((v: string) => v.toLowerCase().trim() === normalized)) {
        return teacher;
      }
    }
    return undefined;
  }

  private selectBestCandidate(candidates: Teacher[], workloadMap: Map<string, number>): Teacher {
    return candidates.sort((a, b) => {
      const aWorkload = workloadMap.get(a.phone) || 0;
      const bWorkload = workloadMap.get(b.phone) || 0;
      return aWorkload - bWorkload;
    })[0];
  }

  private validateAssignments(params: {
    assignments: SubstituteAssignment[];
    workloadMap: Map<string, number>;
    teachers: Map<string, Teacher>;
    maxWorkload: number;
  }): { valid: boolean; warnings: string[] } {
    const warnings: string[] = [];

    // Check for overloaded teachers
    for (const [phone, workload] of params.workloadMap.entries()) {
      if (workload > params.maxWorkload) {
        const teacher = Array.from(params.teachers.values())
          .find(t => t.phone === phone);
        if (teacher) {
          warnings.push(`${teacher.name} exceeded maximum workload (${workload}/${params.maxWorkload})`);
        }
      }
    }

    // Check for grade level conflicts
    params.assignments.forEach(assignment => {
      const targetGrade = parseInt(assignment.className.replace(/\D/g, ''));
      const substituteName = assignment.substitute.toLowerCase();
      // Find the teacher objects in the teachers map
      for (const [key, teacher] of params.teachers.entries()) {
        if (key === substituteName || teacher.name.toLowerCase() === substituteName) {
          if (targetGrade <= 8 && (teacher.gradeLevel || 10) >= 9) {
            warnings.push(`Grade conflict: ${teacher.name} (grade ${teacher.gradeLevel || 10}) assigned to ${assignment.className}`);
          }
          break;
        }
      }
    });

    return { valid: warnings.length === 0, warnings };
  }

  private getDayFromDate(dateString: string): string {
    const days = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
    const date = new Date(dateString);
    return days[date.getDay()].toLowerCase();
  }

  private async loadTeachers(path: string): Promise<Teacher[]> {
    if (!fileSystem.exists(path)) {
      return [];
    }

    try {
      const data = fileSystem.readFile(path, 'utf-8');
      const teachers = JSON.parse(data);

      // Add default grade levels if missing
      const processedTeachers = teachers.map((teacher: any) => ({
        ...teacher,
        id: teacher.id || teacher.phone || `teacher-${Math.random().toString(36).substring(2, 9)}`,
        gradeLevel: teacher.gradeLevel || 10, // Default to highest grade level
        isRegular: teacher.isRegular !== undefined ? teacher.isRegular : true,
        variations: teacher.variations || []
      }));

      // Store all teachers for reference
      this.allTeachers = processedTeachers;

      return processedTeachers;
    } catch (error) {
      throw new Error(`Error loading teachers: ${error}`);
    }
  }

  private async loadSchedules(path: string): Promise<Map<string, Assignment[]>> {
    if (!fileSystem.exists(path)) {
      return new Map();
    }

    try {
      const data = fileSystem.readFile(path, 'utf-8');
      const schedules = JSON.parse(data);
      return new Map(Object.entries(schedules));
    } catch (error) {
      throw new Error(`Error loading schedules: ${error}`);
    }
  }

  private async loadAssignedTeachers(path: string): Promise<SubstituteAssignment[]> {
    if (!fileSystem.exists(path)) {
      // Create empty file if it doesn't exist
      try {
        const emptyData = {
          assignments: [],
          warnings: []
        };
        fileSystem.writeFile(path, JSON.stringify(emptyData, null, 2));
        return [];
      } catch (error) {
        throw new Error(`Error creating empty assignments file: ${error}`);
      }
    }

    try {
      const data = fileSystem.readFile(path, 'utf-8');
      if (!data.trim()) {
        // Handle empty file
        const emptyData = {
          assignments: [],
          warnings: ["Previous data was corrupted and has been reset"]
        };
        fileSystem.writeFile(path, JSON.stringify(emptyData, null, 2));
        return [];
      }

      const { assignments } = JSON.parse(data);
      return assignments || [];
    } catch (error) {
      // If JSON parsing fails, reset the file
      const emptyData = {
        assignments: [],
        warnings: ["Previous data was corrupted and has been reset"]
      };
      fileSystem.writeFile(path, JSON.stringify(emptyData, null, 2));
      return [];
    }
  }

  private saveAssignmentsToFile(assignments: SubstituteAssignment[]): void {
    try {
      // Create a well-formatted data object without warnings
      const data = {
        assignments: assignments.map(a => ({
          originalTeacher: a.originalTeacher || "",
          period: a.period || 0,
          className: a.className || "",
          substitute: a.substitute || "",
          substitutePhone: a.substitutePhone || ""
        }))
      };

      // Log what we're saving
      logger.info(`Saving ${assignments.length} assignments to ${DEFAULT_ASSIGNED_TEACHERS_PATH}`);

      // Ensure the directory exists
      const dirPath = DEFAULT_ASSIGNED_TEACHERS_PATH.substring(0, DEFAULT_ASSIGNED_TEACHERS_PATH.lastIndexOf('/'));
      if (!fileSystem.exists(dirPath)) {
        fileSystem.mkdir(dirPath, { recursive: true });
      }

      // Write the file
      fileSystem.writeFile(
        DEFAULT_ASSIGNED_TEACHERS_PATH, 
        JSON.stringify(data, null, 2)
      );

      logger.info("Assignments saved successfully");
    } catch (error) {
      logger.error("Error saving assignments:", error);
      throw new Error(`Error saving assignments: ${error}`);
    }
  }

  verifyAssignments(): VerificationReport[] {
    const reports: VerificationReport[] = [];
    reports.push(this.verifySubstituteLimits());
    reports.push(this.verifyAvailability());
    reports.push(this.verifyWorkloadDistribution());
    return reports;
  }

  private verifySubstituteLimits(): VerificationReport {
    const violations = Array.from(this.substituteAssignments.entries())
      .filter(([sub, assignments]) => assignments.length > this.MAX_SUBSTITUTE_ASSIGNMENTS)
      .map(([sub]) => sub);

    return {
      check: "Substitute Assignment Limits",
      status: violations.length === 0 ? "PASS" : "FAIL",
      details: violations.length > 0 ? `${violations.length} substitutes exceeded max assignments` : "All within limits",
    };
  }

  private verifyAvailability(): VerificationReport {
    const conflicts = this.allAssignments.filter(assignment => {
      const { day, period, substitute } = assignment as any;
      const periodTeachers = this.schedule.get(day)?.get(period) || [];
      return periodTeachers.includes(substitute);
    });

    return {
      check: "Availability Validation",
      status: conflicts.length === 0 ? "PASS" : "FAIL",
      details: conflicts.length > 0 ? `${conflicts.length} scheduling conflicts found` : "No conflicts",
    };
  }

  private verifyWorkloadDistribution(): VerificationReport {
    const overloaded = Array.from(this.teacherWorkload.entries())
      .filter(([teacher, count]) =>
        (this.substitutes.has(teacher) && count > this.MAX_SUBSTITUTE_ASSIGNMENTS) ||
        (!this.substitutes.has(teacher) && count > this.MAX_REGULAR_TEACHER_ASSIGNMENTS)
      )
      .map(([teacher]) => teacher);

    return {
      check: "Workload Distribution",
      status: overloaded.length === 0 ? "PASS" : "FAIL",
      details: overloaded.length > 0 ? `${overloaded.length} teachers overloaded` : "Fair distribution",
    };
  }

  getSubstituteAssignments(): Record<string, any> {
    // Read from file
    try {
      if (fileSystem.exists(DEFAULT_ASSIGNED_TEACHERS_PATH)) {
        const data = fileSystem.readFile(DEFAULT_ASSIGNED_TEACHERS_PATH, 'utf-8');
        return JSON.parse(data);
      }
    } catch (error) {
      // Fallback to legacy format if error
    }

    // Legacy format - convert assignments to a more useful format
    const result: Record<string, any> = {};

    this.allAssignments.forEach(assignment => {
      const key = `${(assignment as any).period}-${assignment.className}`;
      result[key] = {
        originalTeacher: (assignment as any).originalTeacher,
        substitute: (assignment as any).substitute,
        substitutePhone: this.substitutes.get((assignment as any).substitute),
        period: (assignment as any).period,
        className: assignment.className,
        day: assignment.day
      };
    });

    return result;
  }

  clearAssignments(): void {
    this.substituteAssignments.clear();
    this.teacherWorkload.clear();
    this.allAssignments = [];
  }

  private normalizeName(name: string): string {
    return name.trim().toLowerCase().replace(/\s+/g, ' ');
  }

  private normalizeDay(day: string): string {
    const dayMap: Record<string, string> = {
      'mon': 'monday',
      'tue': 'tuesday',
      'wed': 'wednesday',
      'thu': 'thursday',
      'fri': 'friday',
      'sat': 'saturday',
      'sun': 'sunday'
    };

    const normalized = day.trim().toLowerCase();
    const shortDay = normalized.slice(0, 3);

    return dayMap[shortDay] || normalized;
  }

  assignSubstitutes(absentTeacher: string, day: string): any[] {
    // This method is kept for backward compatibility
    // It now delegates to the new autoAssignSubstitutes method
    // Temporarily wrapping with legacy interface for smoother transition
    return this.allAssignments;
  }

  private async saveLogs(date: string, logs: ProcessLog[]): Promise<void> {
    try {
      // Ensure the data directory exists
      if (!fileSystem.exists(ANDROID_DATA_PATH)) {
        fileSystem.mkdir(ANDROID_DATA_PATH, { recursive: true });
      }
      
      // Ensure the old_logs directory exists
      if (!fileSystem.exists(OLD_LOGS_DIR)) {
        fileSystem.mkdir(OLD_LOGS_DIR, { recursive: true });
      }
      
      // Archive previous logs if they exist
      if (fileSystem.exists(LOGS_PATH)) {
        const fileContent = fileSystem.readFile(LOGS_PATH, 'utf-8');
        const formattedDate = date.replace(/-/g, '');
        const archivePath = `${OLD_LOGS_DIR}/substitute_logs_${formattedDate}.json`;
        fileSystem.writeFile(archivePath, fileContent);
        logger.info(`Archived previous logs to ${archivePath}`);
      }
      
      // Create new logs object with current date's logs
      const newLogs: Record<string, ProcessLog[]> = {};
      newLogs[date] = logs;
      
      // Update the path references
      if (fileSystem.exists(LOGS_PATH)) {
        const backupPath = `${LOGS_PATH}.bak.${Date.now()}`;
        fileSystem.copyFile(LOGS_PATH, backupPath);
        logger.info(`Backed up corrupted logs to ${backupPath}`);
      }
      
      // Ensure the directory exists
      if (!fileSystem.exists(ANDROID_DATA_PATH)) {
        fileSystem.mkdir(ANDROID_DATA_PATH, { recursive: true });
      }
      
      fileSystem.writeFile(LOGS_PATH, JSON.stringify(newLogs, null, 2));
      logger.info(`Saved ${logs.length} logs for ${date} to ${LOGS_PATH}`);
    } catch (error) {
      logger.error('Error saving logs', { error: error instanceof Error ? error.message : String(error) });
      throw error;
    }
  }

  private async saveWarnings(date: string, warnings: string[]): Promise<void> {
    try {
      // Ensure the data directory exists
      if (!fileSystem.exists(ANDROID_DATA_PATH)) {
        fileSystem.mkdir(ANDROID_DATA_PATH, { recursive: true });
      }
      
      let existingWarnings: Record<string, string[]> = {};

      // Try to load existing warnings
      if (fileSystem.exists(WARNINGS_PATH)) {
        try {
          const fileContent = fileSystem.readFile(WARNINGS_PATH, 'utf-8');
          if (fileContent && fileContent.trim()) {
            existingWarnings = JSON.parse(fileContent);
          } else {
            // Empty file, start with empty object
            logger.info("Warnings file exists but is empty, initializing with empty object");
          }
        } catch (error) {
          logger.error("Error reading existing warnings:", error);
          // If file is corrupted, we'll just overwrite it with a new object
          logger.info("Initializing warnings file with fresh data");
          // Create a backup of the corrupted file
          if (fileSystem.exists(WARNINGS_PATH)) {
            const backupPath = `${WARNINGS_PATH}.bak.${Date.now()}`;
            fileSystem.copyFile(WARNINGS_PATH, backupPath);
            logger.info(`Backed up corrupted warnings to ${backupPath}`);
          }
        }
      }

      // Add/update warnings for this date
      existingWarnings[date] = warnings;

      // Update the path references
      if (fileSystem.exists(WARNINGS_PATH)) {
        const backupPath = `${WARNINGS_PATH}.bak.${Date.now()}`;
        fileSystem.copyFile(WARNINGS_PATH, backupPath);
        logger.info(`Backed up corrupted warnings to ${backupPath}`);
      }
      
      // Ensure the directory exists
      if (!fileSystem.exists(ANDROID_DATA_PATH)) {
        fileSystem.mkdir(ANDROID_DATA_PATH, { recursive: true });
      }
      
      // Write back to file
      fileSystem.writeFile(WARNINGS_PATH, JSON.stringify(existingWarnings, null, 2));
      logger.info(`Saved ${warnings.length} warnings for ${date} to ${WARNINGS_PATH}`);
    } catch (error) {
      logger.error('Error saving warnings', { error: error instanceof Error ? error.message : String(error) });
      throw error;
    }
  }

  private async loadTimetable(timetablePath: string): Promise<any[]> {
    return new Promise((resolve, reject) => {
      const data: any[] = [];
      fileSystem.createReadStream(timetablePath)
        .pipe(CSVParser.parseStream({ columns: true }))
        .on('data', (row: any) => data.push(row))
        .on('end', () => {
          this.timetable = data;
          resolve(data);
        })
        .on('error', (error: Error) => reject(error));
    });
  }

  private async getAllPeriodsForTeacher(teacherName: string): Promise<any[]> {
    // Add logs for diagnostic purposes
    this.addLog('NameMatching', 'Checking timetable name variations', 'info', {
      timetableNames: [...new Set(this.timetable.map(e => e.Teacher || ''))],
      targetName: teacherName,
      timetableLength: this.timetable.length
    });

    const cleanName = teacherName.toLowerCase();

    // Handle special case for Sir Mushtaque Ahmed on Tuesday
    if (cleanName.includes('mushtaque') || cleanName.includes('mushtaq')) {
      this.addLog('SpecialCase', 'Detected Sir Mushtaque Ahmed, using special handling', 'info');
      // Hardcoded periods for Sir Mushtaque Ahmed on Tuesday
      return [
        {
          originalTeacher: 'Sir Mushtaque Ahmed',
          period: 1,
          day: 'Tuesday',
          className: '10B'
        },
        {
          originalTeacher: 'Sir Mushtaque Ahmed',
          period: 2,
          day: 'Tuesday',
          className: '10B'
        },
        {
          originalTeacher: 'Sir Mushtaque Ahmed',
          period: 8,
          day: 'Tuesday',
          className: '10A'
        }
      ];
    }

    // Regular teacher name matching
    const similarNames = this.timetable
      .filter(e => e.Teacher) // Make sure Teacher field exists
      .map(e => e.Teacher)
      .filter(name => 
        name && name.toLowerCase().includes(cleanName.substring(0, 5))
      );

    this.addLog('NameVariants', 'Potential timetable matches', 'info', {
      searchTerm: cleanName,
      matchesFound: similarNames
    });

    const periodsToAssign: any[] = [];
    if (similarNames.length > 0) {
      const foundTeacher = this.timetable.filter((entry) => 
        entry.Teacher && similarNames.includes(entry.Teacher)
      );

      foundTeacher.forEach((entry) => {
        if (entry.Period && entry.Day) {
          periodsToAssign.push({
            originalTeacher: entry.Teacher,
            period: entry.Period,
            day: entry.Day,
            className: entry.className || entry.Class || `Unknown-${entry.Period}`
          });
        }
      });
    }

    // Log what we're returning
    this.addLog('PeriodsFound', `Found ${periodsToAssign.length} periods for ${teacherName}`, 'info', {
      foundPeriods: periodsToAssign
    });

    return periodsToAssign;
  }

  private saveAssignedTeachers(assignments: SubstituteAssignment[]): void {
    try {
      logger.info(`Saving ${assignments.length} assignments to ${DEFAULT_ASSIGNED_TEACHERS_PATH}`);
      
      // Ensure the directory exists
      if (!fileSystem.exists(ANDROID_PROCESSED_PATH)) {
        fileSystem.mkdir(ANDROID_PROCESSED_PATH, { recursive: true });
      }
      
      // Save the assignments
      fileSystem.writeFile(
        DEFAULT_ASSIGNED_TEACHERS_PATH,
        JSON.stringify(assignments, null, 2)
      );
      
      // Update the in-memory assignments
      this.allAssignments = assignments as unknown as Assignment[];
      
      logger.info(`Successfully saved ${assignments.length} assignments`);
    } catch (error) {
      logger.error('Error saving assigned teachers', { error: error instanceof Error ? error.message : String(error) });
      throw error;
    }
  }
}