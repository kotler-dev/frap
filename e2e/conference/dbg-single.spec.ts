import { test, expect } from '@playwright/test';
import { withFletta } from '@fletta/playwright';
import { CONF_PATH, confFletta } from './helpers';

test.describe('Conference 2026 Spring', () => {
  test.describe('Debug', () => {
    test('CONF-DBG-SINGLE-PASS: one debug trace on stable schedule', async ({ page }, testInfo) => {
      await page.goto(CONF_PATH.scheduleV1);

      const openLink = await withFletta(
        page.locator('[data-testid="talk-open-opening"]'),
        page,
        confFletta({ debug: true, testInfo })
      );

      await openLink.click();
      await expect(page).toHaveURL(/talk\.html\?id=opening/);
    });
  });
});
