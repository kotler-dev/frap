import { test, expect } from '@playwright/test';
import { attachFlettaContext } from '@fletta/playwright';
import type { ContextEvent } from '@fletta/sdk';
import * as fs from 'fs';
import * as path from 'path';
import { CONTEXT_REPORT_DIR } from './helpers';

type NetworkRow = ContextEvent & {
  request?: {
    protocol?: string;
    phase: string;
    direction?: string;
  };
};

test.describe('C004 WebSocket capture', () => {
  test('WebSocket open and message events in timeline', async ({ page }, testInfo) => {
    attachFlettaContext(page, {
      reportDir: CONTEXT_REPORT_DIR,
      testId: testInfo.title,
    });

    await page.goto('/context/ws-cart.html');
    await expect(page.getByTestId('ws-ready')).toBeVisible({ timeout: 5000 });
  });

  test.afterAll(() => {
    const reportPath = path.join(CONTEXT_REPORT_DIR, 'fletta-context.json');
    if (!fs.existsSync(reportPath)) {
      throw new Error(`Expected context report at ${reportPath}`);
    }
    const report = JSON.parse(fs.readFileSync(reportPath, 'utf-8'));
    const events = report.timeline.events as NetworkRow[];

    const wsEvents = events.filter(
      (e) => e.kind === 'network' && e.request?.protocol === 'websocket'
    );
    expect(wsEvents.length).toBeGreaterThan(0);

    const open = wsEvents.find((e) => e.request?.phase === 'open');
    expect(open).toBeDefined();

    const message = wsEvents.find((e) => e.request?.phase === 'message');
    expect(message).toBeDefined();
    expect(['sent', 'received']).toContain(message?.request?.direction);
  });
});
