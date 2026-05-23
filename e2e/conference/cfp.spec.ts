import { test, expect } from '@playwright/test';
import { withFletta, getLastHealResult } from '@fletta/playwright';
import { CONF_PATH, confFletta } from './helpers';

test.describe('Conference 2026 Spring', () => {
  test.describe('CFP', () => {
    test('CONF-SH-CFP-FAIL: ambiguous submit buttons refuse heal', async ({ page }, testInfo) => {
      await page.goto(CONF_PATH.cfp);

      const submit = await withFletta(
        page.locator('[data-testid="cfp-submit-missing"]'),
        page,
        confFletta({ minConfidence: 0.85, debug: true, testInfo })
      );

      await expect(async () => {
        await submit.click();
      }).rejects.toThrow();

      const healResult = getLastHealResult(submit);
      expect(healResult).toBeDefined();
      expect(healResult!.healed).toBe(false);
      expect(healResult!.top_candidates.length).toBeGreaterThanOrEqual(2);
    });
  });
});
