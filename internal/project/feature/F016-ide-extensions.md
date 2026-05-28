# Feature: IDE Extensions (F016)

## Meta

- **Epic**: Developer Experience → Editor integration
- **Roll-up target**: ## v1.2.0 (AI Integration) — Phase 1; ## v1.4.0 (Java / bank S1) — Phase 2
- **Status**: draft
- **Target release**: v1.2.0 (VS Code / Cursor), v1.4.0 (JetBrains / GigaIDE)
- **Created**: 2026-05-26
- **Related cases**: C004 (generated POM), CP002/CP003 (debug reports), S1/S2 audience
- **Depends on**: F004 (element map + generate), F012 (debug HTML), F008/F013 (Playwright/TS); F014 (Java) for Phase 2

## Goal

**Тонкий DX-слой поверх element map и генерации** — не отдельный продукт рядом с core. Редактор помогает писать и отлаживать тесты с `frap:` / stable IDs, читая те же артефакты, что CLI, MCP (F005) и генератор (F004).

Пользователь не «учит API Frap в IDE» — он получает completion по stable ID, сниппеты и быстрые действия (открыть debug-отчёт, обновить map), пока core остаётся источником истины для healing и audit.

## Principles

1. **Data-first** — сначала контракт артефактов (element map, signatures), потом UI редактора.
2. **One index, many hosts** — один workspace-индекс / optional LSP; VS Code и JetBrains — адаптеры, не три независимых продукта.
3. **No duplicate core** — плагин не вызывает WASM/healing inline; только читает JSON и дергает CLI при необходимости.
4. **TS API completion** — зона ответственности `tsserver` + published types (`frapcode`); F016 закрывает project-aware сценарии (`frap:…`, map, config schema).

## User workflow

### Phase 1 — Playwright / TypeScript (VS Code, Cursor)

1. В проекте есть `frap discover` / `frap generate` (F004) → `.frap/element-map.json` (или путь из конфига).
2. Разработчик пишет `page.locator('frap:|')` — после `frap:` IDE предлагает stable ID из map.
3. При сохранении / по команде «Refresh Frap index» extension перечитывает map (file watcher).
4. После падения теста с `debug: true` — команда «Open last Frap debug report» открывает `frap-debug-explorer.html` (F012).

### Phase 2 — Java / bank S1 (IntelliJ IDEA, GigaIDE)

1. Тот же `.frap/element-map.json` (или Maven resource path).
2. В Page Object: `By.cssSelector("…")` или Frap-обёртка — completion для stable alias / `frap:` в строках и константах.
3. Run configuration: JUnit с `@ExtendWith(FrapExtension)` (F014) + ссылка на последний `frap-reports/`.

## Scope

### In — Phase 0 (contract, все фазы)

- JSON Schema для **workspace config** (`frap.config.json` / ключи из [Frap.md](../../Frap.md): `confidence`, `maxCandidates`, `healStrategy`, `debug`, …).
- Документированный **путь артефактов** (defaults):
  - `.frap/element-map.json` — element map (формат см. `project/architecture/platform-agnostic-core.md`)
  - `.frap/signatures/*.json` — опционально, per-element bind
  - `frap-reports/`, `frap-debug.html` — отчёты (F008, F012)
- Спека **stable ID → display** для completion (id, confidence, cluster, primary selector hint).

### In — Phase 1 (VS Code / Cursor extension)

- Completion в строковых литералах с префиксом `frap:` (Playwright `page.locator`, template literals).
- Сниппеты: `withFrap` setup, `page.locator('frap:…')`, типовой `FrapConfig` block.
- File watcher + команда **Frap: Refresh element index**.
- Команды: **Open Frap debug report** (последний / picker), **Reveal element map** (preview JSON или sidebar tree — minimal).
- `package.json` `contributes.jsonValidation` для config schema.
- README: установка из VSIX / Open VSX; совместимость с Cursor.

### In — Phase 2 (JetBrains plugin)

- Тот же индекс element map; completion в Java/Kotlin string literals и static final constants.
- Marketplace: IntelliJ IDEA; отдельный listing/совместимость для **GigaIDE** (та же платформа, без второго codebase).
- Run/debug: ссылка на каталог отчётов (после F014).

### Out

- Отдельный language server как обязательный артефакт v1 (optional позже, если два host'а разойдутся).
- Healing / DOM parse внутри IDE.
- Замена F004 generate / F005 MCP.
- Полноценный visual Page Object editor.
- Eclipse, Vim-only plugins (backlog).
- Дублирование IntelliSense для экспортируемых TS/Java API пакетов SDK.

## Phasing

| Phase | Release | Deliverable |
|-------|---------|-------------|
| 0 | v1.2.0 (с F004) | Schema + paths + docs в `docs/integrations.md` |
| 1 | v1.2.0 | `extensions/vscode-frap` (working MVP) |
| 2 | v1.4.0 | `extensions/intellij-frap` (после F014 pilot) |

## Acceptance criteria

### Phase 0 — Artifact contract

- [ ] Зафиксирован путь по умолчанию `.frap/element-map.json` и минимальная JSON Schema element map (поля: `elements[].id`, `confidence`, опционально `signature` / selector hint).
- [ ] JSON Schema для `frap.config.json` (или эквивалент в `playwright` adapter config) опубликована в репо (`schemas/`).
- [ ] Документ: как CLI `discover` / F004 `generate` обновляют файлы, которые читает IDE.

### Phase 1 — VS Code / Cursor

- [ ] Extension активируется в workspace с `frapcode-playwright` или `frap.config.json` / `.frap/`.
- [ ] В `page.locator('frap:')` после триггера `:` или `Ctrl+Space` — список stable ID из element map (≥10 элементов в fixture-проекте).
- [ ] Fuzzy match по части ID (`pay` → `pay-btn`, `payment-submit`).
- [ ] File watcher: изменение `element-map.json` обновляет completion без перезапуска IDE.
- [ ] Команда **Frap: Refresh element index** пересканирует `.frap/`.
- [ ] Команда **Frap: Open debug report** открывает последний `frap-debug-explorer.html` или classic index (F012).
- [ ] Сниппеты: минимум `frap-with-playwright`, `frap-locator`.
- [ ] Smoke: ручной чеклист в `extensions/vscode-frap/README.md` (5 минут).
- [ ] CI (optional): `npm test` для completion provider unit tests без headless IDE.

### Phase 2 — JetBrains / GigaIDE

- [ ] Completion для `frap:` / stable ID в Java string literal на demo-проекте с F014.
- [ ] Тот же element map path; настройка custom path в Settings.
- [ ] Документация: установка в IDEA и GigaIDE (корп. marketplace при необходимости).
- [ ] (Optional) gutter icon / intention: jump to element map entry for ID under caret.

### Cross-cutting

- [ ] Нет сетевых вызовов по умолчанию (on-prem, offline).
- [ ] Версионирование: extension `engines` совместим с LTS VS Code; plugin `sinceBuild` для IDEA.

## Implementation notes (sketch)

### Layout (целевой)

```
schemas/
  element-map.schema.json
  frap.config.schema.json
extensions/
  vscode-frap/           # Phase 1
    package.json
    src/
      index.ts
      element-index.ts   # load + watch .frap/
      completion-frap.ts # frap: prefix in strings
      commands.ts        # debug report, refresh
  intellij-frap/         # Phase 2 (Gradle)
    ...
docs/integrations.md   # секция IDE
```

### Completion algorithm (sketch)

1. Detect string literal context + prefix `frap:` (regex / simple parser, not full TS AST required for MVP).
2. Load `ElementIndex` from `.frap/element-map.json` (cached, mtime).
3. Filter by partial ID; sort by confidence desc; show `id` + `confidence` + truncated selector in `detail`.

### Shared index (optional later)

```
frap ide index --workspace . --stdout   # CLI emits completion JSON for any editor
```

Полезно, если JetBrains и VS Code должны делить одну логику без дублирования — **не блокер Phase 1**.

### Dependencies

| Фича | Зачем |
|------|--------|
| F004 | Element map + generate → содержимое completion |
| F012 | Debug report paths + UX команд |
| F008/F013 | Playwright entry, типовые паттерны сниппетов |
| F005 | Тот же map для MCP; IDE не конкурирует |
| F014 | Java completion + JUnit report paths |

### Risks

- **Пустой map** — completion бесполезен до discover; extension должен показывать actionable hint («Run frap discover»).
- **Дрейф формата map** — schema version field `mapVersion` в JSON.
- **Три IDE бренда** — GigaIDE = JetBrains; не плодить репозитории.

## Verification / Test plan

### Manual smoke (Phase 1)

1. Fixture: `test-app` + сгенерированный `.frap/element-map.json` (stub или F004).
2. Открыть `internal/testing/conference/*.spec.ts`, ввести `frap:`, убедиться в списке ID.
3. Изменить map на диске → completion обновился.
4. Прогнать тест с `debug: true` → команда открывает explorer.

### Automation

- Unit: `element-index.ts` parse + filter (fixture JSON).
- E2E IDE: backlog (heavy); достаточно manual для v1.2.

## Related docs

- [F004 Page Object Generator](./F004-page-object-gen.md)
- [F005 MCP Integration](./F005-mcp-integration.md)
- [F008 Playwright Adapter](./F008-playwright-adapter.md)
- [F012 Debug Trace Mode](./F012-debug-trace-mode.md)
- [F014 Java SDK](./F014-java-sdk-ui-adapters.md)
- [platform-agnostic-core.md](../architecture/platform-agnostic-core.md) — ElementMap format
- [docs/audience.md](../../docs/audience.md) — S1 (GigaIDE) / S2 (VS Code)
- [docs/integrations.md](../../docs/integrations.md)
- [Frap.md](../../Frap.md) — config keys, unified DSL

## Non-goals (explicit)

- IDE extension **не** входит в npm/crates поставку core; отдельный release cadence (VSIX / JetBrains plugin).
- Позиционирование: «Frap for VS Code» как **удобство**, не как отдельный SKU; бренд остаётся на `frapcode` / CLI / MCP.
