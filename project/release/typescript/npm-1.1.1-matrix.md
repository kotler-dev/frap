# npm 1.1.1 — Capability Matrix

**Goal:** Single source of truth for npm consumers — what is shipped in `@frap/sdk` and `@frap/playwright` 1.1.1.

**Audience:** TypeScript/Node automation engineers; evaluators comparing against Java/Maven releases.

**Rule:** Capability is "shipped" only when code + tests + user-facing docs + demo example all exist. Partial coverage = `doc-gap` or `code-only`.

---

## Matrix: Capability × Surface

| Capability | Core WASM | `@frap/sdk` | `@frap/playwright` | Tests | User Docs | Demo | Status |
|------------|-----------|-------------|-------------------|-------|-----------|------|--------|
| **Self-healing (`heal`)** | ✅ `healJson` | ✅ `HealingEngine.heal()` | ✅ Custom selector `frap:`, `withFrap()` | CP001–CP005 | [quickstart.md](../../../docs/en/quickstart.md) | Conference E2E | **shipped** |
| **Unified Context (`captureAll`)** | ✅ Event structs | ✅ Types, capture API | ✅ `attachFrapContext()`, `FrapContext` | C002–C004 | [quickstart.md](../../../docs/en/quickstart.md) | Context E2E | **shipped** |
| **RCA (`analyzeRca`)** | ✅ WASM | ✅ Types, helpers | ✅ Post-run `generate-rca.mjs` | C002–C004 | [sdk/typescript/README.md](../../../sdk/typescript/README.md) | — | **shipped** |
| **Reports (jsonl, debug, explorer)** | — | ✅ DTOs, writers | ✅ Reporter integration, HTML | CP005 | [quickstart.md](../../../docs/en/quickstart.md) | Conference E2E | **shipped** |
| **Discover + clustering** | ✅ `build_element_map` | ❌ Not exposed | ❌ Not exposed | — | — | — | **out-of-scope** |
| **Page Object generation** | ✅ `generate_page_object` | ❌ Not exposed | ❌ Not exposed | — | — | — | **out-of-scope** |

---

## Status Definitions

| Status | Meaning |
|--------|---------|
| **shipped** | Code + tests + user docs all present. Ready for consumers. |
| **doc-gap** | Code and tests exist; user-facing docs incomplete. |
| **code-only** | Implemented but not yet exposed to users. |
| **out-of-scope** | Explicitly not in this release; may be in roadmap. |

---

## Out of Scope for 1.1.1

| Capability | Reason | Target |
|------------|--------|--------|
| Discover + clustering | CLI-only workflow; no SDK surface planned | v1.2.0 or backlog |
| Page Object generation | Requires CLI + template engine; no TS target yet | v1.2.0 or backlog |
| MCP integration | Protocol layer, not SDK feature | v1.2.0 |

---

## Capability Parity with Java 1.0.0

| Feature | npm 1.1.1 | Java 1.0.0 |
|---------|-----------|------------|
| Self-healing | ✅ | ✅ |
| Context + RCA | ✅ | ✅ |
| Discover/PO gen | ❌ | ✅ |

Java 1.0.0 is a **broader surface** that includes discover and Page Object generation (product v1.2.0 features). npm will catch up in a future release.

---

## Links

- npm package: https://www.npmjs.com/package/@frap/sdk
- Playwright adapter: https://www.npmjs.com/package/@frap/playwright
- Source: [sdk/typescript](../../../sdk/typescript/), [adapters/playwright](../../../adapters/playwright/)
- Product roadmap: [project/FEATURES.md](../FEATURES.md)

---

*Last updated: 2026-05-29*
