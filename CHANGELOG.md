# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-05-23

### Added

- Rust workspace: `signature`, `clustering`, `healing`, `frap-core` with WASM `healJson`
- TypeScript SDK (`@frap/sdk`) wired to WASM with dev fallback
- Playwright adapter: `withFrap`, custom selector engine, healing events
- JUnit / JSON reporting (CP005) and Conference E2E gates (CP001–CP005)
- Debug Trace Mode (F012): Classic + Explorer HTML reports, healing timeline

### Documentation

- Feature cards F000, F001, F008, F012, F013; MVP scope complete

## [1.1.1] - 2026-05-24

### Added

- F002: Unified Context — `frap-context.json`, C002–C004 E2E gates
- F003: RCA via WASM (`wasm-node` target for Node), `generate-rca.mjs` post-run
- npm publish workflow; first public `@frap/sdk` and `@frap/playwright` on registry.npmjs.org

## [1.1.0] - 2026-05-24

### Added

- `frap-context` Rust crate: `Timeline`, `Event`, correlation, window API
- Playwright context capture: `attachFrapContext`, `captureAll` config
- C002/C003 fixtures: `fixtures/fixtureconf/context/`, `e2e/context/`, `./scripts/test.sh context`

## [Unreleased]

### Java SDK 1.2.0-SNAPSHOT

- Development branch: **`develop/java-v1.1.1`**
- Maven workspace: `1.2.0-SNAPSHOT`

### Planned

- MVP-C: benchmark overhead < 10% vs baseline

## [Java SDK 1.1.1] - 2026-06-04

### Added

- **`frap.runtime.dir`**: bundled `frap-core-rpc` extracts to `<jar-dir>/.frap/bin/` (not `/tmp`); default work dir `work/`, logs `logs/`
- **SnapshotBuilder parity** with MCP `snapshot.js`: `computed_role`, `scope_hint`, `visible` on live `Frap.discover`
- **`DOMElementInfo`** optional collector fields (backward compatible JSON)

### Changed

- MCP file-mode tool examples use `<frap.runtime.dir>/work/...` instead of `/tmp/frap`

### Documentation

- [java-sdk-1.1.1-matrix.md](../project/release/java/java-sdk-1.1.1-matrix.md)
- Validation: **C012** (CI dom-benchmark), **C013** (optional live explore)
- Tag: `java-v1.1.1` → `publish-maven.yml`

## [Java SDK 1.1.0] - 2026-06-03

### Added

- **Core semantic locator pipeline** (CLASSIFY→SCOPE→LOCATE→VERIFY→HEAL): improved PoG with `getByTestId`, `getByRole`, `getByText`, scoped locators; `LocatorRecommendation` fields `scope`, `filter_text`, `match_count`
- **dom-benchmark** ground truth aligned to semantic `locator.value` (pages 01–09)
- **frap-mcp** 1.1.0 on Maven Central (JDK 21 sidecar runners; library stack stays JDK 17)
- Six-platform bundled `frap-core-rpc` unchanged (Rust workspace 0.1.0)

### Changed

- CI: `java-v*` tags no longer trigger full `ci.yml` — Maven publish via `publish-maven.yml` only

### Documentation

- JDK 17 library / JDK 21 MCP sidecar deployment model in getting-started, API reference, sdk README
- Tag: `java-v1.1.0` from branch `release/java-v1.1.0` → workflow `publish-maven.yml`

## [Java SDK 1.0.1] - 2026-06-03

### Added

- Discover/Page Object quality (F019, issues #7–#15): `:has-text` locators, LIST accessors, semantic PoG names, `innerText` snapshots, dom-benchmark fixtures
- **frap-mcp** 1.0.1 on Maven Central: 6 MCP tools; runners `frap-mcp-stdio`, `frap-mcp-http`, `frap-mcp-http-local` (JDK 21)
- Six-platform bundled `frap-core-rpc` (Linux glibc/musl/aarch64, macOS x86_64/aarch64, Windows x64)
- `FrapRpcClient` platform matrix; `Cluster` DTO `member_tag` / `shared_selector`

### Documentation

- [docs/en/discover-page-object.md](./docs/en/discover-page-object.md), [sdk/java/frap-mcp/README.md](./sdk/java/frap-mcp/README.md)
- Tag: `java-v1.0.1` → workflow `publish-maven.yml`

## [Java SDK 1.0.0] - 2026-05-30

### Added

- Maven Central: `io.github.kotler-dev:frap-core-java`, `io.github.kotler-dev:frap-playwright`
- Java Playwright adapter (`adapters/playwright-java`): `withFrap`, healing proxy, `FrapExtension`, discovery, Page Object generation
- Java E2E demo (`examples/java/playwright`): conference healing, CP005 reports, C002 RCA; gate `./scripts/run-java-e2e.sh`
- CI job `java-playwright-e2e`; Maven profiles `java-unit` / `java-e2e` in `sdk/java/pom.xml`
- Bundled `frap-core-rpc` binaries (Linux glibc/musl, macOS aarch64)

### Fixed

- Maven `groupId` aligned with verified Central Portal namespace `io.github.kotler-dev`

### Documentation

- F014 Playwright track; Java SDK 1.0.0 capability matrix documented in `sdk/java/README.md`
