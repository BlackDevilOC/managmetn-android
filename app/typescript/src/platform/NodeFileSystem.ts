/**
 * Node.js implementation of the FileSystem interface
 */

import * as fs from 'fs';
import { FileSystem } from './FileSystem.js';

export class NodeFileSystem implements FileSystem {
  exists(path: string): boolean {
    return fs.existsSync(path);
  }

  readFile(path: string, encoding?: string): string {
    const buffer = fs.readFileSync(path);
    return encoding ? buffer.toString(encoding as any) : buffer.toString('utf-8');
  }

  writeFile(path: string, data: string, options?: { encoding?: string }): void {
    fs.writeFileSync(path, data, options as any);
  }

  mkdir(path: string, options?: { recursive?: boolean }): void {
    fs.mkdirSync(path, options as any);
  }

  copyFile(src: string, dest: string): void {
    fs.copyFileSync(src, dest);
  }

  createReadStream(path: string): any {
    return fs.createReadStream(path);
  }
} 