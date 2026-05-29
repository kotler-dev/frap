# frap Release Index

Single source of truth for current releases across all registries and surfaces.

**Audience:** Release managers, CI maintainers, package consumers.

## Current Releases

| Surface | Registry | Coordinates | Version | Status | Matrix |
|---------|----------|-------------|---------|--------|--------|
| **Rust Core** | repo / embedded | `crates/*`, `frap-core-rpc` | **0.1.0** | internal | [core/rust-0.1.0.md](./core/rust-0.1.0.md) |
| **TypeScript SDK** | npm | `@frap/sdk` | **1.1.1** | published | [typescript/npm-1.1.1-matrix.md](./typescript/npm-1.1.1-matrix.md) |
| **Playwright (TS)** | npm | `@frap/playwright` | **1.1.1** | published | ↑ |
| **Java Core** | Maven Central | `io.github.kotler-dev:frap-core-java` | **1.0.0** | ready (awaiting publish) | [java/java-sdk-1.0.0-matrix.md](./java/java-sdk-1.0.0-matrix.md) |
| **Playwright (Java)** | Maven Central | `io.github.kotler-dev:frap-playwright` | **1.0.0** | ready (awaiting publish) | ↑ |

## Capability Parity

| Capability | npm `@frap/sdk` 1.1.1 | Maven `frap-core-java` 1.0.0 |
|------------|----------------------|--------------------------------|
| Self-healing (`heal`) | ✅ | ✅ |
| Unified Context (`captureAll`) | ✅ | ✅ |
| RCA (`analyzeRca`) | ✅ | ✅ |
| Discover + clustering | ❌ | ✅ |
| Page Object generation | ❌ | ✅ |
| Reports (jsonl, debug, explorer) | ✅ | ✅ |

**Note:** npm 1.1.1 aligns with product release **v1.1.x** (context + RCA). Java 1.0.0 is a **self-contained surface** that bundles capabilities from product releases v1.0.0–v1.2.0 (includes discover/PO gen ahead of npm).

## Artifact Naming Cheat Sheet

```
Brand / CLI:     frap
Site:            https://github.com/kotler-dev/frap

npm SDK:         @frap/sdk
npm Playwright:  @frap/playwright

Maven groupId:   io.github.kotler-dev
Maven artifacts: frap-core-java, frap-playwright
Java packages:   io.github.kotlerdev.frap.*

Core (Rust):     0.1.0 (workspace, not on crates.io)
RPC binary:      frap-core-rpc (bundled in Java JAR, WASM in npm)
```

## Links

| Document | Purpose |
|----------|---------|
| [RELEASE-POLICY.md](./RELEASE-POLICY.md) | Versioning policy, CI tags, per-registry semver rules |
| [CHANGELOG.md](../../CHANGELOG.md) | Product-level changelog (npm-centric) |
| [project/FEATURES.md](../FEATURES.md) | Feature roadmap by product release (v1.0.0, v1.1.0…) |
| [sdk/java/VERIFICATION.md](../../sdk/java/VERIFICATION.md) | Java release verification commands |
| [java/AUDIT-java-1.0.0.md](./java/AUDIT-java-1.0.0.md) | Pre-release audit report (2026-05-30) |
| `./scripts/run-java-e2e.sh` | Java E2E test runner |

## Verification Commands

```bash
# npm
npm view @frap/sdk version
npm view @frap/playwright version

# Maven (after publish, 10-30 min delay)
curl "https://repo1.maven.org/maven2/io/github/kotler-dev/frap-core-java/1.0.0/frap-core-java-1.0.0.pom"
curl "https://repo1.maven.org/maven2/io/github/kotler-dev/frap-playwright/1.0.0/frap-playwright-1.0.0.pom"
```

---

*Last updated: 2026-05-29*
