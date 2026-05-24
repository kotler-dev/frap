# fletta

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![npm @fletta/sdk](https://img.shields.io/npm/v/@fletta/sdk?label=%40fletta%2Fsdk)](https://www.npmjs.com/package/@fletta/sdk)
[![npm @fletta/playwright](https://img.shields.io/npm/v/@fletta/playwright?label=%40fletta%2Fplaywright)](https://www.npmjs.com/package/@fletta/playwright)

> Deterministic engine for UI structure extraction: stable identifiers, self-healing selectors, explainable reports. No ML in core, on-prem first, AI-ready via MCP.

**fletta** parses element trees (DOM, ViewTree, accessibility), clusters components with deterministic algorithms, and generates stable locators for Page Objects and tests. When a selector breaks, it heals by signature matching with confidence scores and diff reports.

## No ML in core, AI-ready

Three layers — not one product category:

| Layer | What | ML in fletta? |
|-------|------|----------------|
| **Core** (OSS) | Signatures, Drain3 clustering, self-healing, WASM | **No** — same input → same output |
| **Integration** (roadmap) | MCP tools: `discover`, `resolve`, `analyze` | **No** — JSON-RPC wire to agents; LLM runs outside |
| **Enhancements** (optional) | Semantic naming, step generation | **Optional** — separate package, BYO API key |

**fletta is a grounding layer, not an AI testing tool.** It does not orchestrate LLMs or generate tests from prompts. An agent (Cursor, Claude, your stack) calls fletta for structured element maps, stable execution, and explainable RCA — deterministic infrastructure the model can rely on.

Details: [docs/positioning.md](docs/positioning.md) · [docs/monetization.md](docs/monetization.md)

## Quick start (npm)

Published packages: [@fletta/sdk](https://www.npmjs.com/package/@fletta/sdk) · [@fletta/playwright](https://www.npmjs.com/package/@fletta/playwright)

```bash
npm install @fletta/playwright @fletta/sdk
```

```typescript
// playwright.config.ts
import { defineConfig } from '@playwright/test';
import { flettaPlaywright, registerFlettaSelector } from '@fletta/playwright';

export default defineConfig({
  ...flettaPlaywright({
    minConfidence: 0.85,
    reportDir: './fletta-reports',
    captureAll: true, // optional: unified context timeline (F002)
  }),
  use: {
    async setup({ selectors }) {
      await registerFlettaSelector(selectors);
    },
  },
});
```

```typescript
// test.spec.ts — custom selector or withFletta() wrapper
await page.locator('fletta:[data-testid="pay-btn"]').click();
```

See [adapters/playwright/README.md](adapters/playwright/README.md) and [sdk/typescript/README.md](sdk/typescript/README.md).

## Quick start (from source)

For Conference / Context E2E demos and core development:

```bash
./scripts/setup.sh
./scripts/build.sh
./scripts/start.sh          # test server on http://localhost:3000

./scripts/test.sh conference  # FixtureConf gates (CONF-*)
./scripts/test.sh context     # Context layer C002–C004

./scripts/stop.sh
```

Release verification (Rust + E2E + lint) runs on git tags `v*` in CI. See [docs/publishing-npm.md](docs/publishing-npm.md) and [docs/benchmark.md](docs/benchmark.md).

## Playwright integration

**Custom selector engine** (recommended) — see Quick start above.

**Wrapper API** — wrap an existing locator with [`withFletta`](adapters/playwright/README.md).

**Unified context** — `captureAll: true` writes `fletta-context.json` (network, console, UI); RCA report via `fletta-rca.json`. Demo: `./scripts/test.sh context`.

## How it works

When a primary selector fails, fletta:

1. Extracts the element signature (path, attributes, text)
2. Clusters similar elements (Drain3)
3. Scores each candidate
4. Heals if confidence ≥ `minConfidence` (default: 0.85)
5. Reports the attempt with diff and top candidates

## Release highlights

**v1.1.1** (npm)

- Unified Context (F002): `fletta-context.json`, C002–C004 E2E
- RCA (F003): `fletta-rca.json`, WASM + `generate-rca.mjs`
- Public packages `@fletta/sdk` and `@fletta/playwright` on [npm](https://www.npmjs.com/settings/fletta/packages)

**v1.0.0**

- Rust/WASM core (`fletta-core`, `healJson`)
- Playwright adapter — custom selector + `withFletta`, JUnit/JSON reports
- Debug Trace Mode (F012) — Classic + Explorer HTML reports
- Conference E2E gates CP001–CP005

## Documentation

| Topic | English | Русский |
|-------|---------|---------|
| Overview | [project/OVERVIEW.md](project/OVERVIEW.md) | [project/OVERVIEW.md](project/OVERVIEW.md) |
| Features & roadmap | [project/FEATURES.md](project/FEATURES.md) | — |
| PoC gates & benchmark | [docs/benchmark.md](docs/benchmark.md) | — |
| npm publishing | [docs/publishing-npm.md](docs/publishing-npm.md) | — |
| Positioning | [docs/positioning.md](docs/positioning.md) | [docs/positioning.md](docs/positioning.md) |
| Playwright adapter | [adapters/playwright/README.md](adapters/playwright/README.md) | [docs/integrations.md](docs/integrations.md) |
| TypeScript SDK | [sdk/typescript/README.md](sdk/typescript/README.md) | — |
| Context E2E | [e2e/context/README.md](e2e/context/README.md) | — |
| Architecture | [project/architecture/](project/architecture/) | — |
| Knowledge base index | [docs/README.md](docs/README.md) | [docs/README.md](docs/README.md) |

## Project structure

```
fletta/
├── crates/                 # Rust core (signature, clustering, healing, context, rca)
├── sdk/typescript/         # TypeScript SDK + WASM bindings
├── adapters/playwright/    # Playwright integration
├── test-app/conference/    # FixtureConf demo pages
├── test-app/context/       # Context layer demo pages
├── e2e/conference/         # PoC gates CP001–CP005 (CONF-*)
├── e2e/context/            # Context gates C002–C004
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
| `./scripts/test.sh conference` | Conference E2E (CONF-*) |
| `./scripts/test.sh context` | Context layer E2E (C002–C004) |
| `./scripts/test.sh [debug\|all]` | Other E2E targets |
| `./scripts/bench-context.sh` | Context capture overhead gate |
| `./scripts/stop.sh [port]` | Stop test server |
| `./scripts/dev.sh` | Dev mode with auto-rebuild |

## Roadmap

- **v1.1.1** — Context + RCA + npm packages — released ([CHANGELOG.md](CHANGELOG.md))
- **v1.0.1** — Benchmark overhead gate (MVP-C)
- **v1.2.0** — MCP + Page Object Generator
- **v1.4.0** — Java SDK & UI adapters
- **v2.0.0** — Multi-platform (Android/iOS)

See [project/FEATURES.md](project/FEATURES.md) for details.

## License

Apache-2.0 — see [LICENSE](LICENSE) and [NOTICE](NOTICE).
