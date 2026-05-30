# Frap live demo (Java + reports)

Short walkthrough for presentations: what the library does, how to see healing and “clusters” in the report.

## Two slides

**Slide 1 — idea.** The test remembers the element (signature: DOM path, `data-testid`, text). After a markup refactor the old selector breaks. Frap snapshots interactive nodes, compares signatures, ranks candidates, and picks a **new stable selector** — the test passes without a manual fix.

**Slide 2 — clustering (for humans).** This is not a “map of the whole page” but **grouping similar candidates** by structural prefix (`button:submit > div:- > …`). In `frap-debug.html` you see steps → **DOM clusters** → top candidates with confidence → heal / reject outcome.

## Healing ≠ clustering

| | Self-healing | “Clusters” in the report |
|---|--------------|---------------------------|
| What it does | Finds an element similar to the recorded one and substitutes a selector when confidence is high | Groups **ranked candidates** by `signature.prefix` — explains the decision |
| Where | Same Rust `heal()` for Java and TS | Built after heal (`buildClusterViews`) |

Full clustering details: see Rust core in `crates/clustering/` and conference E2E reports.

## Two scenarios (show as a pair)

| Goal | Page / test | What you see |
|------|-------------|--------------|
| Smart PASS | `schedule-heal.html` — test uses `talk-open-healing`, page has `talk-card-open-healing` | `ScheduleHealingTest` → heal OK, new selector |
| Smart reject | CFP — two “Submit” buttons, broken `cfp-submit-missing` | `CfpAmbiguousHealTest` → `healed == false`, ≥2 candidates |

Cases: **CONF-SH-SCHED-PASS**, **CONF-SH-CFP-FAIL** — see `e2e/conference/` and `./scripts/run-java-e2e.sh`.

## Java run

```bash
./scripts/run-java-e2e.sh
```

Open the report:

```bash
open examples/java/playwright/target/frap-reports/conference/frap-debug.html
```

In HTML: **DOM clusters**, **Top candidates**, **Healing decision**.

One-liner: *“We don’t guess locators across the whole page — we find what looks like what we recorded and pick the best score.”*

## TypeScript (same engine, same pages)

From repo root:

```bash
node fixtures/fixtureconf/server.js &
./scripts/test.sh conference-dbg
open e2e/frap-reports/conference/frap-debug.html
```

## What not to promise

| Don’t say | Say instead |
|-----------|-------------|
| “Open the site — get all locators” | The test defines locators; Frap **fixes** them when they break |
| “Clustering = Page Object generation” | Report clusters **explain** the pick |
| “Frap always heals” | Two equally good candidates → **reject** (ambiguous) |

## 5-minute pitch

1. Browser: `http://localhost:3000/conference/schedule-heal.html` — what changed in markup.
2. `frap-debug.html` — how the engine decided (schedule, then CFP).
3. Green / red test in the IDE.

## One sentence

> Report clustering groups similar elements so we don’t click the wrong button. Self-healing finds what matches what we recorded and substitutes a selector when confidence is high. Show **schedule-heal (PASS)** + **CFP (FAIL)** with **`frap-debug.html`**.
