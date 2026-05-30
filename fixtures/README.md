# Fixtures

Shared test data and demo applications for Frap SDK examples, E2E, and CI.

| Path | Purpose |
|------|---------|
| [fixtureconf/](fixtureconf/) | FixtureConf 2026 — static conference + context demo app (`:3000`) |
| [contract/](contract/) | Language-agnostic JSON/HTML contracts (clustering, element-map) |

## Quick start

```bash
./scripts/start.sh    # serves fixtureconf at http://localhost:3000
```

Used by [`examples/`](../examples/), [`e2e/`](../e2e/), and CI.
