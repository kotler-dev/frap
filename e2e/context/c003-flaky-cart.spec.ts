import { test, expect } from '@playwright/test';
import { attachFlettaContext, recordContextUiEvent } from '@frap/playwright';
import { CONTEXT_PATH, CONTEXT_REPORT_DIR } from './helpers';

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
});
