import * as fs from 'fs';
import * as path from 'path';
import type { Reporter, TestCase, TestResult } from '@playwright/test/reporter';
import { FlettaPlaywrightConfig } from './config';
import { generateAllDebugHtml } from './debug-viewer';
import {
  clearHealingEventsFile,
  FlettaHealingEvent,
  loadAllHealingEvents,
} from './healing-events';
import { clearDebugReports } from '@fletta/sdk';
import { clearContextBuffers, recordContextUiEvent, writeContextReport } from './context';

export type { FlettaHealingEvent };
/** @deprecated Use FlettaHealingEvent */
export type HealingEvent = FlettaHealingEvent;

export interface FlettaReportSummary {
  totalAttempts: number;
  totalHeals: number;
  expectedHeals: number;
  unexpectedHeals: number;
  rejectedHeals: number;
  averageConfidence: number;
}

export class FlettaReporter implements Reporter {
  private config: FlettaPlaywrightConfig;
  private healingEvents: FlettaHealingEvent[] = [];
  private startTime: number = Date.now();

  constructor(config: FlettaPlaywrightConfig) {
    this.config = config;
  }

  onTestEnd(test: TestCase, result: TestResult): void {
    const testName = test.titlePath().join(' > ');

    // Legacy: Playwright annotations (optional, if tests attach them manually)
    const annotations = test.annotations.filter(a => a.type.startsWith('fletta.'));
    for (const annotation of annotations) {
      if (annotation.type === 'fletta.heal') {
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
          console.error('Failed to parse fletta heal annotation:', e);
        }
      }
    }

    if (this.config.captureAll && result.error) {
      recordContextUiEvent(
        this.config.reportDir,
        testName,
        'failure',
        result.error.message
      );
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

  onEnd(): void {
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

    this.writeJsonReport(reportDir);
    this.writeJUnitReport(reportDir);

    if (this.config.captureAll) {
      const contextPath = writeContextReport(reportDir);
      if (contextPath) {
        console.log(`[fletta] Context report written to: ${contextPath}`);
      }
    }

    try {
      generateAllDebugHtml(reportDir);
    } catch (e) {
      console.error('[fletta] Failed to generate debug HTML:', e);
    }
  }

  private buildSummary(): FlettaReportSummary {
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

  private writeJsonReport(reportDir: string): void {
    const summary = this.buildSummary();
    const report = {
      timestamp: new Date().toISOString(),
      duration: Date.now() - this.startTime,
      summary,
      events: this.healingEvents,
    };

    const reportPath = path.join(reportDir, 'fletta-report.json');
    fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
    console.log(`[fletta] JSON report written to: ${reportPath}`);
    if (summary.unexpectedHeals > 0) {
      console.warn(`[fletta] Report: ${summary.unexpectedHeals} unexpected heal(s) (policy=deny)`);
    }
  }

  private writeJUnitReport(reportDir: string): void {
    const testCases = this.healingEvents.map(event => {
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

    const summary = this.buildSummary();
    const failures = summary.rejectedHeals + summary.unexpectedHeals;

    const junitXml = `<?xml version="1.0" encoding="UTF-8"?>
<testsuites>
  <testsuite name="fletta" tests="${this.healingEvents.length}" failures="${failures}" timestamp="${new Date().toISOString()}">
${testCases}
  </testsuite>
</testsuites>`;

    const junitPath = path.join(reportDir, 'junit.xml');
    fs.writeFileSync(junitPath, junitXml);
    console.log(`[fletta] JUnit report written to: ${junitPath}`);
  }

  private calculateAverageConfidence(healed: FlettaHealingEvent[]): number {
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

export function generateJsonReport(events: FlettaHealingEvent[], reportDir: string): void {
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

export default FlettaReporter;
