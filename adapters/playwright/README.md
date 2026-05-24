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
| `captureAll` | boolean | false | Unified context: network + console + UI → `fletta-context.json` (F002) |

## Unified context (F002)

```typescript
import { attachFlettaContext } from '@fletta/playwright';

test.beforeEach(async ({ page }) => {
  attachFlettaContext(page, { reportDir: './fletta-reports', traceId: 'run-1' });
});
```

Enable `captureAll: true` in `flettaPlaywright({ ... })` to write `fletta-context.json` next to `fletta-report.json`.

Network events include HTTP (`protocol: http`, phases `request`/`response`/`failed`) and WebSocket (`protocol: websocket`, phases `open`/`message`/`close`). Message payloads are truncated to 256 characters in `payload_preview`.

## Reports

After running tests, find reports in `fletta-reports/`:
- `fletta-report.json` — events with `trigger`, `policy`, `outcome`; summary includes `unexpectedHeals`; with `captureAll`: `context_summary` and `context_tests[]`
- `fletta-context.json` — unified timeline (when `captureAll` is enabled)
- `fletta-rca.json` — Root Cause Analysis v2 with `suite` (merged) and `by_test[]` (per-failed-test)
- `junit.xml` — JUnit XML with `fletta-context` suite for all tests; `fletta` suite only when healing events exist
- `fletta-debug.html` — Classic view (A): single test report, or grouped index when 2+ tests use `debug: true`
- `fletta-debug-explorer.html` — Explorer view (B): sidebar + search when 2+ debug reports; stub with link to A when only 1
- `debug-reports/` — per-test JSON/HTML + `manifest.json`

### Context Layer Reports (F002 + F003)

When `captureAll: true`, the reporter tracks all Playwright tests (not just failures):

**fletta-report.json** includes:
```json
{
  "context_summary": {
    "total": 5,
    "passed": 3,
    "failed": 2,
    "skipped": 0,
    "durationMs": 12345
  },
  "context_tests": [
    { "playwrightTestId": "...", "status": "passed", "durationMs": 329, "timestamp": "..." },
    { "playwrightTestId": "...", "status": "failed", "durationMs": 10200, "message": "...", "timestamp": "..." }
  ],
  "rca": { "version": 2, "suite": {...}, "by_test": [...] }
}
```

**fletta-rca.json v2** format:
```json
{
  "version": 2,
  "generated_at": "2026-05-24T08:11:08.759Z",
  "suite": {
    "primary_cause": "flaky",
    "confidence": 0.92,
    "recommendation": "..."
  },
  "by_test": [
    {
      "playwrightTestId": "...C002...",
      "traceId": "uuid",
      "rca": {
        "primary_cause": "api_error",
        "details": { "endpoint": "/api/payment-intent", "status": 504 }
      }
    }
  ]
}
```

- **suite**: RCA from merged timeline (all events) — good for detecting cross-test patterns like flaky latency
- **by_test**: Per-test RCA using `trace_id` correlation — isolated timeline for each failed test

**junit.xml** with `captureAll`:
- Single `fletta-context` testsuite containing **all** test results
- Passed tests have no `<failure>` element
- Failed tests include RCA summary in `<failure message="[fletta-rca] ...">`
- `fletta` suite (healing) only emitted when `enableHealing: true` produces events

## How It Works

1. Primary selector is attempted first
2. If not found, fletta extracts DOM signature
3. Similar elements are found using clustering (Drain3 algorithm)
4. Confidence score is calculated for each candidate
5. If best candidate >= minConfidence, element is "healed"
6. Report includes original selector, new selector, and confidence
