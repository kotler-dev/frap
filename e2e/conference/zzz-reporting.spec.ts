import * as fs from 'fs';
import * as path from 'path';
import { test, expect } from '@playwright/test';
import { CONF_REPORT_DIR } from './helpers';

test.describe('Conference 2026 Spring', () => {
  test.describe('Reporting', () => {
    test('CONF-RPT-RUN-PASS: frap report artifacts exist after suite', async () => {
      test.info().annotations.push({
        type: 'note',
        description: 'Must run last: workers=1, fullyParallel=false, file zzz-*',
      });
      const reportDir = path.resolve(__dirname, '..', CONF_REPORT_DIR);
      const events = path.join(reportDir, 'frap-events.jsonl');
      expect(fs.existsSync(events)).toBe(true);
      expect(fs.statSync(events).size).toBeGreaterThan(0);

      // frap-report.json + junit.xml: scripts/test.sh runs conference/verify-reports.mjs after Playwright
    });

    test('CONF-RPT-RUN-FAIL: fail-only run records rejected heals', async () => {
      test.skip(!process.env.CONF_FAIL_ONLY, 'Run: CONF_FAIL_ONLY=1 ./scripts/test.sh conference-fail');

      const reportDir = path.resolve(__dirname, '..', CONF_REPORT_DIR);
      const summary = JSON.parse(
        fs.readFileSync(path.join(reportDir, 'frap-report.json'), 'utf-8')
      );
      const rejected = summary.summary.rejectedHeals ?? 0;
      const unexpected = summary.summary.unexpectedHeals ?? 0;
      expect(rejected + unexpected).toBeGreaterThan(0);
    });
  });
});
