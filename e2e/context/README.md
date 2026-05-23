# Context Layer E2E (C002, C003, C004)

Demos for F002 Unified Context: HTTP + WebSocket network, console capture, and `fletta-context.json`.

## Prerequisites

```bash
./scripts/start.sh    # test-app on :3000
./scripts/build.sh
```

## Run

```bash
./scripts/test.sh context
```

Or:

```bash
cd e2e
npx playwright test --config=playwright.context.config.ts
node context/verify-context.mjs
```

Performance gate:

```bash
./scripts/bench-context.sh
```

## Expected

- `C002` and slow `C003` use `test.fail` (intentional failure; counted as pass).
- `fletta-reports/context/fletta-context.json` contains HTTP + WebSocket network, logs, and UI events.
- `verify-context.mjs` validates schema after the suite.

## Pages

| Case | URL |
|------|-----|
| C002 | `/context/checkout.html?mode=slow` |
| C003 fast | `/context/cart.html?delay=100` |
| C003 slow | `/context/cart.html?delay=600` |
| C004 WS | `/context/ws-cart.html` (WebSocket echo via `/ws/cart`) |
