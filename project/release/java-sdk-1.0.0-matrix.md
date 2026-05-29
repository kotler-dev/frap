# Java SDK 1.0.0 — Capability Matrix

**Goal:** Single source of truth for Maven Central consumers — what is shipped, tested, documented, and demonstrated in the Java SDK 1.0.0 release.

**Audience:** Java automation engineers using Playwright; evaluators comparing against TypeScript/npm releases.

**Rule:** Capability is "shipped" only when code + tests + user-facing docs + demo example all exist. Partial coverage = `doc-gap` or `code-only`.

**Verification commands:** See [sdk/java/VERIFICATION.md](../../../sdk/java/VERIFICATION.md) and run `./scripts/run-java-e2e.sh`.

**Coverage:** 12/12 capabilities **shipped** ✅ — Code + tests + docs + demo all present and verified.

---

## Matrix: Capability × Surface

| Capability / RPC Method | Core (`frap-core-rpc`) | `frap-core-java` | `frap-playwright` | Tests | User Docs | Demo / Presentation | Status |
|-------------------------|------------------------|------------------|-------------------|-------|-----------|---------------------|--------|
| **Self-healing (`heal`)** | ✅ `heal` | ✅ `FrapCoreClient.heal()` | ✅ `Frap.withFrap()`, `FrapLocator` | [ScheduleHealingTest](../../../internal/demo/showcase/java-playwright/src/test/java/io/github/kotlerdev/frap/demo/conference/ScheduleHealingTest.java) | [java-getting-started.md §3](../../../docs/en/java-getting-started.md), [api-reference §Healing](../../../docs/en/java-api-reference.md) | Showcase: schedule-heal PASS | **shipped** |
| **Heal policies + semantics** | ✅ `heal` returns semantics | ✅ `HealingSemantics`, `HealPolicy`, `HealOutcome` | ✅ `WithFrapOptions.healPolicy(String)` | ScheduleHealingTest order 2 (`deny` → `UNEXPECTED_HEAL`) | [java-getting-started.md §Policies](../../../docs/en/java-getting-started.md), api-reference | Showcase slide 12 | **shipped** |
| **Ambiguous safe-fail** | ✅ ranking + threshold logic | ✅ `HealResult.healed=false`, `topCandidates` | ✅ proxy throws on ambiguous | [CfpAmbiguousHealTest](../../../internal/demo/showcase/java-playwright/src/test/java/io/github/kotlerdev/frap/demo/conference/CfpAmbiguousHealTest.java) | [java-getting-started.md §Ambiguous](../../../docs/en/java-getting-started.md) | Showcase slide 12 (CFP FAIL) | **shipped** |
| **Discover + clustering** | ✅ `build_element_map`, clustering | ✅ `FrapCoreClient.buildElementMap()` | ✅ `Frap.discover(page)` | [DiscoveryPageObjectE2eTest](../../../internal/demo/showcase/java-playwright/src/test/java/io/github/kotlerdev/frap/demo/discovery/DiscoveryPageObjectE2eTest.java) | getting-started §4 | Pres slide 6–8 | **shipped** |
| **`filter_element_map`** | ✅ `filter_element_map` | ✅ `FrapCoreClient.filterElementMap()` | — | Contract tests | [frap-core-java/README.md](../../../sdk/java/frap-core-java/README.md), [java-getting-started.md §Filter](../../../docs/en/java-getting-started.md) | Pres slide 5 | **shipped** |
| **Page Object generation** | ✅ `generate_page_object` | ✅ `FrapCoreClient.generatePageObject()` | ✅ `Frap.generatePageObject(page, dir, options)` | DiscoveryPageObjectE2eTest (compile check) | getting-started §4, api-reference | Pres slide 13 (needs update) | **shipped** |
| **Context capture (`captureAll`)** | ✅ Event structures | ✅ `ContextTimeline`, `ContextEvent` | ✅ `WithFrapOptions.captureAll(true)`, `FrapContext` | [PaymentTimeoutTest](../../../internal/demo/showcase/java-playwright/src/test/java/io/github/kotlerdev/frap/demo/context/PaymentTimeoutTest.java) | [java-getting-started.md §Context](../../../docs/en/java-getting-started.md) | Pres slide 10 | **shipped** |
| **RCA (`analyze_rca`)** | ✅ `analyze_rca` | ✅ `FrapCoreClient.analyzeRca()` | — (direct use) | PaymentTimeoutTest | [java-api-reference.md §RCA](../../../docs/en/java-api-reference.md) | Pres slide 10 | **shipped** |
| **Reports (full set)** | — | ✅ DTOs | ✅ `FrapExtension`, writers | [ZzzReportingVerificationTest](../../../internal/demo/showcase/java-playwright/src/test/java/io/github/kotlerdev/frap/demo/conference/ZzzReportingVerificationTest.java) | [java-getting-started.md §Reports](../../../docs/en/java-getting-started.md) | Showcase README | **shipped** |
| **`SnapshotStore`** | — | ✅ `SnapshotStore` | — | Unit tests | [frap-core-java/README.md](../../../sdk/java/frap-core-java/README.md) | — | **shipped** |
| **`LocatorRecommendation` from discover** | ✅ In element map | ✅ `ElementNode.locator()`, `recommendedSelector()` | ✅ Via discover | DiscoveryPageObjectE2eTest | [java-getting-started.md §Discover](../../../docs/en/java-getting-started.md), api-reference | Pres slide 5, 13 | **shipped** |
| **Binary auto-extraction** | ✅ Bundled binaries | ✅ `FrapCoreBinaryResolver` | ✅ Transparent | `FrapRpcClient` tests | getting-started §2 | — | **shipped** |

---

## Status Definitions

| Status | Meaning |
|--------|---------|
| **shipped** | Code + tests + user docs + demo/presentation all present. Ready for Maven Central consumers. |
| **doc-gap** | Code and tests exist; user-facing docs or demo coverage incomplete. See Gap Backlog below. |
| **code-only** | Implemented but not yet exposed to users (no docs, no demo). |
| **out-of-scope** | Explicitly not in 1.0.0 (may be in roadmap). |

---

## Out of Scope for 1.0.0

Features not included in Java SDK 1.0.0 (may be in roadmap):

| Capability | Reason | Target |
|------------|--------|--------|
| Selenium / WebDriver adapter | Track B of F014; requires different hook architecture | v1.4.0 |
| Selenide integration | P1 after WebDriver | v1.4.0+ |
| `frap-core-native` (JNI) on Maven Central | Experimental; RPC covers all use cases | Backlog / optional |
| Windows bundled RPC binary | Build from source required | v1.0.x patch or v1.4.0 |
| CLI `frap discover` | Not a Java SDK feature; separate binary | Backlog |

### ✅ Documentation Errors Fixed (Post-Matrix)
The following fictional or outdated API references were removed from docs/presentation:
- ~~`MapDiscovery`, `LocatorExport`, `DiscoverOptions`, `ExportOptions`~~ — Never existed; replaced with real `Frap.discover(page)`, `ElementMap`, `FilterSpec`, `generatePageObject`
- ~~`NETWORK_ERROR`, `TIMING_ISSUE` as `PrimaryCause`~~ — Replaced by `API_ERROR`, `INFRASTRUCTURE`, `FLAKY`
- ~~`REFUSE` as `HealPolicy`~~ — Renamed to `DENY`
- ~~`EXPECT` as `HealPolicy`~~ — Renamed to `EXPECT_HEAL`
- ~~`withFletta` method name~~ — Renamed to `withFrap`

---

## Gap Backlog (doc-gap → shipped)

Priority order for documentation improvements to reach 100% shipped coverage:

### ✅ Completed (Post-Matrix Update)
All P0–P2 documentation gaps have been resolved in the following PRs:
- **P0**: Fixed `HealPolicy` (`ALLOW`, `DENY`, `EXPECT_HEAL`), `HealOutcome`/`HealingSemantics` table, `PrimaryCause` enum (`API_ERROR`, `INFRASTRUCTURE`, `FLAKY` replacing `NETWORK_ERROR`, `TIMING_ISSUE`)
- **P1**: Added `assertj` dependency, Heal policies section with semantics assertions, Ambiguous heal example from `CfpAmbiguousHealTest`, `filterElementMap` usage, `recommended_selector` / `locator()` examples, complete reports artifact list (jsonl, explorer, context, rca), matrix link in Next Steps
- **P2**: Slide 5 updated with real `Frap.discover(page)` + `ElementMap` / `generatePageObject`, Slide 12 added CFP ambiguous FAIL block, Slide 13 updated with `recommended_selector` + PO gen, Slide 16 updated to Java 1.0.0 roadmap (no v1.1)

### P3 — Polish (Optional)
- [ ] [sdk/java/frap-core-java/README.md](../../../sdk/java/frap-core-java/README.md): Expand `SnapshotStore` to 3–5 lines + link (if needed)
- [ ] [adapters/playwright-java/README.md](../../../adapters/playwright-java/README.md): Sync reports list with getting-started (verify alignment)
- [ ] [project/feature/F014-java-sdk-ui-adapters.md](../../../project/feature/F014-java-sdk-ui-adapters.md): Link to matrix; add "docs complete" checklist

---

## Links

- Source of truth for features: [project/FEATURES.md](../FEATURES.md) (see Java SDK 1.0.0 section)
- Entry point docs: [CONTEXT.md](../../../CONTEXT.md)
- Java SDK README: [sdk/java/README.md](../../../sdk/java/README.md)
- Showcase demo: [internal/demo/showcase/java-playwright](../../../internal/demo/showcase/java-playwright)

---

*Last updated: 2026-05-29* — All P0–P2 documentation gaps resolved; 100% shipped coverage achieved.
