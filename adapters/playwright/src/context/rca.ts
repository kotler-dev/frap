import type { RcaReport } from '@frap/sdk';
import { formatRcaSummary } from '@frap/sdk';
import * as fs from 'fs';
import * as path from 'path';

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

export function loadRcaReport(reportDir: string): RcaReportV2 | null {
  const reportPath = path.join(reportDir, 'frap-rca.json');
  if (!fs.existsSync(reportPath)) {
    return null;
  }
  return JSON.parse(fs.readFileSync(reportPath, 'utf-8')) as RcaReportV2;
}

export { formatRcaSummary };
