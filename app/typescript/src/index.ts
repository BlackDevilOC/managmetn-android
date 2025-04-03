/**
 * Main entry point for the substitute management system
 */

import { initializePlatform } from './platform/index.js';
import { initializeApp } from './platform/initializeApp.js';
import { SubstituteManager } from './substitute-manager.js';

// Initialize the platform (Node.js, Android, or browser)
initializePlatform();

// Initialize the app (create directories and files)
initializeApp().catch(error => {
  console.error('Failed to initialize app:', error);
});

// Export the SubstituteManager for use in the app
export { SubstituteManager }; 