import { test, expect } from '@playwright/test';
import { attachFlettaContext, recordContextUiEvent } from '@fletta/playwright';
import {
  networkBeforeUiFailure,
  eventsByTraceId,
  getTimelineWindow,
  type ContextEvent,
} from '@fletta/sdk';
import * as fs from 'fs';
import * as path from 'path';
import { CONTEXT_PATH, CONTEXT_REPORT_DIR } from './helpers';

type NetworkEventRow = ContextEvent & {
  request?: { url: string; phase: string; status?: number };
};

test.describe('C002 API Timeout RCA', () => {
  test.fail('payment API timeout precedes missing pay button', async ({ page }, testInfo) => {
    attachFlettaContext(page, {
      reportDir: CONTEXT_REPORT_DIR,
      testId: testInfo.title,
    });

    await page.goto(CONTEXT_PATH.checkoutSlow);

    try {
      await expect(page.getByTestId('pay-btn')).toBeVisible({ timeout: 10_000 });
    } catch {
      recordContextUiEvent(
        CONTEXT_REPORT_DIR,
        '[data-testid=pay-btn]',
        'not_found',
        'Pay button not rendered after slow payment-intent',
        testInfo.title
      );
      throw new Error('pay button not visible (expected for C002)');
    }
  });

  test.afterAll(() => {
    const reportPath = path.join(CONTEXT_REPORT_DIR, 'fletta-context.json');
    if (!fs.existsSync(reportPath)) {
      throw new Error(`Expected context report at ${reportPath}`);
    }
    const report = JSON.parse(fs.readFileSync(reportPath, 'utf-8'));
    const timeline = report.timeline as { events: ContextEvent[] };

    const paymentEvents = timeline.events.filter(
      (e) =>
        e.kind === 'network' &&
        (e as NetworkEventRow).request?.url.includes('payment-intent')
    ) as NetworkEventRow[];
    expect(paymentEvents.length).toBeGreaterThan(0);

    const paymentFail = paymentEvents.find(
      (e) =>
        e.request?.phase === 'failed' ||
        e.request?.status === 504 ||
        (e.request?.status !== undefined && e.request.status >= 500)
    );
    expect(paymentFail).toBeDefined();

    const uiFail = timeline.events.find(
      (e) => e.kind === 'ui' && e.action === 'not_found'
    );
    expect(uiFail).toBeDefined();

    const logError = timeline.events.find(
      (e) => e.kind === 'log' && e.log.level === 'error'
    );
    expect(logError).toBeDefined();

    const traceId = uiFail?.trace_id;
    expect(traceId).toBeDefined();
    const correlated = eventsByTraceId({ events: timeline.events }, traceId!);
    expect(correlated.some((e) => e.kind === 'network')).toBe(true);
    expect(correlated.some((e) => e.kind === 'log')).toBe(true);
    expect(correlated.some((e) => e.kind === 'ui')).toBe(true);

    const failureAt = uiFail!.timestamp_ms;
    expect(networkBeforeUiFailure({ events: timeline.events }, failureAt, 5000)).toBe(true);

    const window = getTimelineWindow({ events: timeline.events }, failureAt, 5000);
    expect(window.some((e) => e.kind === 'network' && (e as NetworkEventRow).request?.url.includes('payment-intent'))).toBe(true);
    expect(window.some((e) => e.kind === 'ui' && e.action === 'not_found')).toBe(true);
  });
});
