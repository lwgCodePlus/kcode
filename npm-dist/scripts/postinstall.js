#!/usr/bin/env node

/**
 * Postinstall script - Copy skills to user's .kcode directory
 * and display installation info
 */

const path = require('path');
const fs = require('fs');
const os = require('os');
const { execSync } = require('child_process');

const binPath = path.join(__dirname, '..', 'bin', 'kcode.exe');
const sourceSkillsDir = path.join(__dirname, '..', 'skills');

// User's .kcode directory
const userKcodeDir = path.join(os.homedir(), '.kcode');
const targetSkillsDir = path.join(userKcodeDir, 'skills');

/**
 * Recursively copy directory
 */
function copyDirectoryRecursive(src, dest) {
  if (!fs.existsSync(dest)) {
    fs.mkdirSync(dest, { recursive: true });
  }
  
  const entries = fs.readdirSync(src, { withFileTypes: true });
  
  for (const entry of entries) {
    const srcPath = path.join(src, entry.name);
    const destPath = path.join(dest, entry.name);
    
    if (entry.isDirectory()) {
      copyDirectoryRecursive(srcPath, destPath);
    } else {
      fs.copyFileSync(srcPath, destPath);
    }
  }
}

/**
 * Count files in directory
 */
function countFiles(dir) {
  let count = 0;
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    if (entry.isDirectory()) {
      count += countFiles(path.join(dir, entry.name));
    } else {
      count++;
    }
  }
  return count;
}

console.log('\n');
console.log('  ╔════════════════════════════════════════╗');
console.log('  ║       kcode     installed! 🎉          ║');
console.log('  ╚════════════════════════════════════════╝');
console.log('\n');

// Copy skills to user's .kcode directory
if (fs.existsSync(sourceSkillsDir)) {
  try {
    // Ensure .kcode directory exists
    if (!fs.existsSync(userKcodeDir)) {
      fs.mkdirSync(userKcodeDir, { recursive: true });
    }
    
    // Clean existing skills directory if exists
    if (fs.existsSync(targetSkillsDir)) {
      fs.rmSync(targetSkillsDir, { recursive: true, force: true });
    }
    
    // Copy skills
    copyDirectoryRecursive(sourceSkillsDir, targetSkillsDir);
    
    const fileCount = countFiles(targetSkillsDir);
    console.log(`  ✅ Skills installed to: ${targetSkillsDir}`);
    console.log(`     (${fileCount} skill files)\n`);
  } catch (err) {
    console.warn('  ⚠️  Warning: Failed to copy skills directory');
    console.warn(`     ${err.message}\n`);
  }
} else {
  console.warn('  ⚠️  Warning: Skills directory not found in package\n');
}

if (!fs.existsSync(binPath)) {
  console.warn('  ⚠️  Warning: Binary not found. Platform may not be supported.');
  console.warn('     Supported: Windows x64');
  console.log('\n');
}

// Check if ast-grep CLI is available
console.log('  Checking ast-grep CLI...');
try {
  const version = execSync('ast-grep --version', { 
    encoding: 'utf-8', 
    stdio: ['pipe', 'pipe', 'pipe'] 
  }).trim();
  console.log(`  ✅ ast-grep CLI found: ${version}\n`);
} catch (e) {
  console.log('  ⚠️  ast-grep CLI not found in PATH.\n');
  console.log('     AST code search features will not work.');
  console.log('     Install with one of:\n');
  console.log('       npm install -g @ast-grep/cli');
  console.log('       bun install -g @ast-grep/cli');
  console.log('       winget install ast-grep\n');
}