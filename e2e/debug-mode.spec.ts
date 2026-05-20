import { test, expect } from '@playwright/test';
import { HealingEngine, DOMSnapshot } from '@fletta/sdk';
import * as fs from 'fs';
import * as path from 'path';

test.describe('F012: Debug Trace Mode', () => {
  test('debug mode generates JSON report', async () => {
    const reportDir = './test-reports-debug';
    
    // Clean up previous test runs
    if (fs.existsSync(reportDir)) {
      fs.rmSync(reportDir, { recursive: true });
    }

    const engine = new HealingEngine({
      minConfidence: 0.70,
      reportDir,
      enableHealing: true,
      enableReporting: false,
      debug: true,
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

    const result = engine.heal('[data-testid="pay-btn"]', originalSignature, snapshot, 'debug-mode-test');

    expect(result.healed).toBe(true);

    // Verify debug report was created
    const debugReportPath = path.join(reportDir, 'fletta-debug.json');
    expect(fs.existsSync(debugReportPath)).toBe(true);

    const debugReport = JSON.parse(fs.readFileSync(debugReportPath, 'utf-8'));
    
    // Verify report structure
    expect(debugReport.testName).toBe('debug-mode-test');
    expect(debugReport.timestamp).toBeDefined();
    expect(debugReport.duration_ms).toBeGreaterThanOrEqual(0);
    expect(debugReport.steps).toHaveLength(4);
    expect(debugReport.clusters).toBeDefined();
    expect(debugReport.healing).toBeDefined();
    expect(debugReport.healing.healed).toBe(true);
    expect(debugReport.healing.confidence).toBeGreaterThan(0.5);

    // Verify steps
    const stepNames = debugReport.steps.map((s: any) => s.name);
    expect(stepNames).toContain('dom_parsed');
    expect(stepNames).toContain('clusters_built');
    expect(stepNames).toContain('candidates_ranked');
    expect(stepNames).toContain('healing_decision');

    // Cleanup
    fs.rmSync(reportDir, { recursive: true });
  });

  test('debug mode works with no candidates found', async () => {
    const reportDir = './test-reports-debug-fail';
    
    // Clean up previous test runs
    if (fs.existsSync(reportDir)) {
      fs.rmSync(reportDir, { recursive: true });
    }

    const engine = new HealingEngine({
      minConfidence: 0.90, // High threshold to force failure
      reportDir,
      enableHealing: true,
      enableReporting: false,
      debug: true,
    });

    const snapshot: DOMSnapshot = {
      html: '<div>unrelated element</div>',
      elements: [
        {
          selector: 'div',
          tag: 'div',
          attributes: {},
          text_content: 'unrelated element',
          path: ['body:-', 'div:-'],
        },
      ],
    };

    const originalSignature = {
      path: [
        { tag: 'button', role: 'submit', depth: 0 },
      ],
      prefix: 'button:submit',
      stable_attrs: {},
      text_content: 'Pay',
      children_hash: 0,
      depth: 1,
    };

    const result = engine.heal('[data-testid="pay-btn"]', originalSignature, snapshot, 'debug-mode-no-candidates-test');

    expect(result.healed).toBe(false);

    // Verify debug report was still created even for failure
    const debugReportPath = path.join(reportDir, 'fletta-debug.json');
    expect(fs.existsSync(debugReportPath)).toBe(true);

    const debugReport = JSON.parse(fs.readFileSync(debugReportPath, 'utf-8'));
    expect(debugReport.testName).toBe('debug-mode-no-candidates-test');
    expect(debugReport.healing.healed).toBe(false);
    expect(debugReport.steps).toHaveLength(4);

    // Cleanup
    fs.rmSync(reportDir, { recursive: true });
  });

  test('debug mode disabled does not generate report', async () => {
    const reportDir = './test-reports-no-debug';
    
    // Clean up previous test runs
    if (fs.existsSync(reportDir)) {
      fs.rmSync(reportDir, { recursive: true });
    }

    const engine = new HealingEngine({
      minConfidence: 0.70,
      reportDir,
      enableHealing: true,
      enableReporting: false,
      debug: false, // Disabled
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

    const result = engine.heal('[data-testid="pay-btn"]', originalSignature, snapshot, 'no-debug-test');

    expect(result.healed).toBe(true);

    // Verify debug report was NOT created
    const debugReportPath = path.join(reportDir, 'fletta-debug.json');
    expect(fs.existsSync(debugReportPath)).toBe(false);

    // Cleanup
    if (fs.existsSync(reportDir)) {
      fs.rmSync(reportDir, { recursive: true });
    }
  });
});
