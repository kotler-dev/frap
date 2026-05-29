# FixtureConf 2026 Spring

Fictional conference site + context fixtures — the **application under test** for Frap examples and E2E (not an SDK sample).

Default URL: `http://localhost:3000` (port via `PORT` env).

## Start

From repo root:

```bash
./scripts/start.sh
# or
node fixtures/fixtureconf/server.js
```

## Routes

| Path | Scenarios |
|------|-----------|
| `/conference/` | Self-healing (schedule-heal), ambiguous heal (cfp), registration, talks |
| `/context/checkout.html` | C002 — slow payment API / RCA |
| `/context/cart.html` | C003 — flaky cart |
| `/context/ws-cart.html` | C004 — WebSocket cart |

## Mock API (server.js)

| Endpoint | Behavior |
|----------|----------|
| `POST /api/payment-intent?mode=slow` | 504 after delay (C002) |
| `GET /api/cart?delay=N` | JSON cart with optional delay |
| WebSocket `/ws/cart` | Live cart updates (C004) |

## Consumers

- [`examples/java/playwright`](../../examples/java/playwright/)
- [`examples/typescript/playwright`](../../examples/typescript/playwright/)
- [`e2e/`](../../e2e/) · [`e2e/conference/`](../../e2e/conference/)

Add new pages here when introducing a case; keep specs in `examples/` or `e2e/`.
