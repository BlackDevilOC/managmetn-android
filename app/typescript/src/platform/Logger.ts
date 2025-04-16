/**
 * Platform abstraction layer for logging
 * This interface can be implemented differently for Node.js and Android
 */

export type LogLevel = 'info' | 'warning' | 'error' | 'debug';

export interface Logger {
  log(level: LogLevel, message: string, data?: any): void;
  info(message: string, data?: any): void;
  warn(message: string, data?: any): void;
  error(message: string, data?: any): void;
  debug(message: string, data?: any): void;
}

// Default implementation that uses console
export class DefaultLogger implements Logger {
  log(level: LogLevel, message: string, data?: any): void {
    const timestamp = new Date().toISOString();
    const dataString = data ? ` ${JSON.stringify(data)}` : '';
    
    switch (level) {
      case 'info':
        console.log(`[${timestamp}] INFO: ${message}${dataString}`);
        break;
      case 'warning':
        console.warn(`[${timestamp}] WARNING: ${message}${dataString}`);
        break;
      case 'error':
        console.error(`[${timestamp}] ERROR: ${message}${dataString}`);
        break;
      case 'debug':
        console.debug(`[${timestamp}] DEBUG: ${message}${dataString}`);
        break;
    }
  }

  info(message: string, data?: any): void {
    this.log('info', message, data);
  }

  warn(message: string, data?: any): void {
    this.log('warning', message, data);
  }

  error(message: string, data?: any): void {
    this.log('error', message, data);
  }

  debug(message: string, data?: any): void {
    this.log('debug', message, data);
  }
}

// This will be replaced with the appropriate implementation at runtime
export let logger: Logger = new DefaultLogger();

// Function to set the logger implementation
export function setLogger(log: Logger): void {
  logger = log;
} 