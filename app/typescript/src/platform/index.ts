/**
 * Platform initialization
 * This file sets up the appropriate implementations based on the environment
 */

import { setFileSystem } from './FileSystem.js';
import { setLogger } from './Logger.js';
import { NodeFileSystem } from './NodeFileSystem.js';
import { NodeLogger } from './NodeLogger.js';
import { AndroidFileSystem } from './AndroidFileSystem.js';
import { AndroidLogger } from './AndroidLogger.js';
import { logger } from './Logger.js';

// Initialize platform-specific implementations
export function initializePlatform(): void {
  // Check if we're running in a browser/Android environment
  const isBrowser = typeof window !== 'undefined' && typeof document !== 'undefined';
  const isAndroid = isBrowser && /android/i.test(navigator.userAgent);
  
  if (isAndroid) {
    // Use Android-specific implementations
    setFileSystem(new AndroidFileSystem());
    setLogger(new AndroidLogger());
    logger.info('Initialized Android platform implementations');
  } else if (typeof process !== 'undefined' && process.versions && process.versions.node) {
    // Use Node.js implementations
    setFileSystem(new NodeFileSystem());
    setLogger(new NodeLogger());
    logger.info('Initialized Node.js platform implementations');
  } else {
    // Default implementations for browser
    logger.info('Using default browser implementations');
  }
} 