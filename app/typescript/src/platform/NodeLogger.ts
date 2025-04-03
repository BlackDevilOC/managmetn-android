/**
 * Node.js implementation of the Logger interface
 */

import { Logger, LogLevel } from './Logger.js';

export class NodeLogger implements Logger {
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