import { test, expect } from '@playwright/test';
import { withFletta, getLastHealResult } from '@fletta/playwright';

test.describe('CP001: Happy Path', () => {
  test('stable test should not trigger healing', async ({ page }) => {
    await page.goto('http://localhost:3000/cp001-stable.html');
    
    const payButton = await withFletta(
      page.locator('[data-testid="old111-pay"]'),
      page,
      { minConfidence: 0.85, enableHealing: true, debug: true },
      'CP001-form-submission-healing-test' 
    );
    
    await payButton.click();
    
    const status = page.locator('#status');
    await expect(status).toBeVisible();
    await expect(status).toHaveText('Оплата успешно обработана! (test)');
    
    const healResult = getLastHealResult(payButton);
    expect(healResult).toBeUndefined();
  });

  test.skip('direct locator should work without healing', async ({ page }) => {
    await page.goto('http://localhost:3000/cp001-stable.html');
    
    const emailInput = page.locator('[data-testid="email-input"]');
    await emailInput.fill('test@example.com');
    
    await expect(emailInput).toHaveValue('test@example.com');
  });

  test.skip('form submission with stable selectors', async ({ page }) => {
    await page.goto('http://localhost:3000/cp001-stable.html');
    
    await page.fill('[data-testid="email-input"]', 'user@example.com');
    await page.fill('[data-testid="card-input"]', '1234 5678 9012 3456');
    
    const payButton = await withFletta(
      page.locator('[data-testid="pay-btn1"]'),
      page,
      { enableHealing: true, debug: true },
      'CP001-form-healing'
    );
    await payButton.click();
    
    await expect(page.locator('#status')).toBeVisible();
  });
});
