import express, { Request, Response } from 'express';
import cors from 'cors';
import { SubstituteManager } from './substitute-manager.js';

const app = express();
const port = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());

// Initialize SubstituteManager
const substituteManager = new SubstituteManager();

// Single endpoint for assigning substitutes
app.post('/api/assign', (req: Request, res: Response) => {
  try {
    const { day, teacherName } = req.body;
    
    if (!day || !teacherName) {
      return res.status(400).json({ 
        error: 'Missing required fields', 
        details: 'Both day and teacherName are required' 
      });
    }

    // Call the substitute manager to handle the assignment
    substituteManager.assignSubstitutes([{
      day,
      teacherName,
      // Default values for other required fields
      period: 1,
      className: 'Default Class',
      substitute: 'Auto Assigned',
      substitutePhone: 'N/A'
    }]);

    res.json({ 
      message: 'Assignment created successfully',
      day,
      teacherName
    });
  } catch (error: any) {
    res.status(500).json({ 
      error: 'Failed to create assignment', 
      details: error.message 
    });
  }
});

// Start server
app.listen(port, () => {
  console.log(`Server running at http://localhost:${port}`);
}); 