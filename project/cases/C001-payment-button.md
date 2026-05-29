# C001: Payment Button Refactor

- **Status**: concept
- **Features**: F001 (Self-Healing Selectors)
- **Related PoC**: CP002 → [CONF-SH-SCHED-PASS](./conference/CASES.md)

## Context

- E-commerce checkout; button «Оплатить» with `data-testid="pay-btn"`
- After redesign ID becomes `data-testid="checkout-pay"`

## Scenario

1. Record test: click `[data-testid="pay-btn"]`
2. Deploy version with renamed test id
3. Classic selector fails
4. frap heals by signature (text, form proximity, structure)
5. Test passes; primary selector update optional (policy)

## Success criteria

Test passes without manual selector update. Implemented in Conference demo as schedule-heal flow (`CONF-SH-SCHED-PASS`).
