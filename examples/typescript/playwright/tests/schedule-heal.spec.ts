import { test, expect } from '@playwright/test';
import { withFrap, getLastHealResult } from '@frap/playwright';
import { DEMO_PATH, demoFrap } from './helpers';

test.describe('Self-healing schedule link', () => {
  test('opens talk after testid refactor', async ({ page }, testInfo) => {
    await page.goto(DEMO_PATH.scheduleHeal);

    const openLink = await withFrap(
      page.locator('[data-testid="talk-open-healing"]'),
      page,
      demoFrap({ minConfidence: 0.7, healPolicy: 'expect_heal', debug: true, testInfo })
    );

    await openLink.click();
    await expect(page).toHaveURL(/talk\.html\?id=healing/);

    const healResult = getLastHealResult(openLink);
    expect(healResult?.healed).toBe(true);
    expect(healResult!.confidence).toBeGreaterThanOrEqual(0.7);
  });
});
