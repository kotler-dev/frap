import { test, expect } from '@playwright/test';
import { withFrap, getLastHealResult } from '@frap/frap-playwright';
import { CONF_PATH, confFrap } from './helpers';

test.describe('Conference 2026 Spring', () => {
  test.describe('Navigation', () => {
    test('CONF-PW-NAV-FAIL: broken nav selector does not pass', async ({ page }, testInfo) => {
      await page.goto(CONF_PATH.index);

      const link = await withFrap(
        page.locator('[data-testid="nav-schedule-ghost"]'),
        page,
        confFrap({ debug: true, testInfo })
      );

      await expect(async () => {
        await link.click();
      }).rejects.toThrow();

      const healResult = getLastHealResult(link);
      expect(healResult?.healed).not.toBe(true);
    });
  });
});
