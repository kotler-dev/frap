#!/usr/bin/env node
/**
 * Post-run verification for Context Layer reports (CP005-equivalent gate).
 * Run from scripts/test.sh context after Playwright exits.
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const reportDir = path.resolve(__dirname, '..', 'fletta-reports', 'context');

function fail(msg) {
  console.error(`[CTX-RPT-VERIFY] ${msg}`);
  process.exit(1);
}

// Check required files exist
const requiredFiles = [
  'fletta-report.json',
  'fletta-context.json',
  'fletta-rca.json',
  'junit.xml',
];

for (const file of requiredFiles) {
  const filePath = path.join(reportDir, file);
  if (!fs.existsSync(filePath)) {
    fail(`Missing ${filePath}`);
  }
}

// Validate fletta-report.json
const reportPath = path.join(reportDir, 'fletta-report.json');
const report = JSON.parse(fs.readFileSync(reportPath, 'utf-8'));

if (!report.timestamp || typeof report.timestamp !== 'string') {
  fail('fletta-report.json missing timestamp');
}

if (typeof report.duration !== 'number' || report.duration < 0) {
  fail('fletta-report.json missing or invalid duration');
}

if (!report.summary || typeof report.summary !== 'object') {
  fail('fletta-report.json missing summary');
}

// Validate context_summary exists with correct structure
if (!report.context_summary || typeof report.context_summary !== 'object') {
  fail('fletta-report.json missing context_summary');
}

const ctxSummary = report.context_summary;
if (typeof ctxSummary.total !== 'number' || ctxSummary.total !== 4) {
  fail(`Expected context_summary.total === 4, got ${ctxSummary.total}`);
}

if (typeof ctxSummary.passed !== 'number' || ctxSummary.passed !== 2) {
  fail(`Expected context_summary.passed === 2, got ${ctxSummary.passed}`);
}

if (typeof ctxSummary.failed !== 'number' || ctxSummary.failed !== 2) {
  fail(`Expected context_summary.failed === 2, got ${ctxSummary.failed}`);
}

if (typeof ctxSummary.skipped !== 'number') {
  fail('context_summary.skipped must be a number');
}

if (typeof ctxSummary.durationMs !== 'number' || ctxSummary.durationMs < 0) {
  fail('context_summary.durationMs must be a non-negative number');
}

// Validate context_tests array
if (!Array.isArray(report.context_tests) || report.context_tests.length !== 4) {
  fail('fletta-report.json missing context_tests array with 4 entries');
}

const validStatuses = ['passed', 'failed', 'timedOut', 'skipped', 'interrupted'];
for (const test of report.context_tests) {
  if (!test.playwrightTestId || typeof test.playwrightTestId !== 'string') {
    fail('context_test missing playwrightTestId');
  }
  if (!validStatuses.includes(test.status)) {
    fail(`context_test invalid status: ${test.status}`);
  }
  if (typeof test.durationMs !== 'number' || test.durationMs < 0) {
    fail('context_test missing or invalid durationMs');
  }
  if (!test.timestamp || typeof test.timestamp !== 'string') {
    fail('context_test missing timestamp');
  }
}

// Validate rca v2 structure
if (!report.rca || typeof report.rca !== 'object') {
  fail('fletta-report.json missing rca');
}

if (report.rca.version !== 2) {
  fail(`Expected rca.version === 2, got ${report.rca.version}`);
}

if (!report.rca.suite || typeof report.rca.suite !== 'object') {
  fail('rca missing suite');
}

if (!Array.isArray(report.rca.by_test)) {
  fail('rca.by_test must be an array');
}

// Validate junit.xml has fletta-context suite with 4 testcases (overhead runs via bench-context.sh)
const junitPath = path.join(reportDir, 'junit.xml');
const junit = fs.readFileSync(junitPath, 'utf-8');

const suiteMatch = junit.match(/<testsuite name="fletta-context" tests="(\d+)" failures="(\d+)"/);
if (!suiteMatch) {
  fail('junit.xml missing fletta-context testsuite');
}

const suiteTests = parseInt(suiteMatch[1], 10);
const suiteFailures = parseInt(suiteMatch[2], 10);

if (suiteTests !== 4) {
  fail(`Expected fletta-context tests="4", got "${suiteTests}"`);
}

if (suiteFailures !== 2) {
  fail(`Expected fletta-context failures="2", got "${suiteFailures}"`);
}

// Count actual testcase elements in fletta-context suite
const contextSuiteStart = junit.indexOf('name="fletta-context"');
const contextSuiteEnd = junit.indexOf('</testsuite>', contextSuiteStart) + '</testsuite>'.length;
const contextSuite = junit.slice(contextSuiteStart, contextSuiteEnd);
const testCaseMatches = contextSuite.match(/<testcase /g) || [];

if (testCaseMatches.length !== 4) {
  fail(`Expected 4 testcases in fletta-context suite, found ${testCaseMatches.length}`);
}

console.log(
  `[CTX-RPT-VERIFY] OK — context_summary: ${ctxSummary.passed} passed, ${ctxSummary.failed} failed, ${ctxSummary.total} total`
);
