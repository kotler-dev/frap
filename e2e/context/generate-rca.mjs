#!/usr/bin/env node
/**
 * Post-run RCA (F003): WASM via Node native loader (not Playwright's babel).
 * Run after Playwright, before verify-rca.mjs / verify-reports.mjs.
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const reportDir = path.resolve(__dirname, '..', 'frap-reports', 'context');
const sdkRoot = path.resolve(__dirname, '../../sdk/typescript');

function fail(msg) {
  console.error(`[frap-rca] ${msg}`);
  process.exit(1);
}

const contextPath = path.join(reportDir, 'frap-context.json');
if (!fs.existsSync(contextPath)) {
  fail(`Missing ${contextPath}`);
}

const { analyzeRca } = await import(pathToFileURL(path.join(sdkRoot, 'dist/rca.js')).href);
const { formatRcaSummary } = await import(
  pathToFileURL(path.join(sdkRoot, 'dist/rca-summary.js')).href
);

const contextReport = JSON.parse(fs.readFileSync(contextPath, 'utf-8'));
const timeline = contextReport.timeline;
if (!timeline?.events?.length) {
  fail('frap-context.json has no timeline events');
}

function timelineForTraceId(traceId) {
  return {
    events: timeline.events.filter((e) => e.trace_id === traceId),
  };
}

let suiteRca;
try {
  suiteRca = await analyzeRca(timeline, 0);
} catch (err) {
  fail(`RCA analysis failed: ${err}`);
}

const reportPath = path.join(reportDir, 'frap-report.json');
let contextTests = [];
if (fs.existsSync(reportPath)) {
  const frapReport = JSON.parse(fs.readFileSync(reportPath, 'utf-8'));
  contextTests = frapReport.context_tests ?? [];
}

const byTest = [];
for (const test of contextTests) {
  if (test.status === 'passed' || test.status === 'skipped' || !test.traceId) {
    continue;
  }
  const testTimeline = timelineForTraceId(test.traceId);
  if (testTimeline.events.length === 0) {
    continue;
  }
  try {
    const testRca = await analyzeRca(testTimeline, 0);
    byTest.push({
      playwrightTestId: test.playwrightTestId,
      traceId: test.traceId,
      rca: testRca,
    });
  } catch (err) {
    console.error(`[frap-rca] Per-test RCA failed for ${test.playwrightTestId}:`, err);
  }
}

const rcaV2 = {
  version: 2,
  generated_at: new Date().toISOString(),
  suite: suiteRca,
  by_test: byTest,
};

const rcaPath = path.join(reportDir, 'frap-rca.json');
fs.writeFileSync(rcaPath, JSON.stringify(rcaV2, null, 2));
console.log(`[frap] RCA report written to: ${rcaPath} (${formatRcaSummary(suiteRca)})`);

if (fs.existsSync(reportPath)) {
  const frapReport = JSON.parse(fs.readFileSync(reportPath, 'utf-8'));
  frapReport.rca = rcaV2;
  fs.writeFileSync(reportPath, JSON.stringify(frapReport, null, 2));
}

const junitPath = path.join(reportDir, 'junit.xml');
if (fs.existsSync(junitPath) && suiteRca) {
  const summary = formatRcaSummary(suiteRca);
  const escaped = summary
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
  let junit = fs.readFileSync(junitPath, 'utf-8');
  junit = junit.replace(
    /<failure message="[^"]*">[^<]*<\/failure>/g,
    `<failure message="${escaped}">${escaped}</failure>`
  );
  fs.writeFileSync(junitPath, junit);
}
