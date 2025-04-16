import { FileSystem } from './FileSystem.js';
import { Platform } from '@capacitor/core';
import { Filesystem, Directory } from '@capacitor/filesystem';

export class AndroidFileSystem implements FileSystem {
  private basePath: string;

  constructor(basePath: string = '/storage/emulated/0/Android/data/com.substituemanagment.managment/files/substitute_data') {
    this.basePath = basePath;
  }

  exists(path: string): boolean {
    try {
      // Check if the file exists using Capacitor Filesystem API
      const result = Filesystem.stat({
        path: this.getRelativePath(path),
        directory: Directory.External
      });
      return !!result;
    } catch (error) {
      return false;
    }
  }

  readFile(path: string, encoding?: string): string {
    try {
      // Read the file using Capacitor Filesystem API
      const result = Filesystem.readFile({
        path: this.getRelativePath(path),
        directory: Directory.External
      });
      return result.data;
    } catch (error) {
      throw new Error(`Error reading file ${path}: ${error}`);
    }
  }

  writeFile(path: string, data: string, options?: { encoding?: string }): void {
    try {
      // Ensure the directory exists
      this.ensureDirectoryExists(this.getDirectoryPath(path));
      
      // Write the file using Capacitor Filesystem API
      Filesystem.writeFile({
        path: this.getRelativePath(path),
        data: data,
        directory: Directory.External
      });
    } catch (error) {
      throw new Error(`Error writing file ${path}: ${error}`);
    }
  }

  mkdir(path: string, options?: { recursive?: boolean }): void {
    try {
      // Create the directory using Capacitor Filesystem API
      Filesystem.mkdir({
        path: this.getRelativePath(path),
        directory: Directory.External,
        recursive: options?.recursive || false
      });
    } catch (error) {
      throw new Error(`Error creating directory ${path}: ${error}`);
    }
  }

  copyFile(src: string, dest: string): void {
    try {
      // Read the source file
      const data = this.readFile(src);
      
      // Write to the destination file
      this.writeFile(dest, data);
    } catch (error) {
      throw new Error(`Error copying file from ${src} to ${dest}: ${error}`);
    }
  }

  createReadStream(path: string): any {
    // Create a simple stream-like object for Android
    return {
      pipe: (stream: any) => stream,
      on: (event: string, callback: Function) => {
        if (event === 'data') {
          // Read the file and call the callback with the data
          try {
            const data = this.readFile(path);
            callback(data);
          } catch (error) {
            console.error(`Error reading file ${path}: ${error}`);
          }
        }
        return this;
      }
    };
  }

  private getRelativePath(fullPath: string): string {
    // Convert the full path to a relative path for Capacitor
    if (fullPath.startsWith(this.basePath)) {
      return fullPath.substring(this.basePath.length + 1);
    }
    return fullPath;
  }

  private getDirectoryPath(filePath: string): string {
    // Get the directory path from a file path
    const lastSlashIndex = filePath.lastIndexOf('/');
    if (lastSlashIndex === -1) {
      return '';
    }
    return filePath.substring(0, lastSlashIndex);
  }

  private ensureDirectoryExists(dirPath: string): void {
    if (dirPath && !this.exists(dirPath)) {
      this.mkdir(dirPath, { recursive: true });
    }
  }
} 