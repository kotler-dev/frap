# @fletta/playwright

Playwright adapter for fletta self-healing selectors.

## Installation

```bash
npm install @fletta/playwright
```

## Quick Start

### Option 1: Custom Selector Engine

```typescript
// playwright.config.ts
import { defineConfig } from '@playwright/test';
import { flettaPlaywright, registerFlettaSelector } from '@fletta/playwright';

export default defineConfig({
  ...flettaPlaywright({
    minConfidence: 0.85,
    reportDir: './fletta-reports',
  }),
  use: {
    async setup({ selectors }) {
      await registerFlettaSelector(selectors);
    },
  },
});
```

```typescript
// test.spec.ts
import { test, expect } from '@playwright/test';

test('payment flow', async ({ page }) => {
  // Use fletta: prefix for self-healing selectors
  await page.locator('fletta:[data-testid="pay-btn"]').click();
});
```

### Option 2: Wrapper API

```typescript
// test.spec.ts
import { test, expect } from '@playwright/test';
import { withFletta } from '@fletta/playwright';

test('payment flow', async ({ page }) => {
  // Wrap existing locator with fletta healing
  const payButton = await withFletta(
    page.getByTestId('pay-btn'), 
    page
  );
  await payButton.click();
});
```

## Configuration

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `minConfidence` | number | 0.85 | Minimum confidence threshold for healing |
| `reportDir` | string | './fletta-reports' | Directory for healing reports |
| `enableHealing` | boolean | true | Enable self-healing |
| `enableReporting` | boolean | true | Enable report generation |
| `healPolicy` | `'allow' \| 'deny' \| 'expect_heal'` | `allow` | Gate semantics for reports (CP001: `deny`, CP002: `expect_heal`) |

## Reports

After running tests, find reports in `fletta-reports/`:
- `fletta-report.json` — events with `trigger`, `policy`, `outcome`; summary includes `unexpectedHeals`
- `junit.xml` — JUnit XML with `<healing trigger="..." policy="..." outcome="..."/>`
- `fletta-debug.html` — Classic view (A): single test report, or grouped index when 2+ tests use `debug: true`
- `fletta-debug-explorer.html` — Explorer view (B): sidebar + search when 2+ debug reports; stub with link to A when only 1
- `debug-reports/` — per-test JSON/HTML + `manifest.json`

## How It Works

1. Primary selector is attempted first
2. If not found, fletta extracts DOM signature
3. Similar elements are found using clustering (Drain3 algorithm)
4. Confidence score is calculated for each candidate
5. If best candidate >= minConfidence, element is "healed"
6. Report includes original selector, new selector, and confidence
