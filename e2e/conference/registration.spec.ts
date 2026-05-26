import { test, expect } from '@playwright/test';
import { withFletta, getLastHealResult } from '@fletta/playwright';
import { CONF_PATH, confFletta } from './helpers';

test.describe('Conference 2026 Spring', () => {
  test.describe('Registration', () => {
    test('CONF-PW-REG-PASS: stable register submit without healing', async ({ page }, testInfo) => {
      await page.goto(CONF_PATH.register);
      await page.locator('[data-testid="register-name"]').fill('Demo User');
      await page.locator('[data-testid="register-email"]').fill('demo@fixtureconf.test');

      const submit = await withFletta(
        page.locator('[data-testid="register-submit"]'),
        page,
        confFletta({ debug: true, testInfo })
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

      const submit = await withFletta(
        page.locator('[data-testid="register-submit"]'),
        page,
        confFletta({ healPolicy: 'deny', debug: true, testInfo })
      );

      await submit.click();
      await expect(page.locator('#register-toast')).toBeVisible();
      expect(getLastHealResult(submit)).toBeUndefined();
    });

    test('CONF-SH-REG-FAIL: healing fails on missing selector', async ({ page }, testInfo) => {
      await page.goto(CONF_PATH.register);

      const ghost = await withFletta(
        page.locator('[data-testid="register-pay-legacy"]'),
        page,
        confFletta({ minConfidence: 0.95, debug: true, testInfo })
      );

      await expect(async () => {
        await ghost.click();
      }).rejects.toThrow();

      const healResult = getLastHealResult(ghost);
      expect(healResult?.healed).toBe(false);
    });
  });
});
