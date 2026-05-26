#!/usr/bin/env node
/**
 * CONF-RPT-RUN-PASS (post-run): fletta-report.json and junit.xml are written in reporter onEnd,
 * after Playwright exits — run from scripts/test.sh conference.
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const reportDir = path.resolve(__dirname, '..', 'fletta-reports', 'conference');

function fail(msg) {
  console.error(`[CONF-RPT-RUN-PASS] ${msg}`);
  process.exit(1);
}

const summaryPath = path.join(reportDir, 'fletta-report.json');
const junitPath = path.join(reportDir, 'junit.xml');
const eventsPath = path.join(reportDir, 'fletta-events.jsonl');

if (!fs.existsSync(summaryPath)) fail(`Missing ${summaryPath}`);
if (!fs.existsSync(junitPath)) fail(`Missing ${junitPath}`);
if (!fs.existsSync(eventsPath) || fs.statSync(eventsPath).size === 0) {
  fail(`Missing or empty ${eventsPath}`);
}

const summary = JSON.parse(fs.readFileSync(summaryPath, 'utf-8'));
if (!summary.summary?.totalAttempts || summary.summary.totalAttempts <= 0) {
  fail('summary.totalAttempts must be > 0');
}

console.log('[CONF-RPT-RUN-PASS] Report artifacts OK');
