import { test, expect } from '@playwright/test';
import { withFletta, getLastHealResult } from '@frap/playwright';
import { confFletta } from './helpers';

test.describe('Conference 2026 Spring', () => {
  test.describe('Clustering', () => {
    test('CONF-CL-REG-TODO: same cluster when li id migrates to data-id', async ({ page }, testInfo) => {
      // test.fixme(true, 'Enable when clustering fallback for id -> data-id migration is ready.');

      await page.setContent(`
        <ul data-testid="participants">
          <li id="1">first</li>
          <li data-id="2">second</li>
        </ul>
      `);

      const migrated = await withFletta(
        page.locator('ul[data-testid="participants"] > li[id="2"]'),
        page,
        confFletta({ minConfidence: 0.7, healPolicy: 'expect_heal', debug: true, testInfo })
      );

      await migrated.click();

      const healResult = getLastHealResult(migrated);
      expect(healResult).toBeDefined();
      expect(healResult!.healed).toBe(true);
      expect(healResult!.selector).toContain('data-id="2"');
      expect(healResult!.confidence).toBeGreaterThanOrEqual(0.7);
    });
  });
});
