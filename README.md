# fletta

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

> Deterministic engine for UI structure extraction: stable identifiers, self-healing selectors, explainable reports. No ML in core, on-prem first, AI-ready via MCP.

**fletta** parses element trees (DOM, ViewTree, accessibility), clusters components with deterministic algorithms, and generates stable locators for Page Objects and tests. When a selector breaks, it heals by signature matching with confidence scores and diff reports.

## Quick start

```bash
./scripts/setup.sh
./scripts/build.sh
./scripts/start.sh          # test server on http://localhost:3000

# In another terminal — Conference demo (CP001–CP005)
./scripts/test.sh conference

./scripts/stop.sh
```

Manual build and test paths: [docs/benchmark.md](docs/benchmark.md).

## Playwright integration

**Custom selector engine** (recommended):

```typescript
// playwright.config.ts
import { defineConfig } from '@playwright/test';
import { flettaPlaywright, registerFlettaSelector } from '@fletta/playwright';

export default defineConfig({
  ...flettaPlaywright({ minConfidence: 0.85, reportDir: './fletta-reports' }),
  use: {
    async setup({ selectors }) {
      await registerFlettaSelector(selectors);
    },
  },
});
```

```typescript
// test.spec.ts
await page.locator('fletta:[data-testid="pay-btn"]').click();
```

**Wrapper API** — wrap an existing locator with [`withFletta`](adapters/playwright/README.md).

## How it works

When a primary selector fails, fletta:

1. Extracts the element signature (path, attributes, text)
2. Clusters similar elements (Drain3)
3. Scores each candidate
4. Heals if confidence ≥ `minConfidence` (default: 0.85)
5. Reports the attempt with diff and top candidates

## v1.0.0 highlights

- Rust/WASM core (`fletta-core`, `healJson`)
- TypeScript SDK (`@fletta/sdk`)
- Playwright adapter — custom selector + `withFletta`, JUnit/JSON reports
- Debug Trace Mode (F012) — Classic + Explorer HTML reports
- Conference E2E gates CP001–CP005

## Documentation

| Topic | English | Русский |
|-------|---------|---------|
| Overview | [project/OVERVIEW.md](project/OVERVIEW.md) | [project/OVERVIEW.md](project/OVERVIEW.md) |
| Features & roadmap | [project/FEATURES.md](project/FEATURES.md) | — |
| PoC gates & benchmark | [docs/benchmark.md](docs/benchmark.md) | — |
| Positioning | [docs/positioning.md](docs/positioning.md) | [docs/positioning.md](docs/positioning.md) |
| Playwright adapter | [adapters/playwright/README.md](adapters/playwright/README.md) | [docs/integrations.md](docs/integrations.md) |
| TypeScript SDK | [sdk/typescript/README.md](sdk/typescript/README.md) | — |
| Architecture | [project/architecture/](project/architecture/) | — |
| Knowledge base index | [docs/README.md](docs/README.md) | [docs/README.md](docs/README.md) |

## Project structure

```
fletta/
├── crates/                 # Rust core (signature, clustering, healing, fletta-core)
├── sdk/typescript/         # TypeScript SDK + WASM bindings
├── adapters/playwright/    # Playwright integration
├── test-app/conference/    # FixtureConf demo pages
├── e2e/conference/         # PoC gates CP001–CP005 (CONF-*)
├── docs/                   # Positioning, benchmark, integrations
├── project/                # Features, architecture, cases
└── scripts/                # setup, build, test, dev
```

## Scripts

| Script | Description |
|--------|-------------|
| `./scripts/setup.sh` | Install dependencies |
| `./scripts/build.sh` | Build SDK, adapter, Rust core + WASM |
| `./scripts/start.sh [port]` | Start test server (default: 3000) |
| `./scripts/test.sh [conference\|debug\|all]` | Run E2E tests |
| `./scripts/stop.sh [port]` | Stop test server |
| `./scripts/dev.sh` | Dev mode with auto-rebuild |

## Roadmap

- **v1.0.0** — Core + Playwright adapter — released ([CHANGELOG.md](CHANGELOG.md))
- **v1.0.1** — Benchmark overhead gate (MVP-C)
- **v1.1.0** — Unified Context (F002) + RCA (F003)
- **v1.2.0** — MCP + Page Object Generator
- **v1.4.0** — Java SDK & UI adapters
- **v2.0.0** — Multi-platform (Android/iOS)

See [project/FEATURES.md](project/FEATURES.md) for details.

## License

Apache-2.0 — see [LICENSE](LICENSE) and [NOTICE](NOTICE).
