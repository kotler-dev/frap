#!/usr/bin/env node
/**
 * Post-run verification for Context Layer E2E (C002–C004).
 * Run from scripts/test.sh context after Playwright exits (reporter writes frap-context.json in onEnd).
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

/** Mirrors frapcode context helpers (avoid ESM subpath resolution in post-run script). */
function eventsByTraceId(timeline, traceId) {
  return timeline.events.filter((e) => e.trace_id === traceId);
}

function getTimelineWindow(timeline, failureAtMs, windowMs) {
  const start = failureAtMs - windowMs;
  const end = failureAtMs + windowMs;
  return timeline.events.filter((e) => e.timestamp_ms >= start && e.timestamp_ms <= end);
}

function networkBeforeUiFailure(timeline, failureAtMs, windowMs = 5000) {
  const window = getTimelineWindow(timeline, failureAtMs, windowMs);
  let lastNetworkFail;
  let uiFailAt;
  for (const event of window) {
    if (event.kind === 'network') {
      const req = event.request;
      if (
        req?.phase === 'failed' ||
        req?.error_text ||
        (req?.status !== undefined && req.status >= 400)
      ) {
        lastNetworkFail = event.timestamp_ms;
      }
    } else if (
      event.kind === 'ui' &&
      (event.action === 'not_found' || event.action === 'failure')
    ) {
      uiFailAt = event.timestamp_ms;
    }
  }
  return lastNetworkFail !== undefined && uiFailAt !== undefined && lastNetworkFail < uiFailAt;
}

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const reportDir = path.resolve(__dirname, '..', 'frap-reports', 'context');

function fail(msg) {
  console.error(`[CTX-VERIFY] ${msg}`);
  process.exit(1);
}

const contextPath = path.join(reportDir, 'frap-context.json');
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

const events = report.timeline.events;
const timeline = { events };

const kinds = new Set(events.map((e) => e.kind));
for (const required of ['network', 'log', 'ui']) {
  if (!kinds.has(required)) {
    fail(`Missing at least one ${required} event in timeline`);
  }
}

const hasHttp = events.some(
  (e) => e.kind === 'network' && (!e.request?.protocol || e.request.protocol === 'http')
);
const hasWs = events.some(
  (e) => e.kind === 'network' && e.request?.protocol === 'websocket'
);
if (!hasHttp) {
  fail('Expected HTTP network events in timeline');
}
if (!hasWs) {
  fail('Expected WebSocket network events in timeline');
}

// C004: WebSocket open + message
const wsEvents = events.filter(
  (e) => e.kind === 'network' && e.request?.protocol === 'websocket'
);
const wsOpen = wsEvents.find((e) => e.request?.phase === 'open');
const wsMessage = wsEvents.find((e) => e.request?.phase === 'message');
if (!wsOpen) {
  fail('C004: expected WebSocket open event in timeline');
}
if (!wsMessage) {
  fail('C004: expected WebSocket message event in timeline');
}
if (!['sent', 'received'].includes(wsMessage.request?.direction)) {
  fail('C004: WebSocket message must have direction sent or received');
}

// C002: payment 504 before UI not_found
const paymentEvents = events.filter(
  (e) => e.kind === 'network' && e.request?.url?.includes('payment-intent')
);
if (paymentEvents.length === 0) {
  fail('C002: expected payment-intent network events');
}
const paymentFail = paymentEvents.find(
  (e) =>
    e.request?.phase === 'failed' ||
    e.request?.status === 504 ||
    (e.request?.status !== undefined && e.request.status >= 500)
);
if (!paymentFail) {
  fail('C002: expected payment-intent failure (504 or failed phase)');
}

const uiFail = events.find((e) => e.kind === 'ui' && e.action === 'not_found');
if (!uiFail) {
  fail('C002: expected UI not_found event');
}

const logError = events.find((e) => e.kind === 'log' && e.log?.level === 'error');
if (!logError) {
  fail('C002: expected console error log event');
}

const traceId = uiFail.trace_id;
if (!traceId) {
  fail('C002: UI failure must have trace_id');
}
const correlated = eventsByTraceId(timeline, traceId);
if (!correlated.some((e) => e.kind === 'network')) {
  fail('C002: correlated timeline must include network events');
}
if (!correlated.some((e) => e.kind === 'log')) {
  fail('C002: correlated timeline must include log events');
}
if (!correlated.some((e) => e.kind === 'ui')) {
  fail('C002: correlated timeline must include ui events');
}

const failureAt = uiFail.timestamp_ms;
if (!networkBeforeUiFailure(timeline, failureAt, 5000)) {
  fail('C002: network failure must precede UI failure within 5s window');
}

const window = getTimelineWindow(timeline, failureAt, 5000);
if (
  !window.some(
    (e) => e.kind === 'network' && e.request?.url?.includes('payment-intent')
  )
) {
  fail('C002: ±5s window must include payment-intent network event');
}
if (!window.some((e) => e.kind === 'ui' && e.action === 'not_found')) {
  fail('C002: ±5s window must include UI not_found event');
}

// C003: fast vs slow /api/cart latency spread (duration_ms or ?delay= hint from URL)
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

const cartNetwork = events.filter(
  (e) => e.kind === 'network' && e.request?.url?.includes('/api/cart')
);
const cartLatencies = cartNetwork
  .map((e) => latencyHint(e.request.url, e.request.duration_ms))
  .filter((v) => v !== undefined);
if (cartLatencies.length < 2) {
  fail('C003: expected at least two /api/cart latency hints');
}
const slow = cartLatencies.some((ms) => ms >= 500);
const fast = cartLatencies.some((ms) => ms < 300);
if (!slow || !fast) {
  fail('C003: expected fast (<300ms) and slow (≥500ms) cart latency hints');
}
if (Math.max(...cartLatencies) - Math.min(...cartLatencies) < 400) {
  fail(
    `C003: cart latency spread must be ≥400ms, got ${Math.max(...cartLatencies) - Math.min(...cartLatencies)}`
  );
}

console.log(
  `[CTX-VERIFY] OK — ${report.event_count ?? events.length} events (C002/C003/C004 + websocket)`
);
