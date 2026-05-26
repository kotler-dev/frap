import { test, expect } from '@playwright/test';
import { withFrap, getLastHealResult } from '@frap/frap-playwright';
import { CONF_PATH, confFrap } from './helpers';

test.describe('Conference 2026 Spring', () => {
  test.describe('Registration', () => {
    test('CONF-PW-REG-PASS: stable register submit without healing', async ({ page }, testInfo) => {
      await page.goto(CONF_PATH.register);
      await page.locator('[data-testid="register-name"]').fill('Demo User');
      await page.locator('[data-testid="register-email"]').fill('demo@fixtureconf.test');

      const submit = await withFrap(
        page.locator('[data-testid="register-submit"]'),
        page,
        confFrap({ debug: true, testInfo })
      );

      await submit.click();
      await expect(page.locator('#register-toast')).toBeVisible();
      await expect(page.locator('#register-toast')).toContainText('успешна');

      expect(getLastHealResult(submit)).toBeUndefined();
    });

    test('CONF-POL-REG-PASS: deny policy on stable UI does not heal', async ({ page }, testInfo) => {
      await page.goto(CONF_PATH.register);
      await page.locator('[data-testid="register-name"]').fill('Demo User');
      await page.locator('[data-testid="register-email"]').fill('demo@fixtureconf.test');

      const submit = await withFrap(
        page.locator('[data-testid="register-submit"]'),
        page,
        confFrap({ healPolicy: 'deny', debug: true, testInfo })
      );

      await submit.click();
      await expect(page.locator('#register-toast')).toBeVisible();
      expect(getLastHealResult(submit)).toBeUndefined();
    });

    test('CONF-SH-REG-FAIL: healing fails on missing selector', async ({ page }, testInfo) => {
      await page.goto(CONF_PATH.register);

      const ghost = await withFrap(
        page.locator('[data-testid="register-pay-legacy"]'),
        page,
        confFrap({ minConfidence: 0.95, debug: true, testInfo })
      );

      await expect(async () => {
        await ghost.click();
      }).rejects.toThrow();

      const healResult = getLastHealResult(ghost);
      expect(healResult?.healed).toBe(false);
    });
  });
});
