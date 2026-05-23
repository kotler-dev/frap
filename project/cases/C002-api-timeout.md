# C002: API Timeout Root Cause

- **Status**: validated
- **Features**: F002, F003
- **E2E**: [e2e/context/c002-payment-timeout.spec.ts](../../e2e/context/c002-payment-timeout.spec.ts)
- **Fixture**: [test-app/context/checkout.html](../../test-app/context/checkout.html)

## Scenario

1. Open checkout with `?mode=slow` (POST `/api/payment-intent` delayed 8s → 504).
2. Test waits up to **10s** for `[data-testid=pay-btn]` — times out after API error.
3. `fletta-context.json` shows network 504 **before** UI `not_found`, correlated `trace_id`, and console `error` log.

## Run

```bash
./scripts/test.sh context
```
