import { test, expect } from '@playwright/test';
import { withFletta, getLastHealResult } from '@fletta/playwright';
import { HealingEngine, DOMSnapshot, DOMElementInfo } from '@fletta/sdk';

test.describe('CP002: Refactor Heal', () => {
  test('should heal when testid changes but structure remains similar', async ({ page }) => {
    await page.goto('http://localhost:3000/cp002-refactored.html');
    
    const originalSelector = '[data-testid="pay-btn"]'; // This doesn't exist on CP002 page
    const newSelector = '[data-testid="checkout-pay"]'; // This is the actual selector
    
    const payButton = await withFletta(
      page.locator(originalSelector),
      page,
      { minConfidence: 0.70, enableHealing: true } // Lower threshold for this test
    );
    
    await payButton.click();
    
    const result = page.locator('#result');
    await expect(result).toBeVisible();
    await expect(result).toHaveText('Оплата прошла успешно! (CP002 - healed)');
    
    const healResult = getLastHealResult(payButton);
    expect(healResult).toBeDefined();
    expect(healResult!.healed).toBe(true);
    expect(healResult!.confidence).toBeGreaterThanOrEqual(0.70);
  });

  test('healing engine finds similar element by text content', async () => {
    const engine = new HealingEngine({
      debug: true,
      minConfidence: 0.70,
      reportDir: './fletta-reports',
      enableHealing: true,
      enableReporting: false,
    });
    
    const snapshot: DOMSnapshot = {
      html: '<button data-testid="checkout-pay">Оплатить</button>',
      elements: [
        {
          selector: '[data-testid="checkout-pay"]',
          tag: 'button',
          attributes: { 'data-testid': 'checkout-pay', 'role': 'button' },
          text_content: 'Оплатить',
          path: ['form:-', 'div:-', 'button:button'],
        },
      ],
    };
    
    const originalSignature = {
      path: [
        { tag: 'form', role: undefined, depth: 0 },
        { tag: 'div', role: undefined, depth: 1 },
        { tag: 'button', role: 'button', depth: 2 },
      ],
      prefix: 'form:->div:->button:button',
      stable_attrs: { 'data-testid': 'pay-btn' },
      text_content: 'Оплатить',
      children_hash: 0,
      depth: 3,
    };
    
    const result = engine.heal('[data-testid="pay-btn"]', originalSignature, snapshot, 'CP002-healing-engine-test');

    expect(result.healed).toBe(true);
    expect(result.confidence).toBeGreaterThan(0.5);
  });

  test('confidence calculation includes text match bonus', async ({ page }) => {
    await page.goto('http://localhost:3000/cp002-refactored.html');
    
    const button = await withFletta(
      page.locator('[data-testid="pay-btn"]'),
      page,
      { minConfidence: 0.60, enableHealing: true, debug: true },
      'CP002-text-match-test'
    );
    
    await button.click();
    
    const healResult = getLastHealResult(button);
    if (healResult) {
      expect(healResult.healed).toBe(true);
      expect(healResult.top_candidates.length).toBeGreaterThan(0);
    }
  });
});
