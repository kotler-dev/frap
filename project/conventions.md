# Конвенции проекта frap

См. также [project/README.md](./README.md) — карта раздела `project/`.

## Именование

### Фичи
- ID: `F000`, `F001`, … `F017` (расширять по мере добавления)
- Файлы: `F001-self-healing.md`, `F002-unified-context.md`
- Карточка: YAML frontmatter + секции из [_template.md](./feature/_template.md)

### Кейсы
- PoC (legacy): `CP001`–`CP005` → реализация в `CONF-*`
- Conference E2E: `CONF-<FEAT>-<AREA>-<OUTCOME>` — матрица [cases/conference/CASES.md](./cases/conference/CASES.md)
- Scenario: `C001`–`C010`
- Файлы: `C002-api-timeout.md` (плоско в [cases/](./cases/))

### Релизы
- Product tags: `v1.0.0`, `v1.1.0`, … — см. [FEATURES.md](./FEATURES.md)
- Registry versions: npm `@frap/*`, Maven `1.0.0`, Rust core `0.1.0` — см. [release/README.md](./release/README.md)

## Статусы

### Фичи (карточка + FEATURES.md)

| Symbol / word | Meaning |
|---------------|---------|
| ✅ / `done` | Shipped per FEATURES rule |
| ⚠️ / `in-progress` | Partial |
| ❌ / `draft` | Not started or design only |
| ⏸️ / `frozen` | Paused |
| 🚫 / `cancelled` | Dropped |

**SSOT for release status:** [FEATURES.md](./FEATURES.md) only. `docs/roadmap.md` — narrative, not status.

### Кейсы

| Status | Meaning |
|--------|---------|
| `concept` | Spec only |
| `script-ready` | Manual script exists |
| `validated` | E2E + fixtures green |

## Git

### Коммиты
- Формат: `type(scope): message` (English)
- Types: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`

### Ветки
- `main` — stable
- `feature/F001-*` — feature work

## Код

### Rust
- `snake_case` functions, `PascalCase` types
- No `unwrap()` in production paths
- Healing/scoring/clustering **only in Core** ([ADR-001](./architecture/ADR-001-core-language-strategy.md))

### TypeScript
- Strict TS, no `any` without reason
- SDK stays thin: transport, hooks, config — no algorithm duplication

## Документация

| Layer | Path |
|-------|------|
| Agent entry | [CONTEXT.md](../CONTEXT.md) |
| Engineering SSOT | [project/FEATURES.md](./FEATURES.md), [traceability.md](./traceability.md) |
| Feature cards | [project/feature/](./feature/) |
| Case specs | [project/cases/](./cases/) |
| Public indexes | [docs/features.md](../docs/features.md), [docs/cases.md](../docs/cases.md) |
| Market / pitch | [docs/](../docs/) |

### При изменении фичи
1. Update feature card in [feature/](./feature/)
2. Update [FEATURES.md](./FEATURES.md) status
3. Update [traceability.md](./traceability.md) if E2E/fixture paths change
4. Update public index in `docs/` if needed

## Тесты

- E2E naming: `CONF-*` in conference specs; `c00N-*` in context
- Gate scripts: [scripts/test.sh](../scripts/test.sh), [run-java-e2e.sh](../scripts/run-java-e2e.sh)
