# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-05-23

### Added

- Rust workspace: `signature`, `clustering`, `healing`, `fletta-core` with WASM `healJson`
- TypeScript SDK (`@fletta/sdk`) wired to WASM with dev fallback
- Playwright adapter: `withFletta`, custom selector engine, healing events
- JUnit / JSON reporting (CP005) and Conference E2E gates (CP001–CP005)
- Debug Trace Mode (F012): Classic + Explorer HTML reports, healing timeline

### Documentation

- Feature cards F000, F001, F008, F012, F013; `project/FEATURES.md` MVP 100%

## [1.1.1] - 2026-05-24

### Added

- F002: Unified Context — `fletta-context.json`, C002–C004 E2E gates
- F003: RCA via WASM (`wasm-node` target for Node), `generate-rca.mjs` post-run
- npm publish workflow; first public `@fletta/sdk` and `@fletta/playwright` on registry.npmjs.org

## [1.1.0] - 2026-05-24

### Added

- `fletta-context` Rust crate: `Timeline`, `Event`, correlation, window API
- Playwright context capture: `attachFlettaContext`, `captureAll` config
- C002/C003 fixtures: `test-app/context/`, `e2e/context/`, `./scripts/test.sh context`

## [1.0.1] - Unreleased

### Planned

- MVP-C: benchmark overhead < 10% vs baseline ([docs/benchmark.md](docs/benchmark.md))
