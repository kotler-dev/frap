import * as fs from 'fs';
import * as path from 'path';
import type { Reporter, TestCase, TestResult } from '@playwright/test/reporter';
import { FlettaPlaywrightConfig } from './config';
import { generateDebugHtml } from './debug-viewer';

export interface HealingEvent {
  testName: string;
  selector: string;
  healed: boolean;
  confidence: number;
  newSelector?: string;
  timestamp: string;
}

export class FlettaReporter implements Reporter {
  private config: FlettaPlaywrightConfig;
  private healingEvents: HealingEvent[] = [];
  private startTime: number = Date.now();

  constructor(config: FlettaPlaywrightConfig) {
    this.config = config;
  }

  onTestEnd(test: TestCase, result: TestResult): void {
    const testName = test.titlePath().join(' > ');
    
    const annotations = test.annotations.filter(a => a.type.startsWith('fletta.'));
    
    for (const annotation of annotations) {
      if (annotation.type === 'fletta.heal') {
        try {
          const healData = JSON.parse(annotation.description || '{}');
          this.healingEvents.push({
            testName,
            selector: healData.originalSelector || 'unknown',
            healed: true,
            confidence: healData.confidence || 0,
            newSelector: healData.newSelector,
            timestamp: new Date().toISOString(),
          });
        } catch (e) {
          console.error('Failed to parse fletta heal annotation:', e);
        }
      }
    }

    if (result.error && this.config.enableHealing) {
      this.healingEvents.push({
        testName,
        selector: 'unknown',
        healed: false,
        confidence: 0,
        timestamp: new Date().toISOString(),
      });
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

    this.writeJsonReport(reportDir);
    this.writeJUnitReport(reportDir);

    // Generate HTML debug report if debug data exists
    const debugReportPath = path.join(reportDir, 'fletta-debug.json');
    if (fs.existsSync(debugReportPath)) {
      try {
        const debugReport = JSON.parse(fs.readFileSync(debugReportPath, 'utf-8'));
        generateDebugHtml(debugReport, reportDir);
      } catch (e) {
        console.error('[fletta] Failed to generate debug HTML:', e);
      }
    }
  }

  private writeJsonReport(reportDir: string): void {
    const report = {
      timestamp: new Date().toISOString(),
      duration: Date.now() - this.startTime,
      summary: {
        totalHeals: this.healingEvents.filter(e => e.healed).length,
        totalFailures: this.healingEvents.filter(e => !e.healed).length,
        averageConfidence: this.calculateAverageConfidence(),
      },
      events: this.healingEvents,
    };

    const reportPath = path.join(reportDir, 'fletta-report.json');
    fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
    console.log(`[fletta] JSON report written to: ${reportPath}`);
  }

  private writeJUnitReport(reportDir: string): void {
    const testCases = this.healingEvents.map(event => {
      const healingXml = event.healed
        ? `<healing healed="true" confidence="${event.confidence.toFixed(2)}" original="${this.escapeXml(event.selector)}" new="${this.escapeXml(event.newSelector || '')}"/>`
        : '';

      return `    <testcase name="${this.escapeXml(event.testName)}" time="0">
${healingXml}
    </testcase>`;
    }).join('\n');

    const totalTests = this.healingEvents.length;
    const failures = this.healingEvents.filter(e => !e.healed).length;

    const junitXml = `<?xml version="1.0" encoding="UTF-8"?>
<testsuites>
  <testsuite name="fletta" tests="${totalTests}" failures="${failures}" timestamp="${new Date().toISOString()}">
${testCases}
  </testsuite>
</testsuites>`;

    const junitPath = path.join(reportDir, 'junit.xml');
    fs.writeFileSync(junitPath, junitXml);
    console.log(`[fletta] JUnit report written to: ${junitPath}`);
  }

  private calculateAverageConfidence(): number {
    const healed = this.healingEvents.filter(e => e.healed);
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

export function generateJsonReport(events: HealingEvent[], reportDir: string): void {
  if (!fs.existsSync(reportDir)) {
    fs.mkdirSync(reportDir, { recursive: true });
  }

  const report = {
    timestamp: new Date().toISOString(),
    summary: {
      totalHeals: events.filter(e => e.healed).length,
      totalAttempts: events.length,
      averageConfidence: events.filter(e => e.healed).reduce((sum, e) => sum + e.confidence, 0) / 
        Math.max(1, events.filter(e => e.healed).length),
    },
    events,
  };

  const reportPath = path.join(reportDir, 'healing-report.json');
  fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
}

// Default export for Playwright reporter API
export default FlettaReporter;
