# Context Layer E2E (C002, C003, C004)

Demos for F002 Unified Context and F003 Root Cause Analysis: HTTP + WebSocket network, console capture, timeline, and RCA reports.

## Prerequisites

```bash
./scripts/start.sh    # test-app on :3000
./scripts/build.sh
```

## Run

```bash
./scripts/test.sh context
```

This executes:
1. `cargo test -p fletta-context` — Rust unit tests
2. `npx playwright test --config=playwright.context.config.ts` — E2E tests
3. `node context/verify-context.mjs` — validate timeline schema
4. `node context/verify-reports.mjs` — CP005-equivalent gate (reports structure)
5. `node context/verify-rca.mjs` — validate RCA classification

Or run manually:

```bash
cd e2e
npx playwright test --config=playwright.context.config.ts
node context/verify-context.mjs
node context/verify-reports.mjs
node context/verify-rca.mjs
```

Performance gate:

```bash
./scripts/bench-context.sh
```

## Report Artifacts

Located in `e2e/fletta-reports/context/` (not committed to git):

| File | Description | Schema |
|------|-------------|--------|
| `fletta-context.json` | Full timeline: HTTP, WebSocket, console, UI events | version: 1, timeline.events[] |
| `fletta-context-events.jsonl` | Raw events line-by-line (pre-aggregation) | JSONL |
| `fletta-rca.json` | Root Cause Analysis v2 | version: 2, suite + by_test[] |
| `fletta-report.json` | Fletta summary with context_summary | summary (healing) + context_summary + context_tests[] + rca |
| `junit.xml` | JUnit XML with fletta-context suite | 5 testcases (3 passed, 2 failed with RCA) |

**Note:** Artifacts are in `.gitignore`. Run `./scripts/test.sh context` to generate fresh reports locally.

## fletta-rca.json v2 Format

```json
{
  "version": 2,
  "generated_at": "2026-05-24T08:11:08.759Z",
  "suite": {
    "primary_cause": "flaky",
    "confidence": 0.92,
    "details": { "pattern": "api latency spread >= 400ms..." },
    "recommendation": "Investigate intermittent latency for /api/cart"
  },
  "by_test": [
    {
      "playwrightTestId": "...C002 payment API timeout...",
      "traceId": "...",
      "rca": {
        "primary_cause": "api_error",
        "details": { "endpoint": "/api/payment-intent", "status": 504 }
      }
    }
  ]
}
```

- **suite**: RCA from merged timeline (all tests)
- **by_test**: Per-test RCA for failed tests (isolated timeline by trace_id)

## fletta-report.json Context Section

```json
{
  "timestamp": "...",
  "duration": 14278,
  "summary": { "totalAttempts": 0, ... },
  "events": [],
  "context_summary": {
    "total": 5,
    "passed": 3,
    "failed": 2,
    "skipped": 0,
    "durationMs": 12345
  },
  "context_tests": [
    { "playwrightTestId": "...", "status": "passed", "durationMs": 329, ... },
    { "playwrightTestId": "...", "status": "failed", "durationMs": 10200, "message": "..." }
  ],
  "rca": { "version": 2, "suite": {...}, "by_test": [...] }
}
```

## Expected Test Results

- `C002` and slow `C003` use `test.fail` (intentional failure; counted as pass by Playwright).
- JUnit reports 4 testcases in `test.sh context`: 2 passed (C003 fast, C004), 2 failed (C002, C003 slow).
- Overhead gate (`< 20%`): `./scripts/bench-context.sh` (runs `overhead.spec.ts` only).
- Both failed tests include RCA in `<failure message="[fletta-rca] ...">`.

## Pages

| Case | URL |
|------|-----|
| C002 | `/context/checkout.html?mode=slow` |
| C003 fast | `/context/cart.html?delay=100` |
| C003 slow | `/context/cart.html?delay=600` |
| C004 WS | `/context/ws-cart.html` (WebSocket echo via `/ws/cart`) |
