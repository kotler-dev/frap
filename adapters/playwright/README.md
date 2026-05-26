# frapcode-playwright

Playwright adapter for Frap deterministic selectors.

## Installation

```bash
npm install frapcode-playwright
```

## Quick Start

### Option 1: Custom Selector Engine

```typescript
// playwright.config.ts
import { defineConfig } from '@playwright/test';
import { frapPlaywright, registerFrapSelector } from 'frapcode-playwright';

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

```typescript
// test.spec.ts
import { test, expect } from '@playwright/test';

test('payment flow', async ({ page }) => {
  // Use frap: prefix for self-healing selectors
  await page.locator('frap:[data-testid="pay-btn"]').click();
});
```

### Option 2: Wrapper API

```typescript
// test.spec.ts
import { test, expect } from '@playwright/test';
import { withFrap } from 'frapcode-playwright';

test('payment flow', async ({ page }) => {
  // Wrap existing locator with Frap healing
  const payButton = await withFrap(
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
| `reportDir` | string | './frap-reports' | Directory for healing reports |
| `enableHealing` | boolean | true | Enable self-healing |
| `enableReporting` | boolean | true | Enable report generation |
| `healPolicy` | `'allow' \| 'deny' \| 'expect_heal'` | `allow` | Gate semantics for reports (CP001: `deny`, CP002: `expect_heal`) |
| `captureAll` | boolean | false | Unified context: network + console + UI → `frap-context.json` (F002) |

## Unified context (F002)

```typescript
import { attachFrapContext } from 'frapcode-playwright';

test.beforeEach(async ({ page }) => {
  attachFrapContext(page, { reportDir: './frap-reports', traceId: 'run-1' });
});
```

Enable `captureAll: true` in `frapPlaywright({ ... })` to write `frap-context.json` next to `frap-report.json`.

Network events include HTTP (`protocol: http`, phases `request`/`response`/`failed`) and WebSocket (`protocol: websocket`, phases `open`/`message`/`close`). Message payloads are truncated to 256 characters in `payload_preview`.

## Reports

After running tests, find reports in `frap-reports/`:
- `frap-report.json` — events with `trigger`, `policy`, `outcome`; summary includes `unexpectedHeals`; with `captureAll`: `context_summary` and `context_tests[]`
- `frap-context.json` — unified timeline (when `captureAll` is enabled)
- `frap-rca.json` — Root Cause Analysis v2 with `suite` (merged) and `by_test[]` (per-failed-test)
- `junit.xml` — JUnit XML with `frapcode-context` suite for all tests; `frap` suite only when healing events exist
- `frap-debug.html` — Classic view (A): single test report, or grouped index when 2+ tests use `debug: true`
- `frap-debug-explorer.html` — Explorer view (B): sidebar + search when 2+ debug reports; stub with link to A when only 1
- `debug-reports/` — per-test JSON/HTML + `manifest.json`

### Context Layer Reports (F002 + F003)

When `captureAll: true`, the reporter tracks all Playwright tests (not just failures):

**frap-report.json** includes:
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

**frap-rca.json v2** format:
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
- Single `frapcode-context` testsuite containing **all** test results
- Passed tests have no `<failure>` element
- Failed tests include RCA summary in `<failure message="[frapcode-rca] ...">`
- `frap` suite (healing) only emitted when `enableHealing: true` produces events

## How It Works

1. Primary selector is attempted first
2. If not found, frap extracts DOM signature
3. Similar elements are found using clustering (Drain3 algorithm)
4. Confidence score is calculated for each candidate
5. If best candidate >= minConfidence, element is "healed"
6. Report includes original selector, new selector, and confidence
