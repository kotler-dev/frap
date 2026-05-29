import { test, expect } from '@playwright/test';
import { withFrap, getLastHealResult } from '@frap/playwright';
import { CONF_PATH, confFrap } from './helpers';

test.describe('Conference 2026 Spring', () => {
  test.describe('Schedule', () => {
    test('CONF-SH-SCHED-PASS: opens talk after testid refactor', async ({ page }, testInfo) => {
      await page.goto(CONF_PATH.scheduleHeal);

      const openLink = await withFrap(
        page.locator('[data-testid="talk-open-healing"]'),
        page,
        confFrap({ minConfidence: 0.7, healPolicy: 'expect_heal', debug: true, testInfo })
      );

      await openLink.click();
      await expect(page).toHaveURL(/talk\.html\?id=healing/);

      const healResult = getLastHealResult(openLink);
      expect(healResult?.healed).toBe(true);
      expect(healResult!.confidence).toBeGreaterThanOrEqual(0.7);
    });

    test('CONF-POL-SCHED-WARN: unexpected heal when policy is deny', async ({ page }, testInfo) => {
      await page.goto(CONF_PATH.scheduleHeal);

      const openLink = await withFrap(
        page.locator('[data-testid="talk-open-healing"]'),
        page,
        confFrap({ minConfidence: 0.7, healPolicy: 'deny', debug: true, testInfo })
      );

      await openLink.click();
      await expect(page).toHaveURL(/talk\.html\?id=healing/);

      const healResult = getLastHealResult(openLink);
      expect(healResult?.healed).toBe(true);
      expect(healResult?.semantics?.outcome).toBe('unexpected_heal');
    });
  });
});
