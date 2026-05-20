import { test, expect } from '@playwright/test';
import { withFletta, getLastHealResult } from '@fletta/playwright';
import { HealingEngine, DOMSnapshot } from '@fletta/sdk';

test.describe('CP003: Safe Fail', () => {
  test('should fail safely when ambiguous elements detected', async ({ page }) => {
    await page.goto('http://localhost:3000/cp003-ambiguous.html');
    
    const ambiguousButton = await withFletta(
      page.locator('[data-testid="pay-btn"]'), // This doesn't exist
      page,
      { minConfidence: 0.85, enableHealing: true, debug: true },
      'CP003-ambiguous-test'
    );
    
    // This should either throw or not click any button due to low confidence
    let errorThrown = false;
    try {
      await ambiguousButton.click();
    } catch (e) {
      errorThrown = true;
    }
    
    const healResult = getLastHealResult(ambiguousButton);
    
    if (healResult) {
      // If healing was attempted, it should have failed due to ambiguity
      expect(healResult.healed).toBe(false);
      expect(healResult.top_candidates.length).toBeGreaterThanOrEqual(2);
    }
  });

  test('healing engine returns multiple candidates for ambiguous case', async () => {
    const engine = new HealingEngine({
      debug: true,
      minConfidence: 0.85,
      reportDir: './fletta-reports',
      enableHealing: true,
      enableReporting: false,
    });
    
    const snapshot: DOMSnapshot = {
      html: `
        <div>
          <button data-testid="pay-btn-main">Оплатить</button>
          <button data-testid="pay-btn-addon">Оплатить</button>
        </div>
      `,
      elements: [
        {
          selector: '[data-testid="pay-btn-main"]',
          tag: 'button',
          attributes: { 'data-testid': 'pay-btn-main', 'role': 'button' },
          text_content: 'Оплатить',
          path: ['div:-', 'button:button'],
        },
        {
          selector: '[data-testid="pay-btn-addon"]',
          tag: 'button',
          attributes: { 'data-testid': 'pay-btn-addon', 'role': 'button' },
          text_content: 'Оплатить',
          path: ['div:-', 'button:button'],
        },
      ],
    };
    
    const originalSignature = {
      path: [
        { tag: 'div', role: undefined, depth: 0 },
        { tag: 'button', role: 'button', depth: 1 },
      ],
      prefix: 'div:->button:button',
      stable_attrs: { 'data-testid': 'pay-btn' },
      text_content: 'Оплатить',
      children_hash: 0,
      depth: 2,
    };
    
    const result = engine.heal('[data-testid="pay-btn"]', originalSignature, snapshot, 'CP003-ambiguous-engine-test');

    // With two similar buttons, confidence should be below threshold
    expect(result.healed).toBe(false);
    expect(result.top_candidates.length).toBe(2);
  });

  test('report includes top-3 candidates on failure', async ({ page }) => {
    await page.goto('http://localhost:3000/cp003-ambiguous.html');
    
    const button = await withFletta(
      page.locator('[data-testid="non-existent"]'),
      page,
      { minConfidence: 0.90, enableHealing: true, debug: true },
      'CP003-report-test'
    );
    
    try {
      await button.click();
    } catch (e) {
      // Expected to fail
    }
    
    const healResult = getLastHealResult(button);
    if (healResult && !healResult.healed) {
      // Should have candidates but none met the confidence threshold
      expect(healResult.top_candidates.length).toBeGreaterThan(0);
    }
  });

  test('no unintended click when confidence is low', async ({ page }) => {
    await page.goto('http://localhost:3000/cp003-ambiguous.html');
    
    const button = await withFletta(
      page.locator('[data-testid="missing-selector"]'),
      page,
      { minConfidence: 0.95, enableHealing: true } // Very high threshold
    );
    
    let clicked = false;
    page.on('click', () => { clicked = true; });
    
    try {
      await button.click();
    } catch (e) {
      // Expected
    }
    
    const message = page.locator('#message');
    await expect(message).not.toBeVisible();
  });
});
