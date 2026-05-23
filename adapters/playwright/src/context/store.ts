import type { ContextEvent, ContextTimeline } from '@fletta/sdk';
import { sortTimelineEvents } from '@fletta/sdk';
import * as fs from 'fs';
import * as path from 'path';

const traceIds = new Map<string, string>();
const testStarts = new Map<string, number>();

function contextEventsPath(reportDir: string): string {
  return path.join(reportDir, 'fletta-context-events.jsonl');
}

export function getContextBufferKey(reportDir: string, testId?: string): string {
  return testId ? `${reportDir}::${testId}` : reportDir;
}

export function setContextTraceId(reportDir: string, testId: string | undefined, traceId: string): void {
  traceIds.set(getContextBufferKey(reportDir, testId), traceId);
}

export function getContextTraceId(reportDir: string, testId?: string): string | undefined {
  return traceIds.get(getContextBufferKey(reportDir, testId));
}

export function markContextTestStart(reportDir: string, testId?: string): void {
  testStarts.set(getContextBufferKey(reportDir, testId), Date.now());
}

export function getContextTestStart(reportDir: string, testId?: string): number {
  return testStarts.get(getContextBufferKey(reportDir, testId)) ?? Date.now();
}

function appendEventToDisk(reportDir: string, event: ContextEvent): void {
  try {
    if (!fs.existsSync(reportDir)) {
      fs.mkdirSync(reportDir, { recursive: true });
    }
    fs.appendFileSync(contextEventsPath(reportDir), `${JSON.stringify(event)}\n`, 'utf-8');
  } catch (e) {
    console.error('[fletta] Failed to persist context event:', e);
  }
}

export function pushContextEvent(
  reportDir: string,
  event: ContextEvent,
  _testId?: string
): void {
  appendEventToDisk(reportDir, event);
}

export function loadAllContextEvents(reportDir: string): ContextEvent[] {
  const filePath = contextEventsPath(reportDir);
  if (!fs.existsSync(filePath)) {
    return [];
  }
  const lines = fs.readFileSync(filePath, 'utf-8').trim().split('\n').filter(Boolean);
  return lines.map((line) => JSON.parse(line) as ContextEvent);
}

export function getContextTimeline(reportDir: string): ContextTimeline {
  return { events: sortTimelineEvents(loadAllContextEvents(reportDir)) };
}

export function mergeContextTimelines(reportDir: string): ContextTimeline {
  return getContextTimeline(reportDir);
}

export function clearContextBuffers(reportDir: string): void {
  const filePath = contextEventsPath(reportDir);
  if (fs.existsSync(filePath)) {
    fs.unlinkSync(filePath);
  }
  for (const key of [...traceIds.keys()]) {
    if (key === reportDir || key.startsWith(`${reportDir}::`)) {
      traceIds.delete(key);
    }
  }
  for (const key of [...testStarts.keys()]) {
    if (key === reportDir || key.startsWith(`${reportDir}::`)) {
      testStarts.delete(key);
    }
  }
}

export function writeContextReport(reportDir: string): string | null {
  const timeline = mergeContextTimelines(reportDir);
  if (timeline.events.length === 0) {
    return null;
  }

  if (!fs.existsSync(reportDir)) {
    fs.mkdirSync(reportDir, { recursive: true });
  }

  const report = {
    version: 1,
    generated_at: new Date().toISOString(),
    event_count: timeline.events.length,
    timeline,
  };

  const reportPath = path.join(reportDir, 'fletta-context.json');
  fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
  return reportPath;
}
