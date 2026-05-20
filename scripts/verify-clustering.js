#!/usr/bin/env node

/**
 * Manual verification script for clustering algorithm
 * Usage: node scripts/verify-clustering.js
 */

const path = require('path');

// Load the built SDK
const sdkPath = path.join(__dirname, '../sdk/typescript/dist/core.js');
const { HealingEngine } = require(sdkPath);

console.log('=== fletta Clustering Verification ===\n');

// Test CP002: Refactor Heal
console.log('Test 1: CP002 - Refactor Heal');
console.log('==============================');

const cp002Engine = new HealingEngine({
  minConfidence: 0.70,
  reportDir: './fletta-reports',
  enableHealing: true,
  enableReporting: false,
});

const cp002Snapshot = {
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

const cp002Original = {
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

console.log('Original signature text: "Оплатить"');
console.log('Target selector: [data-testid="pay-btn"] (does not exist)');
console.log('Expected: Heal to [data-testid="checkout-pay"]\n');

const cp002Result = cp002Engine.heal('[data-testid="pay-btn"]', cp002Original, cp002Snapshot);

console.log('\nResult:');
console.log('  Healed:', cp002Result.healed);
console.log('  Confidence:', cp002Result.confidence.toFixed(2));
console.log('  New selector:', cp002Result.selector);
console.log('  Diff:', cp002Result.diff);
console.log('  Top candidates:', cp002Result.top_candidates.length);

const cp002Pass = cp002Result.healed && cp002Result.confidence >= 0.70;
console.log('\nCP002 Status:', cp002Pass ? '✓ PASS' : '✗ FAIL');

// Test CP003: Safe Fail (Ambiguous)
console.log('\n\nTest 2: CP003 - Safe Fail (Ambiguous)');
console.log('=======================================');

const cp003Engine = new HealingEngine({
  minConfidence: 0.85,
  reportDir: './fletta-reports',
  enableHealing: true,
  enableReporting: false,
});

const cp003Snapshot = {
  html: `
    <button data-testid="pay-btn-main">Оплатить</button>
    <button data-testid="pay-btn-addon">Оплатить</button>
  `,
  elements: [
    {
      selector: '[data-testid="pay-btn-main"]',
      tag: 'button',
      attributes: { 'data-testid': 'pay-btn-main', 'role': 'button' },
      text_content: 'Оплатить',
      path: ['div:main', 'button:button'],
    },
    {
      selector: '[data-testid="pay-btn-addon"]',
      tag: 'button',
      attributes: { 'data-testid': 'pay-btn-addon', 'role': 'button' },
      text_content: 'Оплатить',
      path: ['div:addon', 'button:button'],
    },
  ],
};

const cp003Original = {
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

console.log('Original signature text: "Оплатить"');
console.log('Target selector: [data-testid="pay-btn"] (does not exist)');
console.log('Available: Two buttons with same text but different contexts');
console.log('Expected: FAIL (ambiguous) with top-3 candidates\n');

const cp003Result = cp003Engine.heal('[data-testid="pay-btn"]', cp003Original, cp003Snapshot);

console.log('\nResult:');
console.log('  Healed:', cp003Result.healed);
console.log('  Confidence:', cp003Result.confidence.toFixed(2));
console.log('  Diff:', cp003Result.diff);
console.log('  Top candidates:', cp003Result.top_candidates.length);
cp003Result.top_candidates.forEach((c, i) => {
  console.log(`    ${i}: "${c.selector}" confidence=${c.confidence.toFixed(2)}`);
});

const cp003Pass = !cp003Result.healed && cp003Result.top_candidates.length >= 2;
console.log('\nCP003 Status:', cp003Pass ? '✓ PASS' : '✗ FAIL');

// Summary
console.log('\n\n=== Summary ===');
console.log('CP002 (Refactor Heal):', cp002Pass ? '✓ PASS' : '✗ FAIL');
console.log('CP003 (Safe Fail):', cp003Pass ? '✓ PASS' : '✗ FAIL');
console.log('Overall:', (cp002Pass && cp003Pass) ? '✓ ALL TESTS PASS' : '✗ SOME TESTS FAILED');
