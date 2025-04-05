import { SubstituteServer } from './SubstituteServer.js';
import { logger } from '../platform/Logger.js';

/**
 * Run the server
 */
async function runServer() {
  try {
    // Create a new server instance
    const server = new SubstituteServer(3000);
    
    // Start the server
    await server.start();
    
    // Log that the server is running
    logger.info('Server is running on port 3000');
    
    // Handle process exit
    process.on('SIGINT', () => {
      logger.info('Shutting down server...');
      server.stop();
      process.exit(0);
    });
    
  } catch (error) {
    logger.error('Error running server', { error: error instanceof Error ? error.message : String(error) });
    process.exit(1);
  }
}

// Run the server
runServer(); 