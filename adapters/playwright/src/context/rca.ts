import { analyzeRca, formatRcaSummary, type RcaReport } from '@fletta/sdk';
import * as fs from 'fs';
import * as path from 'path';
import { getContextTimeline, getContextTimelineForTraceId } from './store';
import type { FlettaContextTestResult } from '../reporter';

export interface PerTestRca {
  playwrightTestId: string;
  traceId?: string;
  rca: RcaReport;
}

export interface RcaReportV2 {
  version: 2;
  generated_at: string;
  suite: RcaReport;
  by_test: PerTestRca[];
}

export async function writeRcaReport(
  reportDir: string,
  contextTests?: FlettaContextTestResult[]
): Promise<string | null> {
  const timeline = getContextTimeline(reportDir);
  if (timeline.events.length === 0) {
    return null;
  }

  let suiteRca: RcaReport;
  try {
    suiteRca = await analyzeRca(timeline, 0);
  } catch (err) {
    console.error('[fletta] RCA analysis failed:', err);
    return null;
  }

  const byTestRcas: PerTestRca[] = [];
  
  // Analyze per-test RCA for failed tests
  if (contextTests && contextTests.length > 0) {
    for (const test of contextTests) {
      // Only analyze failed tests that have a traceId
      if (test.status !== 'passed' && test.status !== 'skipped' && test.traceId) {
        const testTimeline = getContextTimelineForTraceId(reportDir, test.traceId);
        if (testTimeline.events.length > 0) {
          try {
            const testRca = await analyzeRca(testTimeline, 0);
            byTestRcas.push({
              playwrightTestId: test.playwrightTestId,
              traceId: test.traceId,
              rca: testRca,
            });
          } catch (err) {
            console.error(`[fletta] RCA analysis failed for test ${test.playwrightTestId}:`, err);
          }
        }
      }
    }
  }

  if (!fs.existsSync(reportDir)) {
    fs.mkdirSync(reportDir, { recursive: true });
  }

  const report: RcaReportV2 = {
    version: 2,
    generated_at: new Date().toISOString(),
    suite: suiteRca,
    by_test: byTestRcas,
  };

  const reportPath = path.join(reportDir, 'fletta-rca.json');
  fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
  console.log(
    `[fletta] RCA report written to: ${reportPath} (${formatRcaSummary(suiteRca)})`
  );
  return reportPath;
}

export function loadRcaReport(reportDir: string): RcaReportV2 | null {
  const reportPath = path.join(reportDir, 'fletta-rca.json');
  if (!fs.existsSync(reportPath)) {
    return null;
  }
  return JSON.parse(fs.readFileSync(reportPath, 'utf-8')) as RcaReportV2;
}

export { formatRcaSummary };
