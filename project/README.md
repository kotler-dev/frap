# Project planning

Engineering source of truth: feature status, cases, architecture, releases.

**Not here:** product pitch and market copy → [`docs/`](../docs/) · runnable code → [`examples/`](../examples/), [`e2e/`](../e2e/), [`fixtures/`](../fixtures/).

## Layout

| Path | Purpose |
|------|---------|
| [FEATURES.md](./FEATURES.md) | **SSOT** — feature statuses, release roll-up, statistics |
| [feature/](./feature/) | Feature cards `F000`–`F017` (use [_template.md](./feature/_template.md)) |
| [cases/](./cases/) | Scenario specs `C*`, `CONF-*` matrix |
| [traceability.md](./traceability.md) | Feature ↔ case ↔ E2E ↔ fixture ↔ gate |
| [architecture/](./architecture/) | Design docs + [ADR-001](./architecture/ADR-001-core-language-strategy.md) |
| [release/](./release/) | Version index, capability matrices, [RELEASE-POLICY](./release/RELEASE-POLICY.md) |
| [conventions.md](./conventions.md) | IDs, statuses, naming |
| [OVERVIEW.md](./OVERVIEW.md) | Short engineering intro (links to `docs/` for positioning) |

## docs/ vs project/

| Question | Read |
|----------|------|
| What is shipped? (code truth) | [FEATURES.md](./FEATURES.md), [traceability.md](./traceability.md) |
| Why frap? competitors? audience? | [docs/positioning.md](../docs/positioning.md), [docs/audience.md](../docs/audience.md) |
| PoC gates CP*, metrics | [docs/benchmark.md](../docs/benchmark.md) |
| Case catalog (public index) | [docs/cases.md](../docs/cases.md) → specs in [cases/](./cases/) |
| Feature catalog (public index) | [docs/features.md](../docs/features.md) → cards in [feature/](./feature/) |
| Product roadmap narrative | [docs/roadmap.md](../docs/roadmap.md) — **status SSOT: FEATURES.md only** |
| Published package versions | [release/README.md](./release/README.md) |

## Quick commands

```bash
./scripts/test.sh conference   # CONF-* + reports
./scripts/test.sh context        # C002–C004
./scripts/run-java-e2e.sh        # Java SDK gate
```

## For agents

1. Read [FEATURES.md](./FEATURES.md) before implementing a feature.
2. Read the matching card in [feature/](./feature/).
3. Check [traceability.md](./traceability.md) for E2E/fixture paths.
4. Update status in **FEATURES.md** when done — not in `docs/index.md` alone.
