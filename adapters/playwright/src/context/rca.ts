import { analyzeRca, formatRcaSummary, type RcaReport } from '@fletta/sdk';
import * as fs from 'fs';
import * as path from 'path';
import { getContextTimeline } from './store';

export async function writeRcaReport(reportDir: string): Promise<string | null> {
  const timeline = getContextTimeline(reportDir);
  if (timeline.events.length === 0) {
    return null;
  }

  let report: RcaReport;
  try {
    report = await analyzeRca(timeline, 0);
  } catch (err) {
    console.error('[fletta] RCA analysis failed:', err);
    return null;
  }

  if (!fs.existsSync(reportDir)) {
    fs.mkdirSync(reportDir, { recursive: true });
  }

  const reportPath = path.join(reportDir, 'fletta-rca.json');
  fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
  console.log(
    `[fletta] RCA report written to: ${reportPath} (${formatRcaSummary(report)})`
  );
  return reportPath;
}

export function loadRcaReport(reportDir: string): RcaReport | null {
  const reportPath = path.join(reportDir, 'fletta-rca.json');
  if (!fs.existsSync(reportPath)) {
    return null;
  }
  return JSON.parse(fs.readFileSync(reportPath, 'utf-8')) as RcaReport;
}

export { formatRcaSummary };
