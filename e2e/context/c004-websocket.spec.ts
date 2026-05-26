import { test, expect } from '@playwright/test';
import { attachFrapContext } from '@frap/frap-playwright';
import { CONTEXT_REPORT_DIR } from './helpers';

test.describe('C004 WebSocket capture', () => {
  test('WebSocket open and message events in timeline', async ({ page }, testInfo) => {
    attachFrapContext(page, {
      reportDir: CONTEXT_REPORT_DIR,
      testId: testInfo.title,
    });

    await page.goto('/context/ws-cart.html');
    await expect(page.getByTestId('ws-ready')).toBeVisible({ timeout: 5000 });
  });
});
