/**
 * Platform abstraction layer for file operations
 * This interface can be implemented differently for Node.js and Android
 */

export interface FileSystem {
  exists(path: string): boolean;
  readFile(path: string, encoding?: string): string;
  writeFile(path: string, data: string, options?: { encoding?: string }): void;
  mkdir(path: string, options?: { recursive?: boolean }): void;
  copyFile(src: string, dest: string): void;
  createReadStream(path: string): any; // This will be implemented differently in each platform
}

// Default implementation that can be used for testing or as a fallback
export class DefaultFileSystem implements FileSystem {
  exists(path: string): boolean {
    console.warn('DefaultFileSystem.exists() called - not implemented');
    return false;
  }

  readFile(path: string, encoding?: string): string {
    console.warn('DefaultFileSystem.readFile() called - not implemented');
    return '';
  }

  writeFile(path: string, data: string, options?: { encoding?: string }): void {
    console.warn('DefaultFileSystem.writeFile() called - not implemented');
  }

  mkdir(path: string, options?: { recursive?: boolean }): void {
    console.warn('DefaultFileSystem.mkdir() called - not implemented');
  }

  copyFile(src: string, dest: string): void {
    console.warn('DefaultFileSystem.copyFile() called - not implemented');
  }

  createReadStream(path: string): any {
    console.warn('DefaultFileSystem.createReadStream() called - not implemented');
    return null;
  }
}

// This will be replaced with the appropriate implementation at runtime
export let fileSystem: FileSystem = new DefaultFileSystem();

// Function to set the file system implementation
export function setFileSystem(fs: FileSystem): void {
  fileSystem = fs;
} 