import type { ContextEvent, ContextTimeline } from '@frap/sdk';
import { sortTimelineEvents } from '@frap/sdk';
import * as fs from 'fs';
import * as path from 'path';

const traceIds = new Map<string, string>();
const testStarts = new Map<string, number>();

function contextEventsPath(reportDir: string): string {
  return path.join(reportDir, 'frap-context-events.jsonl');
}

function traceIdMapPath(reportDir: string): string {
  return path.join(reportDir, 'frap-context-traceIds.json');
}

export function getContextBufferKey(reportDir: string, testId?: string): string {
  return testId ? `${reportDir}::${testId}` : reportDir;
}

export function setContextTraceId(reportDir: string, testId: string | undefined, traceId: string): void {
  traceIds.set(getContextBufferKey(reportDir, testId), traceId);
  // Persist traceId mapping to disk for reporter access
  persistTraceIdMap(reportDir);
}

export function getContextTraceId(reportDir: string, testId?: string): string | undefined {
  // First try in-memory map
  const key = getContextBufferKey(reportDir, testId);
  const inMemory = traceIds.get(key);
  if (inMemory) {
    return inMemory;
  }
  // Fall back to disk (for reporter process)
  return loadTraceIdMap(reportDir)[testId ?? '__global__'];
}

function persistTraceIdMap(reportDir: string): void {
  try {
    const map = loadTraceIdMap(reportDir);
    // Add all in-memory entries for this reportDir
    for (const [key, traceId] of traceIds.entries()) {
      if (key.startsWith(`${reportDir}::`)) {
        const testId = key.slice(`${reportDir}::`.length);
        map[testId] = traceId;
      } else if (key === reportDir) {
        map['__global__'] = traceId;
      }
    }
    if (!fs.existsSync(reportDir)) {
      fs.mkdirSync(reportDir, { recursive: true });
    }
    fs.writeFileSync(traceIdMapPath(reportDir), JSON.stringify(map, null, 2));
  } catch (e) {
    console.error('[frap] Failed to persist traceId map:', e);
  }
}

function loadTraceIdMap(reportDir: string): Record<string, string> {
  try {
    const path_ = traceIdMapPath(reportDir);
    if (fs.existsSync(path_)) {
      return JSON.parse(fs.readFileSync(path_, 'utf-8'));
    }
  } catch (e) {
    console.error('[frap] Failed to load traceId map:', e);
  }
  return {};
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
    console.error('[frap] Failed to persist context event:', e);
  }
}

export function pushContextEvent(
  reportDir: string,
  event: ContextEvent,
  _testId?: string
): void {
  // Sync append: workers and reporter run in separate processes; in-memory buffers are not shared.
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

export function getContextTimelineForTraceId(reportDir: string, traceId: string): ContextTimeline {
  const allEvents = loadAllContextEvents(reportDir);
  const filtered = allEvents.filter(e => {
    // Check if the event has a trace_id matching the given traceId
    const eventTraceId = (e as unknown as { trace_id?: string }).trace_id;
    return eventTraceId === traceId;
  });
  return { events: sortTimelineEvents(filtered) };
}

export function listTraceIds(reportDir: string): string[] {
  const allEvents = loadAllContextEvents(reportDir);
  const traceIds = new Set<string>();
  for (const event of allEvents) {
    const traceId = (event as unknown as { trace_id?: string }).trace_id;
    if (traceId) {
      traceIds.add(traceId);
    }
  }
  return Array.from(traceIds);
}

export function mergeContextTimelines(reportDir: string): ContextTimeline {
  return getContextTimeline(reportDir);
}

export function clearContextBuffers(reportDir: string): void {
  const filePath = contextEventsPath(reportDir);
  if (fs.existsSync(filePath)) {
    fs.unlinkSync(filePath);
  }
  // Clear persisted traceId map
  const traceIdPath = traceIdMapPath(reportDir);
  if (fs.existsSync(traceIdPath)) {
    fs.unlinkSync(traceIdPath);
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

  const reportPath = path.join(reportDir, 'frap-context.json');
  fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
  return reportPath;
}
