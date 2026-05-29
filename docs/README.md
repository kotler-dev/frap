# Frap Documentation

Public knowledge base: positioning, audience, integrations, indexes.

**Engineering SSOT** lives in [`project/`](../project/) — see [project/README.md](../project/README.md).

## docs/ vs project/

| Question | Read |
|----------|------|
| Why frap? pitch, pains, audience | `docs/` (this folder) |
| Feature **status** shipped or not | [project/FEATURES.md](../project/FEATURES.md) |
| Feature ↔ E2E ↔ fixtures | [project/traceability.md](../project/traceability.md) |
| Full feature / case specs | [project/feature/](../project/feature/), [project/cases/](../project/cases/) |
| Released npm/Maven versions | [project/release/](../project/release/) |

## Structure

```
docs/
├── README.md           # Этот файл
├── index.md            # Связующий индекс (меню, маппинги)
├── features.md         # Index → project/feature/ (not SSOT)
├── cases.md            # Index → project/cases/ (not SSOT)
├── positioning.md      # Позиционирование, one-liner
├── benchmark.md        # PoC/MVP gates, CP001–CP005
├── integrations.md     # Playwright, JUnit
├── roadmap.md          # Narrative roadmap (status → FEATURES.md)
└── …
```

## С чего начать

| Задача | Документ |
|--------|----------|
| Питч, конкуренты | [positioning.md](./positioning.md) |
| PoC gates | [benchmark.md](./benchmark.md) |
| Карта фич (index) | [features.md](./features.md) → [project/FEATURES.md](../project/FEATURES.md) |
| Карта кейсов (index) | [cases.md](./cases.md) → [project/cases/](../project/cases/) |
| Java SDK | [en/java-getting-started.md](./en/java-getting-started.md) |

## Добавить фичу или кейс

1. Card/spec in `project/feature/` or `project/cases/`
2. Row in [project/FEATURES.md](../project/FEATURES.md) or [project/cases/README.md](../project/cases/README.md)
3. [project/traceability.md](../project/traceability.md) if E2E exists
4. Index line in [features.md](./features.md) or [cases.md](./cases.md)

## For AI agents

1. [CONTEXT.md](../CONTEXT.md) — repo map
2. [project/README.md](../project/README.md) — planning layout
3. Product copy → `docs/` · implementation truth → `project/`
