import * as fs from 'fs';
import * as path from 'path';
import { debugReportJsonPath, writeDebugReport, type DebugReport } from '@frap/sdk';
import {
  classifyHealOutcome,
  type HealPolicy,
  type HealTrigger,
  type HealingSemantics,
  type HealOutcome,
} from '@frap/sdk';

export interface FrapHealingEvent {
  playwrightTestId: string;
  healSessionName?: string;
  originalSelector: string;
  newSelector?: string;
  healed: boolean;
  confidence: number;
  trigger: HealTrigger;
  policy: HealPolicy;
  outcome: HealOutcome;
  timestamp: string;
}

const eventsByTest = new Map<string, FrapHealingEvent[]>();

function resolvePlaywrightTestId(testInfo?: { titlePath: string[] }, healSessionName?: string): string {
  if (testInfo?.titlePath?.length) {
    return testInfo.titlePath.join(' > ');
  }
  try {
    const { test } = require('@playwright/test') as typeof import('@playwright/test');
    const info = test.info();
    if (info?.titlePath?.length) {
      return info.titlePath.join(' > ');
    }
  } catch {
    // reporter / non-test context
  }
  return healSessionName || 'unknown';
}

/** @deprecated Reporter runs in a separate process; test id is resolved via test.info() in workers */
export function setCurrentPlaywrightTestId(_testId: string): void {
  // no-op: kept for API compatibility
}

export function getCurrentPlaywrightTestId(
  testInfo?: { titlePath: string[] },
  healSessionName?: string
): string {
  return resolvePlaywrightTestId(testInfo, healSessionName);
}

function eventsFilePath(reportDir: string): string {
  return path.join(reportDir, 'frap-events.jsonl');
}

function appendEventToDisk(reportDir: string, event: FrapHealingEvent): void {
  try {
    if (!fs.existsSync(reportDir)) {
      fs.mkdirSync(reportDir, { recursive: true });
    }
    fs.appendFileSync(eventsFilePath(reportDir), `${JSON.stringify(event)}\n`, 'utf-8');
  } catch (e) {
    console.error('[frap] Failed to persist healing event:', e);
  }
}

export function recordHealingEvent(
  event: Omit<FrapHealingEvent, 'playwrightTestId' | 'timestamp'>,
  reportDir?: string,
  testInfo?: { titlePath: string[] }
): FrapHealingEvent {
  const full: FrapHealingEvent = {
    ...event,
    playwrightTestId: resolvePlaywrightTestId(testInfo, event.healSessionName),
    timestamp: new Date().toISOString(),
  };

  const key = full.playwrightTestId;
  if (!eventsByTest.has(key)) {
    eventsByTest.set(key, []);
  }
  eventsByTest.get(key)!.push(full);

  if (reportDir) {
    appendEventToDisk(reportDir, full);
  }

  return full;
}

export function loadAllHealingEvents(reportDir: string): FrapHealingEvent[] {
  const filePath = eventsFilePath(reportDir);
  if (!fs.existsSync(filePath)) {
    return [];
  }

  const lines = fs.readFileSync(filePath, 'utf-8').trim().split('\n').filter(Boolean);
  return lines.map(line => JSON.parse(line) as FrapHealingEvent);
}

export function clearHealingEventsFile(reportDir: string): void {
  const filePath = eventsFilePath(reportDir);
  if (fs.existsSync(filePath)) {
    fs.unlinkSync(filePath);
  }
}

export function consumeHealingEvents(playwrightTestId: string): FrapHealingEvent[] {
  const events = eventsByTest.get(playwrightTestId) ?? [];
  eventsByTest.delete(playwrightTestId);
  return events;
}

export function buildSemantics(
  policy: HealPolicy,
  healed: boolean,
  healingAttempted: boolean,
  trigger: HealTrigger
): HealingSemantics {
  return {
    trigger,
    policy,
    outcome: classifyHealOutcome(policy, healed, healingAttempted),
  };
}

export function enrichDebugReport(
  reportDir: string,
  semantics: HealingSemantics,
  testName: string
): void {
  const reportPath = debugReportJsonPath(reportDir, testName);
  if (!fs.existsSync(reportPath)) {
    const legacyPath = path.join(reportDir, 'frap-debug.json');
    if (!fs.existsSync(legacyPath)) {
      return;
    }
    try {
      const report = JSON.parse(fs.readFileSync(legacyPath, 'utf-8')) as DebugReport;
      report.semantics = semantics;
      writeDebugReport(reportDir, report);
    } catch (e) {
      console.error('[frap] Failed to enrich debug report with semantics:', e);
    }
    return;
  }

  try {
    const report = JSON.parse(fs.readFileSync(reportPath, 'utf-8')) as DebugReport;
    report.semantics = semantics;
    writeDebugReport(reportDir, report);
  } catch (e) {
    console.error('[frap] Failed to enrich debug report with semantics:', e);
  }
}

export function logHealSemantics(semantics: HealingSemantics, originalSelector: string, newSelector?: string): void {
  if (semantics.outcome === 'unexpected_heal') {
    console.warn(
      `[frap] Unexpected heal (policy=${semantics.policy}, trigger=${semantics.trigger}): ` +
        `"${originalSelector}" → "${newSelector ?? 'n/a'}"`
    );
    return;
  }
  if (semantics.outcome === 'rejected') {
    console.log(
      `[frap] Healing rejected (policy=${semantics.policy}, trigger=${semantics.trigger}): "${originalSelector}"`
    );
    return;
  }
  if (semantics.outcome === 'healed') {
    console.log(
      `[frap] Healing event (policy=${semantics.policy}, trigger=${semantics.trigger}, outcome=healed)`
    );
  }
}
