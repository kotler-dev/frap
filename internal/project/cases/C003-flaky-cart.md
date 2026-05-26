# C003: Flaky Cart Diagnosis

- **Status**: validated
- **Features**: F001, F002, F003
- **E2E**: [e2e/context/c003-flaky-cart.spec.ts](../../e2e/context/c003-flaky-cart.spec.ts)
- **Fixture**: [test-app/context/cart.html](../../test-app/context/cart.html)

## Scenario

1. Fast run: `GET /api/cart?delay=100` — cart ready marker visible (`duration_ms < 300`).
2. Slow run: `delay=600` — ready marker not visible within 400ms (intentional fail, `duration_ms ≥ 500`).
3. Single `frap-context.json` compares both `/api/cart` latencies (spread ≥ 400ms).

## Run

```bash
./scripts/test.sh context
```
