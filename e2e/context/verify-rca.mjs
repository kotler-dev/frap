#!/usr/bin/env node
/**
 * Post-run verification for RCA reports (F003).
 * Run from scripts/test.sh context after Playwright exits.
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const reportDir = path.resolve(__dirname, '..', 'fletta-reports', 'context');

function fail(msg) {
  console.error(`[RCA-VERIFY] ${msg}`);
  process.exit(1);
}

const rcaPath = path.join(reportDir, 'fletta-rca.json');
if (!fs.existsSync(rcaPath)) {
  fail(`Missing ${rcaPath}`);
}

const contextPath = path.join(reportDir, 'fletta-context.json');
if (!fs.existsSync(contextPath)) {
  fail(`Missing ${contextPath}`);
}

const rca = JSON.parse(fs.readFileSync(rcaPath, 'utf-8'));
const context = JSON.parse(fs.readFileSync(contextPath, 'utf-8'));

if (rca.version !== 1) {
  fail(`Expected RCA version 1, got ${rca.version}`);
}

const validCauses = ['ui_change', 'api_error', 'infrastructure', 'flaky', 'unknown'];
if (!validCauses.includes(rca.primary_cause)) {
  fail(`Invalid primary_cause: ${rca.primary_cause}`);
}

if (typeof rca.confidence !== 'number' || rca.confidence < 0 || rca.confidence > 1) {
  fail('confidence must be a number between 0 and 1');
}

if (!rca.recommendation || typeof rca.recommendation !== 'string') {
  fail('recommendation must be a non-empty string');
}

if (!Array.isArray(rca.timeline_excerpt)) {
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
if (rca.primary_cause !== 'flaky') {
  fail(
    `Expected primary_cause flaky for merged C002+C003 run, got ${rca.primary_cause}`
  );
}

if (!rca.details?.pattern?.includes('spread')) {
  fail('Expected flaky pattern describing latency spread in RCA details');
}

const junitPath = path.join(reportDir, 'junit.xml');
if (fs.existsSync(junitPath)) {
  const junit = fs.readFileSync(junitPath, 'utf-8');
  if (!junit.includes('fletta-rca') && !junit.includes('fletta-context')) {
    fail('JUnit report missing fletta-rca or fletta-context failure suite');
  }
}

console.log(
  `[RCA-VERIFY] OK — primary_cause=${rca.primary_cause} confidence=${rca.confidence.toFixed(2)}`
);
