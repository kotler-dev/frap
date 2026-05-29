# Traceability matrix

Feature ↔ case ↔ automation ↔ fixtures. **Status SSOT:** [FEATURES.md](./FEATURES.md).

Rule from FEATURES.md: shipped = code + tests + docs + at least one passing case.

## Shipped (validated gates)

| Feature | Cases | E2E / tests | Fixtures | Gate |
|---------|-------|-------------|----------|------|
| F000 Core API | CP002 (→ CONF) | `crates/` tests, `smoke-frap-core-rpc.sh` | `fixtures/contract/` | CI `rust-core` |
| F001 Self-healing | C001 concept; **CONF-SH-*** | `e2e/conference/schedule*.spec.ts`, `cfp.spec.ts` | `fixtures/fixtureconf/conference/schedule-*`, `cfp.html` | `./scripts/test.sh conference` |
| F008 Playwright (TS) | CP001–CP005 (→ CONF) | `e2e/conference/*.spec.ts` | `fixtures/fixtureconf/conference/` | `./scripts/test.sh conference` |
| F012 Debug trace | — | `e2e/debug-mode.spec.ts` | — | `./scripts/test.sh debug` |
| F013 TypeScript SDK | CP002/CP003 | Conference + context E2E | — | `./scripts/build.sh` + E2E |
| F002 Unified context | **C002**, **C003**, C004 | `e2e/context/c002-*`, `c003-*`, `c004-*`, `overhead.spec.ts` | `fixtures/fixtureconf/context/` | `./scripts/test.sh context`, `bench-context.sh` |
| F003 RCA | C002, C003 | `e2e/context/verify-rca.mjs`, specs above | same | `./scripts/test.sh context` |
| F014 Java Playwright | CONF-*, C002 | `examples/java/playwright/` `@Tag("e2e")` | `fixtures/fixtureconf/` | `./scripts/run-java-e2e.sh` |
| F004 Page Object (Java) | C004 concept | `DiscoveryPageObjectE2eTest` | conference pages | `./scripts/run-java-e2e.sh` |

## Partial / docs-only

| Feature | Cases | Notes |
|---------|-------|-------|
| F004 PO gen (TS/CLI) | C004 | Java RPC shipped; npm export backlog |
| F017 Structural contract | C010 | F017.0 docs; no E2E gate yet |
| F005 MCP | C005 | Not implemented |
| F006 Multi-platform | C006 | v3 roadmap |
| F011 AI-agent testing | C007, C008 | v3 roadmap |
| F009 Feedback | CP006 | Not implemented |

## Conference CONF-* (primary demo)

Full matrix: [cases/conference/CASES.md](./cases/conference/CASES.md).

| Legacy CP | CONF (examples) | Area |
|-----------|-----------------|------|
| CP001 | CONF-PW-REG-PASS | registration |
| CP002 | CONF-SH-SCHED-PASS | schedule heal |
| CP003 | CONF-SH-CFP-FAIL | ambiguous heal fail |
| CP004 | CONF-PW-SPK-PASS | speakers |
| CP005 | CONF-RPT-RUN-PASS | JUnit + json reports |

## Context layer

| Case | Spec | Fixture page |
|------|------|--------------|
| C002 API timeout | `e2e/context/c002-payment-timeout.spec.ts` | `fixtures/fixtureconf/context/checkout.html` |
| C003 Flaky cart | `e2e/context/c003-flaky-cart.spec.ts` | `fixtures/fixtureconf/context/cart.html` |
| C004 WebSocket | `e2e/context/c004-websocket.spec.ts` | `fixtures/fixtureconf/context/ws-cart.html` |

Case specs: [cases/C002-api-timeout.md](./cases/C002-api-timeout.md), [C003-flaky-cart.md](./cases/C003-flaky-cart.md).

## Release matrices

| Surface | Matrix |
|---------|--------|
| npm `@frap/sdk` 1.1.1 | [release/typescript/npm-1.1.1-matrix.md](./release/typescript/npm-1.1.1-matrix.md) |
| Maven Java 1.0.0 | [release/java/java-sdk-1.0.0-matrix.md](./release/java/java-sdk-1.0.0-matrix.md) |
| Rust core 0.1.0 | [release/core/rust-0.1.0.md](./release/core/rust-0.1.0.md) |
