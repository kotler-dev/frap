import { test, expect } from '@playwright/test';
import { CONF_PATH } from './helpers';

test.describe('Conference 2026 Spring', () => {
  test.describe('Speakers', () => {
    test('CONF-PW-SPK-PASS: native role locator without Frap', async ({ page }) => {
      await page.goto(CONF_PATH.speakers);

      await page.getByRole('link', { name: 'Профиль спикера Алексей' }).click();
      await expect(page).toHaveURL(/speaker\.html\?id=alexey/);
      await expect(page.getByRole('heading', { name: 'Алексей Тестов' })).toBeVisible();
    });
  });
});
