#!/usr/bin/env node

/**
 * Prepack script - Copy native binary and skills from parent project
 * This runs before npm pack/publish
 */

const fs = require('fs');
const path = require('path');

const npmDistDir = path.join(__dirname, '..');
const projectRoot = path.join(npmDistDir, '..');
const binDir = path.join(npmDistDir, 'bin');
const sourceExe = path.join(projectRoot, 'target', 'kcode.exe');
const targetExe = path.join(binDir, 'kcode.exe');

// Skills are already in npm-dist/skills directory, no need to copy

// Ensure bin directory exists
if (!fs.existsSync(binDir)) {
  fs.mkdirSync(binDir, { recursive: true });
}

// Check if source exe exists
if (!fs.existsSync(sourceExe)) {
  console.error('Error: Native binary not found at ../target/kcode.exe');
  console.error('Please run "mvn package -Pnative" first to build the native image.');
  process.exit(1);
}

// Copy the binary
fs.copyFileSync(sourceExe, targetExe);
console.log(`✅ Copied native binary to bin/kcode.exe`);

// Get file size
const stats = fs.statSync(targetExe);
const sizeMB = (stats.size / (1024 * 1024)).toFixed(2);
console.log(`📦 Binary size: ${sizeMB} MB`);