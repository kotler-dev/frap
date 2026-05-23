import { test, expect } from '@playwright/test';
import { attachFlettaContext, recordContextUiEvent } from '@fletta/playwright';
import type { ContextEvent } from '@fletta/sdk';
import * as fs from 'fs';
import * as path from 'path';
import { CONTEXT_PATH, CONTEXT_REPORT_DIR } from './helpers';

type CartNetworkEvent = ContextEvent & {
  request?: { url: string; duration_ms?: number };
};

test.describe('C003 Flaky Cart Diagnosis', () => {
  test('fast cart load shows ready marker', async ({ page }, testInfo) => {
    attachFlettaContext(page, {
      reportDir: CONTEXT_REPORT_DIR,
      testId: `${testInfo.title}-fast`,
    });
    await page.goto(CONTEXT_PATH.cartFast);
    await expect(page.getByTestId('cart-ready')).toBeVisible({ timeout: 5000 });
  });

  test.fail('slow cart exceeds visibility threshold', async ({ page }, testInfo) => {
    attachFlettaContext(page, {
      reportDir: CONTEXT_REPORT_DIR,
      testId: `${testInfo.title}-slow`,
    });
    await page.goto(CONTEXT_PATH.cartSlow);

    try {
      await expect(page.getByTestId('cart-ready')).toBeVisible({ timeout: 400 });
    } catch {
      recordContextUiEvent(
        CONTEXT_REPORT_DIR,
        '[data-testid=cart-ready]',
        'not_found',
        'Cart ready marker late when /api/cart delay=600',
        `${testInfo.title}-slow`
      );
      throw new Error('cart ready not visible in time (expected for slow C003)');
    }
  });

  test.afterAll(() => {
    const reportPath = path.join(CONTEXT_REPORT_DIR, 'fletta-context.json');
    if (!fs.existsSync(reportPath)) {
      throw new Error(`Expected context report at ${reportPath}`);
    }
    const report = JSON.parse(fs.readFileSync(reportPath, 'utf-8'));
    const events = report.timeline.events as CartNetworkEvent[];

    const cartResponses = events.filter(
      (e) =>
        e.kind === 'network' &&
        e.request?.url.includes('/api/cart') &&
        e.request.duration_ms !== undefined
    );
    expect(cartResponses.length).toBeGreaterThanOrEqual(2);

    const slow = cartResponses.some((e) => (e.request?.duration_ms ?? 0) >= 500);
    const fast = cartResponses.some((e) => (e.request?.duration_ms ?? 0) < 300);
    expect(slow).toBe(true);
    expect(fast).toBe(true);

    const durations = cartResponses.map((e) => e.request!.duration_ms!);
    expect(Math.max(...durations) - Math.min(...durations)).toBeGreaterThanOrEqual(400);
  });
});
