# frap

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![npm @frap/sdk](https://img.shields.io/npm/v/@frap/sdk?label=%40frap%2Fsdk)](https://www.npmjs.com/package/@frap/sdk)
[![npm @frap/playwright](https://img.shields.io/npm/v/@frap/playwright?label=%40frap%2Fplaywright)](https://www.npmjs.com/package/@frap/playwright)

> Deterministic engine for UI structure extraction: stable identifiers, self-healing selectors, explainable reports. No ML in core, on-prem first, AI-ready via MCP.

**frap** parses element trees (DOM, ViewTree, accessibility), clusters components with deterministic algorithms, and generates stable locators for Page Objects and tests. When a selector breaks, it heals by signature matching with confidence scores and diff reports.

## No ML in core, AI-ready

Three layers — not one product category:

| Layer | What | ML in frap? |
|-------|------|----------------|
| **Core** (OSS) | Signatures, Drain3 clustering, self-healing, WASM | **No** — same input → same output |
| **Integration** (roadmap) | MCP tools: `discover`, `resolve`, `analyze` | **No** — JSON-RPC wire to agents; LLM runs outside |
| **Enhancements** (optional) | Semantic naming, step generation | **Optional** — separate package, BYO API key |

**frap is a grounding layer, not an AI testing tool.** It does not orchestrate LLMs or generate tests from prompts. An agent (Cursor, Claude, your stack) calls frap for structured element maps, stable execution, and explainable RCA — deterministic infrastructure the model can rely on.

Details: [docs/positioning.md](docs/positioning.md) · [docs/monetization.md](docs/monetization.md)

## Quick start (npm)

Published packages: [@frap/sdk](https://www.npmjs.com/package/@frap/sdk) · [@frap/playwright](https://www.npmjs.com/package/@frap/playwright)

```bash
npm install @frap/playwright @frap/sdk
```

```typescript
// playwright.config.ts
import { defineConfig } from '@playwright/test';
import { frapPlaywright, registerFlettaSelector } from '@frap/playwright';

export default defineConfig({
  ...frapPlaywright({
    minConfidence: 0.85,
    reportDir: './frap-reports',
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
await page.locator('frap:[data-testid="pay-btn"]').click();
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

**Unified context** — `captureAll: true` writes `frap-context.json` (network, console, UI); RCA report via `frap-rca.json`. Demo: `./scripts/test.sh context`.

## How it works

When a primary selector fails, frap:

1. Extracts the element signature (path, attributes, text)
2. Clusters similar elements (Drain3)
3. Scores each candidate
4. Heals if confidence ≥ `minConfidence` (default: 0.85)
5. Reports the attempt with diff and top candidates

## Release highlights

**v1.1.1** (npm)

- Unified Context (F002): `frap-context.json`, C002–C004 E2E
- RCA (F003): `frap-rca.json`, WASM + `generate-rca.mjs`
- Public packages `@frap/sdk` and `@frap/playwright` on [npm](https://www.npmjs.com/settings/frap/packages)

**v1.0.0**

- Rust/WASM core (`frap-core`, `healJson`)
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
frap/
├── crates/                      # Rust core (signature, clustering, healing, context, rca)
├── sdk/                         # TypeScript + Java SDK
├── adapters/                    # Playwright integrations
├── internal/
│   ├── architecture/            # Architecture docs (internal)
│   ├── testing/conference/      # Conference E2E (CONF-*)
│   ├── demo/
│   │   ├── presentation/        # Slide deck
│   │   ├── site/                # Demo server + FixtureConf + context pages
│   │   └── showcase/java-playwright/  # Java E2E demo
│   └── project/                 # Features, cases (internal planning)
├── e2e/                         # Playwright runner + context specs
├── docs/                        # Public docs
├── project/                     # Public feature/case docs
└── scripts/
```

See [internal/README.md](internal/README.md) for the internal workspace map.

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
