# frap — engineering overview

Short intro for contributors. Product pitch → [docs/positioning.md](../docs/positioning.md) · full vision → [Frap.md](../Frap.md).

## What we build

Deterministic Rust core (signatures, clustering, healing, context, RCA) exposed via thin SDKs and adapters. **No ML in core**, explainable scores and diffs, on-prem first.

## Three layers

| Layer | Scope | Release |
|-------|--------|---------|
| 1 — Discovery & heal | Core, WASM, Playwright/Java adapters | v1.0+ |
| 2 — Generation | Page Object, MCP (roadmap) | v1.2+ |
| 3 — Observability | RCA, drift, health (roadmap) | v1.1–v2.0 |

Details: [architecture/README.md](./architecture/README.md).

## Where to look

| Need | Doc |
|------|-----|
| Feature status (SSOT) | [FEATURES.md](./FEATURES.md) |
| Feature ↔ E2E ↔ fixtures | [traceability.md](./traceability.md) |
| Scenario specs | [cases/](./cases/) |
| Published versions | [release/README.md](./release/README.md) |
| ADR / core guardrails | [architecture/ADR-001-core-language-strategy.md](./architecture/ADR-001-core-language-strategy.md) |

## Verify locally

```bash
./scripts/build.sh
./scripts/test.sh conference
./scripts/test.sh context
./scripts/run-java-e2e.sh   # Java
```

## Principles

Integration not replacement (Playwright/Selenium), algorithms only in Core ([ADR-001](./architecture/ADR-001-core-language-strategy.md)), tri-plane context (UI + logs + network).
