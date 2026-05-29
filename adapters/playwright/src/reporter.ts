import * as fs from 'fs';
import * as path from 'path';
import type { Reporter, TestCase, TestResult } from '@playwright/test/reporter';
import { FrapPlaywrightConfig } from './config';
import { generateAllDebugHtml } from './debug-viewer';
import {
  clearHealingEventsFile,
  FrapHealingEvent,
  loadAllHealingEvents,
} from './healing-events';
import { clearDebugReports } from '@frap/sdk';
import {
  clearContextBuffers,
  recordContextUiEvent,
  writeContextReport,
  loadRcaReport,
  formatRcaSummary,
  getContextTraceId,
} from './context';
import type { RcaReportV2 } from './context';

export type { FrapHealingEvent };
/** @deprecated Use FrapHealingEvent */
export type HealingEvent = FrapHealingEvent;

export interface FrapContextFailure {
  playwrightTestId: string;
  message: string;
  timestamp: string;
  rca?: RcaReportV2;
}

export interface FrapContextTestResult {
  playwrightTestId: string;
  status: 'passed' | 'failed' | 'timedOut' | 'skipped' | 'interrupted';
  durationMs: number;
  message?: string;
  timestamp: string;
  traceId?: string;
  rca?: RcaReportV2;
}

export interface FrapReportSummary {
  totalAttempts: number;
  totalHeals: number;
  expectedHeals: number;
  unexpectedHeals: number;
  rejectedHeals: number;
  averageConfidence: number;
}

export class FrapReporter implements Reporter {
  private config: FrapPlaywrightConfig;
  private healingEvents: FrapHealingEvent[] = [];
  private contextFailures: FrapContextFailure[] = [];
  private contextTests: FrapContextTestResult[] = [];
  private startTime: number = Date.now();

  constructor(config: FrapPlaywrightConfig) {
    this.config = config;
  }

  onTestEnd(test: TestCase, result: TestResult): void {
    const testName = test.titlePath().join(' > ');

    // Legacy: Playwright annotations (optional, if tests attach them manually)
    const annotations = test.annotations.filter(a => a.type.startsWith('frap.'));
    for (const annotation of annotations) {
      if (annotation.type === 'frap.heal') {
        try {
          const healData = JSON.parse(annotation.description || '{}');
          this.healingEvents.push({
            playwrightTestId: testName,
            originalSelector: healData.originalSelector || 'unknown',
            newSelector: healData.newSelector,
            healed: true,
            confidence: healData.confidence || 0,
            trigger: healData.trigger || 'selector_missing',
            policy: healData.policy || 'allow',
            outcome: healData.outcome || 'healed',
            timestamp: new Date().toISOString(),
          });
        } catch (e) {
          console.error('Failed to parse frap heal annotation:', e);
        }
      }
    }

    if (this.config.captureAll) {
      // Use test.title (just the test title) for traceId lookup compatibility
      // Tests pass testInfo.title to attachFrapContext, not the full titlePath
      const traceIdKey = test.title;
      const traceId = getContextTraceId(this.config.reportDir, traceIdKey);

      const testResult: FrapContextTestResult = {
        playwrightTestId: testName,
        status: result.status,
        durationMs: result.duration,
        timestamp: new Date().toISOString(),
        traceId: traceId ?? undefined,
      };

      if (result.error) {
        const errorMessage = result.error.message ?? 'unknown error';
        testResult.message = errorMessage;

        recordContextUiEvent(
          this.config.reportDir,
          testName,
          'failure',
          errorMessage,
          test.title
        );

        this.contextFailures.push({
          playwrightTestId: testName,
          message: errorMessage,
          timestamp: testResult.timestamp,
        });
      }

      this.contextTests.push(testResult);
    }

    if (result.error && this.config.enableHealing) {
      this.healingEvents.push({
        playwrightTestId: testName,
        originalSelector: 'unknown',
        healed: false,
        confidence: 0,
        trigger: 'selector_missing',
        policy: 'allow',
        outcome: 'rejected',
        timestamp: new Date().toISOString(),
      });
    }
  }

  onBegin(): void {
    if (this.config.enableReporting && this.config.reportDir) {
      clearHealingEventsFile(this.config.reportDir);
      clearDebugReports(this.config.reportDir);
      if (this.config.captureAll) {
        clearContextBuffers(this.config.reportDir);
      }
    }
  }

  async onEnd(): Promise<void> {
    if (!this.config.enableReporting) {
      return;
    }

    const reportDir = this.config.reportDir;
    if (!fs.existsSync(reportDir)) {
      fs.mkdirSync(reportDir, { recursive: true });
    }

    const diskEvents = loadAllHealingEvents(reportDir);
    if (diskEvents.length > 0) {
      this.healingEvents = diskEvents;
    }

    if (this.config.captureAll) {
      const contextPath = writeContextReport(reportDir);
      if (contextPath) {
        console.log(`[frap] Context report written to: ${contextPath}`);
      }
      // RCA (WASM) runs in e2e/context/generate-rca.mjs after Playwright — reporter loader cannot load .wasm.
    }

    this.writeJsonReport(reportDir);
    this.writeJUnitReport(reportDir);

    try {
      generateAllDebugHtml(reportDir);
    } catch (e) {
      console.error('[frap] Failed to generate debug HTML:', e);
    }
  }

  private buildSummary(): FrapReportSummary {
    const healedEvents = this.healingEvents.filter(e => e.outcome === 'healed');
    const unexpected = this.healingEvents.filter(e => e.outcome === 'unexpected_heal');
    const rejected = this.healingEvents.filter(e => e.outcome === 'rejected');
    const expectedHeals = healedEvents.filter(e => e.policy === 'expect_heal');

    return {
      totalAttempts: this.healingEvents.length,
      totalHeals: healedEvents.length,
      expectedHeals: expectedHeals.length,
      unexpectedHeals: unexpected.length,
      rejectedHeals: rejected.length,
      averageConfidence: this.calculateAverageConfidence(healedEvents),
    };
  }

  private buildContextSummary() {
    const total = this.contextTests.length;
    const passed = this.contextTests.filter(t => t.status === 'passed').length;
    const failed = this.contextTests.filter(t => t.status === 'failed' || t.status === 'timedOut').length;
    const skipped = this.contextTests.filter(t => t.status === 'skipped').length;

    return {
      total,
      passed,
      failed,
      skipped,
      durationMs: this.contextTests.reduce((sum, t) => sum + t.durationMs, 0),
    };
  }

  private writeJsonReport(reportDir: string): void {
    const summary = this.buildSummary();
    const report: Record<string, unknown> = {
      timestamp: new Date().toISOString(),
      duration: Date.now() - this.startTime,
      summary,
      events: this.healingEvents,
    };

    if (this.config.captureAll && this.contextTests.length > 0) {
      const contextSummary = this.buildContextSummary();
      report.context_summary = contextSummary;
      report.context_tests = this.contextTests;

      const rcaV2 = loadRcaReport(reportDir);
      if (rcaV2) {
        report.rca = rcaV2;
      }
    }

    const reportPath = path.join(reportDir, 'frap-report.json');
    fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
    console.log(`[frap] JSON report written to: ${reportPath}`);
    if (summary.unexpectedHeals > 0) {
      console.warn(`[frap] Report: ${summary.unexpectedHeals} unexpected heal(s) (policy=deny)`);
    }
  }

  private writeJUnitReport(reportDir: string): void {
    const summary = this.buildSummary();
    const healingFailures = summary.rejectedHeals + summary.unexpectedHeals;

    // Build frap healing suite (only if there are healing events)
    let frapSuite = '';
    if (this.healingEvents.length > 0) {
      const healingCases = this.healingEvents.map(event => {
        const attrs = [
          `healed="${event.healed}"`,
          `confidence="${event.confidence.toFixed(2)}"`,
          `trigger="${this.escapeXml(event.trigger)}"`,
          `policy="${this.escapeXml(event.policy)}"`,
          `outcome="${this.escapeXml(event.outcome)}"`,
          `original="${this.escapeXml(event.originalSelector)}"`,
          `new="${this.escapeXml(event.newSelector || '')}"`,
        ].join(' ');
        const healingXml = `      <healing ${attrs}/>`;

        return `    <testcase name="${this.escapeXml(event.playwrightTestId)}" time="0">
${healingXml}
    </testcase>`;
      }).join('\n');

      frapSuite = `
  <testsuite name="frap" tests="${this.healingEvents.length}" failures="${healingFailures}" timestamp="${new Date().toISOString()}">
${healingCases}
  </testsuite>`;
    }

    // Build frap-context suite with all tests (if captureAll enabled)
    let contextSuite = '';
    if (this.config.captureAll && this.contextTests.length > 0) {
      const contextSummary = this.buildContextSummary();
      const rcaV2 = loadRcaReport(reportDir);
      const suiteRca = rcaV2?.suite;

      const contextCases = this.contextTests.map(test => {
        const isFailed = test.status === 'failed' || test.status === 'timedOut';
        let failureXml = '';

        if (isFailed && test.message) {
          const failureBody = suiteRca
            ? this.escapeXml(formatRcaSummary(suiteRca))
            : this.escapeXml(test.message);
          failureXml = `
      <failure message="${failureBody}">${failureBody}</failure>`;
        }

        return `    <testcase name="${this.escapeXml(test.playwrightTestId)}" time="${(test.durationMs / 1000).toFixed(3)}">${failureXml}
    </testcase>`;
      }).join('\n');

      contextSuite = `
  <testsuite name="frap-context" tests="${contextSummary.total}" failures="${contextSummary.failed}" timestamp="${new Date().toISOString()}">
${contextCases}
  </testsuite>`;
    }

    const junitXml = `<?xml version="1.0" encoding="UTF-8"?>
<testsuites>${frapSuite}${contextSuite}
</testsuites>`;

    const junitPath = path.join(reportDir, 'junit.xml');
    fs.writeFileSync(junitPath, junitXml);
    console.log(`[frap] JUnit report written to: ${junitPath}`);
  }

  private calculateAverageConfidence(healed: FrapHealingEvent[]): number {
    if (healed.length === 0) return 0;
    return healed.reduce((sum, e) => sum + e.confidence, 0) / healed.length;
  }

  private escapeXml(str: string): string {
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&apos;');
  }
}

export function generateJsonReport(events: FrapHealingEvent[], reportDir: string): void {
  if (!fs.existsSync(reportDir)) {
    fs.mkdirSync(reportDir, { recursive: true });
  }

  const healed = events.filter(e => e.outcome === 'healed');
  const report = {
    timestamp: new Date().toISOString(),
    summary: {
      totalAttempts: events.length,
      totalHeals: healed.length,
      unexpectedHeals: events.filter(e => e.outcome === 'unexpected_heal').length,
      rejectedHeals: events.filter(e => e.outcome === 'rejected').length,
      averageConfidence:
        healed.reduce((sum, e) => sum + e.confidence, 0) / Math.max(1, healed.length),
    },
    events,
  };

  const reportPath = path.join(reportDir, 'healing-report.json');
  fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
}

export default FrapReporter;
