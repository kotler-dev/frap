#!/usr/bin/env node
/**
 * Post-run verification for Context Layer E2E (C002–C004).
 * Run from scripts/test.sh context after Playwright exits.
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const reportDir = path.resolve(__dirname, '..', 'fletta-reports', 'context');

function fail(msg) {
  console.error(`[CTX-VERIFY] ${msg}`);
  process.exit(1);
}

const contextPath = path.join(reportDir, 'fletta-context.json');
if (!fs.existsSync(contextPath)) {
  fail(`Missing ${contextPath}`);
}

const report = JSON.parse(fs.readFileSync(contextPath, 'utf-8'));
if (report.version !== 1) {
  fail(`Expected version 1, got ${report.version}`);
}
if (!report.timeline?.events || !Array.isArray(report.timeline.events)) {
  fail('timeline.events must be an array');
}
if (report.timeline.events.length === 0) {
  fail('timeline.events must not be empty');
}

const kinds = new Set(report.timeline.events.map((e) => e.kind));
for (const required of ['network', 'log', 'ui']) {
  if (!kinds.has(required)) {
    fail(`Missing at least one ${required} event in timeline`);
  }
}

const hasHttp = report.timeline.events.some(
  (e) => e.kind === 'network' && (!e.request?.protocol || e.request.protocol === 'http')
);
const hasWs = report.timeline.events.some(
  (e) => e.kind === 'network' && e.request?.protocol === 'websocket'
);
if (!hasHttp) {
  fail('Expected HTTP network events in timeline');
}
if (!hasWs) {
  fail('Expected WebSocket network events in timeline');
}

console.log(
  `[CTX-VERIFY] OK — ${report.event_count ?? report.timeline.events.length} events (network/log/ui + websocket)`
);
