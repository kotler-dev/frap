import { test, expect } from '@playwright/test';
import { withFrap, getLastHealResult } from '@frap/playwright';
import { CONF_PATH, confFrap } from './helpers';

test.describe('Conference 2026 Spring', () => {
  test.describe('CFP', () => {
    test('CONF-SH-CFP-FAIL: ambiguous submit buttons refuse heal', async ({ page }, testInfo) => {
      await page.goto(CONF_PATH.cfp);

      const submit = await withFrap(
        page.locator('[data-testid="cfp-submit-missing"]'),
        page,
        confFrap({ minConfidence: 0.85, debug: true, testInfo })
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
