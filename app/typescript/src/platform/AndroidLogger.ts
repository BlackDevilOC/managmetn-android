import { Logger, LogLevel } from './Logger.js';

export class AndroidLogger implements Logger {
  private tag: string;

  constructor(tag: string = 'SubstituteManager') {
    this.tag = tag;
  }

  log(level: LogLevel, message: string, data?: any): void {
    const timestamp = new Date().toISOString();
    const dataString = data ? ` ${JSON.stringify(data)}` : '';
    
    // Use Android's Log API through a bridge
    switch (level) {
      case 'info':
        console.log(`[${this.tag}] [${timestamp}] INFO: ${message}${dataString}`);
        break;
      case 'warning':
        console.warn(`[${this.tag}] [${timestamp}] WARNING: ${message}${dataString}`);
        break;
      case 'error':
        console.error(`[${this.tag}] [${timestamp}] ERROR: ${message}${dataString}`);
        break;
      case 'debug':
        console.debug(`[${this.tag}] [${timestamp}] DEBUG: ${message}${dataString}`);
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