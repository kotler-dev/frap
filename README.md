# Frap

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

> Deterministic library for working with locators in test automation.
> *Stable locators when the interface changes.*

Frap helps tests survive UI changes: finds elements on a page, remembers their structural fingerprint, and picks a new selector when needed — with a report and a **confidence score** (0.0–1.0: how sure Frap is that the found element is the same one).

**Full locator path:** page map → element fingerprint → selector recovery → failure cause analysis → interface change tracking across runs *(v1.2.0 — feedback loop, F009; v2.0+ — structural drift and CI gate, F017)*.

---

## What is Frap

**What Frap does:**

- **Find** — builds a map of interactive elements with recommended selectors in a single pass, without manual DOM inspection
- **Hold** — when markup changes, restores the link between selector and element by structure, instead of failing on the first diff
- **Explain** — shows confidence, alternative candidates, and why a test failed — not a black-box "fix"

Frap parses element trees (**DOM** — today; **accessibility tree** — v1.4+ via CDP, v2.0+ multi-platform, F006), groups similar blocks with deterministic algorithms, and generates stable locators for tests.

---

## Problem

The interface changes faster than tests are updated.

**Pain points:**

- Hours of manual DOM inspection on new pages
- Frontend refactoring causes massive selector failures in CI
- Unclear why auto-recovery chose a new element
- Opaque cloud tools often fail enterprise security requirements

**What automators expect:**

- Take a snapshot of a page and get all available locators
- Group elements by structure and business zones
- Quickly start PageObject without manual routine
- Reduce flaky failures via deterministic logic

---

## Positioning

Frap is a structural and stable identifier layer, not a replacement for your test tools.

**Principle: Integration, not replacement.**

```
Playwright (today) / Selenium·WebDriver (v1.4+) / JUnit
        ↓
TypeScript SDK / Java SDK / JSON-RPC
        ↓
Rust Core: fingerprints · clustering · recovery · context
```

Frap wraps your existing locators and adds structural resilience.

---

## Locator Lifecycle

One clear lifecycle for every locator:

1. **Page map** — collect element list: tags, attributes, path, role, text
2. **Element fingerprint** — capture stable traits and structural prefix (signature)
3. **Selector recovery** — if the element is not found, find the best candidate by confidence and selection rules
4. **Report** — what changed, which selector was picked, confidence score, top candidates

---

## Core Features

### Page Map (Element Map)

In a single pass, Frap builds a **page map** — a flat catalog of elements with a recommended selector and confidence score. Not a DOM tree, not `querySelector('*')`. This catalog is the foundation for PageObjects and baselines.

API: `Frap.discover`, `buildElementMap`.

### Clustering

Elements are grouped by structural template:

- **SINGLE** — unique element (button, field)
- **LIST** — two or more similar items (repeating cards, rows)

Five identical cards → one `items(index)` method, not five copies. Clustering removes noise and narrows the search during selector recovery.

### Locator Recovery

When a selector stops finding an element (changed `data-testid`, class, wrapper), Frap **recovers the locator** — picks a new selector for the same element:

1. Compare saved fingerprint against the current page
2. Rank candidates by confidence score
3. Decide: recover if confidence ≥ threshold, or fail safely with a report (e.g. two similar candidates — refuse, to avoid clicking the wrong element)

Behavior is deterministic: same input → same output.

API: `withFrap`, `heal`, `HealResult`.

### Unified Run Context & Failure Cause Analysis

**Unified run context** — a single timeline from three sources: UI actions (clicks, locator failures), network (HTTP, timeouts), logs (console, page errors).

**Failure cause analysis** — after a test fails, Frap inspects this context and answers: UI issue, network issue, timing issue, or unclear. This is **not** picking a new selector. Example: API timeout → don't "fix" the selector, report a network cause.

API: `captureAll`, `ContextTimeline`; `analyzeRca` → returns `RcaReport`.

### Page Object Generation

Workflow from zero to first tests:

- Page map → filter interactive elements → group by clusters → read recommended selectors → generate Page Object skeleton → evolve with baseline fingerprints; UI change tracking across runs — **v1.2.0** (F009 feedback), **v2.0+** (F017 drift-report)

---

## Principles

- **Explainable by design** — every decision shows its work
- **No ML by default** — deterministic algorithms, same input → same output
- **On-prem first** — works without cloud APIs
- **Integration, not replacement** — complements Playwright, Selenium, and JUnit

---

## Architecture

**Unified Rust Core**

- Single source of truth for fingerprints, clustering, and locator recovery
- Consistent behavior across Java, TypeScript, and future SDKs
- Performance and determinism without divergence

**Thin SDK Adapters**

- JSON-RPC contract and DTO schema for all transports
- Contract tests for every SDK
- API convenience without duplicating logic

You own your fingerprints and can reproduce behavior across stacks.

---

## API Summary

Names in the table are the target Core/RPC contract; concrete SDKs may differ (e.g. `HealingEngine.heal()` in TypeScript).

| Action | API | Purpose |
|--------|-----|---------|
| Page map | `Frap.discover`, `buildElementMap` | Snapshot → element catalog |
| Filter | `filterElementMap`, `FilterSpec` | Interactive only, min cluster size |
| PO generation | `generatePageObject`, `GenerateOptions` | Page Object skeleton |
| Locator recovery | `withFrap`, `heal` → `HealResult` | Pick a new selector |
| Unified run context | `captureAll`, `ContextTimeline` | UI + network + logs |
| Failure cause analysis | `analyzeRca` → `RcaReport` | Classify: UI / network / timing |

---

## Packages

| Registry | SDK / Core | Playwright |
|----------|------------|------------|
| npm | `@frap/sdk` | `@frap/playwright` |
| Maven | `io.github.kotler-dev:frap-core-java` | `io.github.kotler-dev:frap-playwright` |

Note: npm requires scoped install `@frap/...`; Maven uses `io.github.kotler-dev` groupId.

---

## Documentation

| Topic | Link |
|-------|------|
| Examples (by language) | [examples/](examples/) |
| FixtureConf demo app | [fixtures/fixtureconf/](fixtures/fixtureconf/) |
| Java SDK | [sdk/java/README.md](sdk/java/README.md) |
| TypeScript SDK | [sdk/typescript/README.md](sdk/typescript/README.md) |
| Java example (Playwright) | [examples/java/playwright/](examples/java/playwright/) |
| TypeScript example (Playwright) | [examples/typescript/playwright/](examples/typescript/playwright/) |
| Playwright adapter (TS) | [adapters/playwright/README.md](adapters/playwright/README.md) |
| Playwright adapter (Java) | [adapters/playwright-java/README.md](adapters/playwright-java/README.md) |
| Rust core | [crates/core/README.md](crates/core/README.md) |

---

## Roadmap

- **1.0.0** — core + Java SDK: page map, locator recovery, reports, context and RCA, PO generation (Maven Central)
- **1.2.0** — feedback loop (F009), MCP, npm parity for discover/PO
- **1.4+** — CDP source (incl. web accessibility tree), WebDriver/Selenide (F014)
- **2.0+** — structural contracts and drift gate (F017), visual fingerprints (F007), health score (F010), multi-platform and accessibility tree (F006)

---

## Let's frap

[Examples](examples/) · [Java SDK](sdk/java/README.md) · [TypeScript SDK](sdk/typescript/README.md)

---

## License

Apache-2.0 — see [LICENSE](LICENSE) and [NOTICE](NOTICE).
