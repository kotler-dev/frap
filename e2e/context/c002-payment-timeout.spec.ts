import { test, expect } from '@playwright/test';
import { attachFrapContext, recordContextUiEvent } from '@frap/playwright';
import { CONTEXT_PATH, CONTEXT_REPORT_DIR } from './helpers';

test.describe('C002 API Timeout RCA', () => {
  test.fail('payment API timeout precedes missing pay button', async ({ page }, testInfo) => {
    attachFrapContext(page, {
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
});
