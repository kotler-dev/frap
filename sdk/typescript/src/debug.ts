import * as crypto from 'crypto';
import * as fs from 'fs';
import * as path from 'path';

export type DebugStepName = 'dom_parsed' | 'clusters_built' | 'candidates_ranked' | 'healing_decision';

export interface DebugStep {
  name: DebugStepName;
  timestamp: string;
  duration_ms: number;
  input?: unknown;
  output?: unknown;
}

export interface ClusterView {
  id: string;
  prefix: string;
  element_count: number;
  elements: Array<{
    selector: string;
    signature_preview: string;
    text_content?: string;
  }>;
}

export interface HealingDebugInfo {
  healed: boolean;
  selector: string;
  confidence: number;
  top_candidates: Array<{
    selector: string;
    confidence: number;
  }>;
}

import type { HealingSemantics } from './healing-semantics';

export interface DebugReport {
  timestamp: string;
  testName: string;
  duration_ms: number;
  steps: DebugStep[];
  clusters: ClusterView[];
  healing: HealingDebugInfo;
  /** Set by Playwright adapter after heal attempt (policy / trigger / outcome) */
  semantics?: HealingSemantics;
}

export function debugReportsDir(reportDir: string): string {
  return path.join(reportDir, 'debug-reports');
}

export function debugReportSlug(testName: string): string {
  const base = testName
    .replace(/[^a-zA-Z0-9\u0400-\u04FF]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 60);
  const hash = crypto.createHash('sha256').update(testName).digest('hex').slice(0, 8);
  return `${base || 'test'}-${hash}`;
}

export function debugReportJsonPath(reportDir: string, testName: string): string {
  return path.join(debugReportsDir(reportDir), `${debugReportSlug(testName)}.json`);
}

export function writeDebugReport(reportDir: string, report: DebugReport): void {
  const dir = debugReportsDir(reportDir);
  if (!fs.existsSync(reportDir)) {
    fs.mkdirSync(reportDir, { recursive: true });
  }
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }

  const content = JSON.stringify(report, null, 2);
  const perTestPath = path.join(dir, `${debugReportSlug(report.testName)}.json`);
  fs.writeFileSync(perTestPath, content);
  fs.writeFileSync(path.join(reportDir, 'fletta-debug.json'), content);
}

export function clearDebugReports(reportDir: string): void {
  const dir = debugReportsDir(reportDir);
  if (fs.existsSync(dir)) {
    fs.rmSync(dir, { recursive: true, force: true });
  }
  const legacyJson = path.join(reportDir, 'fletta-debug.json');
  const legacyHtml = path.join(reportDir, 'fletta-debug.html');
  const explorerHtml = path.join(reportDir, 'fletta-debug-explorer.html');
  if (fs.existsSync(legacyJson)) fs.unlinkSync(legacyJson);
  if (fs.existsSync(legacyHtml)) fs.unlinkSync(legacyHtml);
  if (fs.existsSync(explorerHtml)) fs.unlinkSync(explorerHtml);
}

export function buildElementFoundDebugReport(testName: string, selector: string): DebugReport {
  const now = new Date().toISOString();
  return {
    timestamp: now,
    testName,
    duration_ms: 0,
    steps: [
      {
        name: 'dom_parsed',
        timestamp: now,
        duration_ms: 0,
        output: { found: true, selector },
      },
      {
        name: 'healing_decision',
        timestamp: now,
        duration_ms: 0,
        output: { healed: false, reason: 'element_found', healing_skipped: true },
      },
    ],
    clusters: [],
    healing: {
      healed: false,
      selector,
      confidence: 1.0,
      top_candidates: [],
    },
  };
}

export class DebugTracer {
  private steps: DebugStep[] = [];
  private stepStartTimes: Map<string, number> = new Map();
  private startTime: number;

  constructor(private testName: string) {
    this.startTime = Date.now();
  }

  step(name: DebugStepName, input?: unknown): void {
    this.stepStartTimes.set(name, Date.now());
    this.steps.push({
      name,
      timestamp: new Date().toISOString(),
      duration_ms: 0,
      input,
    });
  }

  endStep(name: DebugStepName, output?: unknown): void {
    const startTime = this.stepStartTimes.get(name);
    const duration = startTime ? Date.now() - startTime : 0;

    const step = this.steps.find(s => s.name === name && s.duration_ms === 0);
    if (step) {
      step.duration_ms = duration;
      step.output = output;
    }
  }

  buildReport(clusters: ClusterView[], healing: HealingDebugInfo): DebugReport {
    return {
      timestamp: new Date().toISOString(),
      testName: this.testName,
      duration_ms: Date.now() - this.startTime,
      steps: this.steps,
      clusters,
      healing,
    };
  }
}
