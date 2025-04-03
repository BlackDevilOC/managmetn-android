/**
 * Simple CSV parser that works in both Node.js and Android environments
 */

export interface CSVParserOptions {
  columns?: boolean;
  skip_empty_lines?: boolean;
  trim?: boolean;
  relax_column_count?: boolean;
}

export class CSVParser {
  /**
   * Parse CSV content into an array of objects or arrays
   */
  static parse(content: string, options: CSVParserOptions = {}): any[] {
    const {
      columns = false,
      skip_empty_lines = true,
      trim = true,
      relax_column_count = true
    } = options;

    // Split content into lines
    let lines = content.split(/\r?\n/);
    
    // Skip empty lines if requested
    if (skip_empty_lines) {
      lines = lines.filter(line => line.trim() !== '');
    }
    
    if (lines.length === 0) {
      return [];
    }

    // Process header row if columns mode is enabled
    let headers: string[] = [];
    if (columns) {
      headers = this.parseRow(lines[0], trim);
      lines = lines.slice(1);
    }

    // Parse data rows
    return lines.map(line => {
      const values = this.parseRow(line, trim);
      
      if (columns) {
        // Convert to object using headers
        const result: Record<string, string> = {};
        headers.forEach((header, index) => {
          if (index < values.length || relax_column_count) {
            result[header] = values[index] || '';
          }
        });
        return result;
      } else {
        // Return as array
        return values;
      }
    });
  }

  /**
   * Create a transform stream that parses CSV data
   * This is a placeholder that should be implemented by the platform-specific code
   */
  static parseStream(options: CSVParserOptions = {}): any {
    // This is a placeholder that should be implemented by the platform-specific code
    // For now, we'll return a simple pass-through transform
    return {
      pipe: (stream: any) => stream,
      on: (event: string, callback: Function) => {
        if (event === 'data') {
          // This is a simplified implementation
          // In a real implementation, this would parse the CSV data
          callback({});
        }
        return this;
      }
    };
  }

  /**
   * Parse a single CSV row into an array of values
   */
  private static parseRow(line: string, trim: boolean): string[] {
    const result: string[] = [];
    let currentValue = '';
    let inQuotes = false;
    
    for (let i = 0; i < line.length; i++) {
      const char = line[i];
      
      if (char === '"') {
        if (inQuotes && i + 1 < line.length && line[i + 1] === '"') {
          // Escaped quote
          currentValue += '"';
          i++; // Skip the next quote
        } else {
          // Toggle quote state
          inQuotes = !inQuotes;
        }
      } else if (char === ',' && !inQuotes) {
        // End of field
        result.push(trim ? currentValue.trim() : currentValue);
        currentValue = '';
      } else {
        currentValue += char;
      }
    }
    
    // Add the last field
    result.push(trim ? currentValue.trim() : currentValue);
    
    return result;
  }
} 