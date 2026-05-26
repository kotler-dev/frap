#!/usr/bin/env node
/**
 * Post-run verification for RCA reports (F003).
 * Run from scripts/test.sh context after Playwright exits.
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const reportDir = path.resolve(__dirname, '..', 'frap-reports', 'context');

function fail(msg) {
  console.error(`[RCA-VERIFY] ${msg}`);
  process.exit(1);
}

const rcaPath = path.join(reportDir, 'frap-rca.json');
if (!fs.existsSync(rcaPath)) {
  fail(`Missing ${rcaPath}`);
}

const contextPath = path.join(reportDir, 'frap-context.json');
if (!fs.existsSync(contextPath)) {
  fail(`Missing ${contextPath}`);
}

const rca = JSON.parse(fs.readFileSync(rcaPath, 'utf-8'));
const context = JSON.parse(fs.readFileSync(contextPath, 'utf-8'));

// Handle both v1 (legacy) and v2 formats
const isV2 = rca.version === 2;
if (!isV2 && rca.version !== 1) {
  fail(`Expected RCA version 1 or 2, got ${rca.version}`);
}

// For v2, use suite RCA; for v1, use the rca directly
const suiteRca = isV2 ? rca.suite : rca;
const perTestRcas = isV2 ? rca.by_test : [];

const validCauses = ['ui_change', 'api_error', 'infrastructure', 'flaky', 'unknown'];
if (!validCauses.includes(suiteRca.primary_cause)) {
  fail(`Invalid primary_cause: ${suiteRca.primary_cause}`);
}

if (typeof suiteRca.confidence !== 'number' || suiteRca.confidence < 0 || suiteRca.confidence > 1) {
  fail('confidence must be a number between 0 and 1');
}

if (!suiteRca.recommendation || typeof suiteRca.recommendation !== 'string') {
  fail('recommendation must be a non-empty string');
}

if (!Array.isArray(suiteRca.timeline_excerpt)) {
  fail('timeline_excerpt must be an array');
}

const events = context.timeline?.events ?? [];
const payment504 = events.some(
  (e) =>
    e.kind === 'network' &&
    e.request?.url?.includes('payment-intent') &&
    (e.request?.status === 504 || e.request?.phase === 'failed')
);
if (!payment504) {
  fail('C002 evidence missing: expected payment-intent 504/failed in context timeline');
}

const cartResponses = events.filter(
  (e) => e.kind === 'network' && e.request?.url?.includes('/api/cart')
);

function latencyHint(url, durationMs) {
  if (durationMs !== undefined && durationMs !== null) {
    return durationMs;
  }
  const query = url.split('?')[1];
  if (!query) return undefined;
  for (const part of query.split('&')) {
    const [key, value] = part.split('=');
    if (key === 'delay') {
      return Number(value);
    }
  }
  return undefined;
}

const cartLatencies = cartResponses
  .map((e) => latencyHint(e.request.url, e.request.duration_ms))
  .filter((v) => v !== undefined);

if (cartLatencies.length < 2) {
  fail('C003 evidence missing: expected at least two /api/cart latency hints');
}

const spread = Math.max(...cartLatencies) - Math.min(...cartLatencies);
if (spread < 400) {
  fail(`C003 evidence missing: cart latency spread ${spread}ms < 400ms`);
}

// Merged context run: flaky aggregate wins over single-run api_error (C003 AC).
if (suiteRca.primary_cause !== 'flaky') {
  fail(
    `Expected primary_cause flaky for merged C002+C003 run, got ${suiteRca.primary_cause}`
  );
}

if (!suiteRca.details?.pattern?.includes('spread')) {
  fail('Expected flaky pattern describing latency spread in RCA details');
}

// Validate per-test RCA for v2
if (isV2) {
  // Find C002 per-test RCA (should be api_error)
  const c002Rca = perTestRcas.find(entry => 
    entry.playwrightTestId.includes('C002') && 
    entry.playwrightTestId.includes('payment')
  );
  
  if (!c002Rca) {
    fail('Missing per-test RCA for C002 (payment timeout test)');
  }
  
  if (c002Rca.rca.primary_cause !== 'api_error') {
    fail(
      `Expected C002 per-test primary_cause api_error, got ${c002Rca.rca.primary_cause}`
    );
  }
  
  if (!c002Rca.rca.details?.endpoint?.includes('payment-intent')) {
    fail('C002 per-test RCA missing payment-intent endpoint');
  }
  
  // Validate traceId is present
  if (!c002Rca.traceId) {
    fail('C002 per-test RCA missing traceId');
  }
}

const junitPath = path.join(reportDir, 'junit.xml');
if (fs.existsSync(junitPath)) {
  const junit = fs.readFileSync(junitPath, 'utf-8');
  if (!junit.includes('frap-rca') && !junit.includes('frap-context')) {
    fail('JUnit report missing frap-rca or frap-context failure suite');
  }
}

console.log(
  `[RCA-VERIFY] OK — primary_cause=${suiteRca.primary_cause} confidence=${suiteRca.confidence.toFixed(2)}` +
  (isV2 ? `, per-test entries: ${perTestRcas.length}` : '')
);
