# Quick Start

Install Frap for Playwright:

```bash
npm install @frap/frap @frap/frap-playwright
```

## Configure Playwright

```typescript
// playwright.config.ts
import { defineConfig } from '@playwright/test';
import { frapPlaywright, registerFrapSelector } from '@frap/frap-playwright';

export default defineConfig({
  ...frapPlaywright({
    minConfidence: 0.85,
    reportDir: './frap-reports',
  }),
  use: {
    async setup({ selectors }) {
      await registerFrapSelector(selectors);
    },
  },
});
```

## Use in Tests

```typescript
// test.spec.ts
import { test, expect } from '@playwright/test';

test('payment flow', async ({ page }) => {
  // Self-healing selector with frap: prefix
  await page.locator('frap:[data-testid="pay-btn"]').click();
});
```

When a selector fails, Frap finds the element by its structural signature.

## Reports

After running tests, find reports in `frap-reports/`:

- `frap-report.json` — healing events and outcomes
- `junit.xml` — JUnit format with Frap annotations

## More

- [Integrations](integrations.md) — full configuration reference
- [Adapter README](../../adapters/playwright/README.md) — detailed API
