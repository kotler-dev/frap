# Таблица фич fletta

**Главная страница проекта** — единый источник правды по статусам фич, статистике и планированию релизов.

Статусы: ✅ Реализовано / ⚠️ Частично / ❌ Не реализовано / ⏸️ Заморожено / 🚫 Отменено

**Правило**: Фича считается реализованной только если есть работающий код + тесты + документация + минимум один проходящий кейс.

---

## Терминология

| Термин | Описание |
|--------|----------|
| Signature | Сигнатура элемента — устойчивые атрибуты + структурный путь |
| Drain3 | Алгоритм кластеризации, адаптированный для DOM/логов |
| MCP | Model Context Protocol — JSON-RPC для LLM-агентов |
| RCA | Root Cause Analysis — определение причины падения |
| Self-healing | Автоматическое восстановление селектора при изменении UI |

---

## Планирование релизов

- **release=v1.0.0** — MVP: Core + Playwright adapter
- **release=v1.1.0** — Context Layer: Unified Context + RCA
- **release=v1.2.0** — AI Integration: MCP + Page Object Gen + Feedback Loop
- **release=v2.0.0** — Scale: Multi-platform + Visual + Health Score
- **release=v3.0.0** — Future: AI-Agent Testing
- **release=v1.4.0** — Java SDK & UI adapters (bank S1)
- **release=backlog** — пока не запланировано (в т.ч. Python SDK)

---

## Приоритеты (Severity)

- **Critical** — блокирует MVP или критично для целевого релиза
- **High** — важная фича, без неё можно обойтись
- **Medium** — улучшение UX/функциональности
- **Low** — nice to have

---

## MVP v1.0.0 (Core + Playwright)

| Фича | Статус | Severity | Release | Примечание |
|------|--------|----------|---------|------------|
| [F000: Core Platform API](./feature/F000-core-platform-api.md) | ✅ | Critical | v1.0.0 | P0 WASM + `fletta-core`; FFI/JSON-RPC → v1.4.0 |
| [F001: Self-Healing Selectors](./feature/F001-self-healing.md) | ✅ | Critical | v1.0.0 | Rust Core + WASM runtime; Conference E2E |
| [F013: TypeScript SDK](./feature/F013-typescript-sdk.md) | ✅ | Critical | v1.0.0 | `healJson` WASM; README API |
| [F008: Playwright Adapter](./feature/F008-playwright-adapter.md) | ✅ | Critical | v1.0.0 | `withFletta`, JUnit в CI (CP005) |
| [F012: Debug Trace Mode](./feature/F012-debug-trace-mode.md) | ✅ | Medium | v1.0.0 | Debug режим с HTML отчётом (Classic + Explorer) |

---

## v1.1.0 (Context Layer)

| Фича | Статус | Severity | Release | Примечание |
|------|--------|----------|---------|------------|
| [F002: Unified Context](./feature/F002-unified-context.md) | ✅ | High | v1.1.0 | timeline + captureAll; C002/C003/C004 e2e |
| [F003: Root Cause Analysis](./feature/F003-rca.md) | ✅ | High | v1.1.0 | fletta-rca + WASM; C002/C003 verify-rca |

---

## v1.2.0 (AI Integration)

| Фича | Статус | Severity | Release | Примечание |
|------|--------|----------|---------|------------|
| [F004: Page Object Generator](./feature/F004-page-object-gen.md) | ❌ | Medium | v1.2.0 | Автогенерация из структуры |
| [F005: MCP/A2A Integration](./feature/F005-mcp-integration.md) | ❌ | Medium | v1.2.0 | JSON-RPC для LLM-агентов |
| [F009: Feedback Loop](./feature/F009-feedback-loop.md) | ❌ | Medium | v1.2.0 | Обучение на исправлениях |

---

## v2.0.0 (Scale)

| Фича | Статус | Severity | Release | Примечание |
|------|--------|----------|---------|------------|
| [F006: Multi-Platform Core](./feature/F006-multi-platform.md) | ❌ | Medium | v2.0.0 | Web + Android + iOS |
| [F007: Visual Fingerprint](./feature/F007-visual-fingerprint.md) | ❌ | Low | v2.0.0 | Визуальные признаки в сигнатуре |
| [F010: Test Health Score](./feature/F010-test-health.md) | ❌ | Medium | v2.0.0 | Метрика стабильности теста |

---

## v1.4.0 (Java / bank S1)

| Фича | Статус | Severity | Release | Примечание |
|------|--------|----------|---------|------------|
| [F014: Java SDK & UI Adapters](./feature/F014-java-sdk-ui-adapters.md) | ❌ | High | v1.4.0 | JUnit 5 + WebDriver P0; Selenide P1 |

---

## SDK / Adapters (backlog)

| Фича | Статус | Severity | Release | Примечание |
|------|--------|----------|---------|------------|
| [F015: Python SDK & Adapters](./feature/F015-python-sdk-adapters.md) | ❌ | Medium | backlog | pytest + JSON-RPC; FFI later |

---

## v3.0.0 (AI-Agent Testing)

| Фича | Статус | Severity | Release | Примечание |
|------|--------|----------|---------|------------|
| [F011: AI-Agent Testing & Audit](./feature/F011-ai-agent-testing.md) | ❌ | Low | v3.0.0 | Тестирование MCP tool calls |

---

## Критичные фичи для релизов

### v1.0.0 MVP
- [x] F000: Core Platform API — P0 WASM + `fletta-core` (FFI/RPC → v1.4.0)
- [x] F001: Self-Healing Selectors — Rust + WASM в SDK
- [x] F013: TypeScript SDK — `sdk/typescript` + README
- [x] F008: Playwright Adapter — Conference E2E + JUnit artifact
- [x] F012: Debug Trace Mode — developer experience

### v1.4.0 Java
- [ ] F014: Java SDK & UI Adapters — JUnit 5 + WebDriver; Selenide P1

### v1.1.0 Context
- [x] F002: Unified Context — timeline (F002.0–F002.6)
- [x] F003: RCA — классификация причин (F003.0–F003.5)

#### Подзадачи v1.1.0 (F002)

| ID | Статус | Зависимости |
|----|--------|-------------|
| F002.0 Cases C002/C003 | ✅ | — |
| F002.1 `crates/context` | ✅ | F002.0 (желательно) |
| F002.2 Network capture | ✅ | F002.1 |
| F002.3 Console capture | ✅ | F002.1 |
| F002.4 Correlation + window | ✅ | F002.2, F002.3 |
| F002.5 Report + `captureAll` | ✅ | F002.4 |
| F002.6 WebSocket capture | ✅ | F002.2 |

#### Подзадачи v1.1.0 (F003)

| ID | Статус | Зависимости |
|----|--------|-------------|
| F003.0 RootCause + rules | ✅ | F002.4 |
| F003.1 Classifier pipeline | ✅ | F003.0 |
| F003.2 RCA report JSON | ✅ | F003.1 |
| F003.3 Playwright / JUnit | ✅ | F003.2 |
| F003.4 Flaky aggregate | ✅ | F002.5, F003.1 |
| F003.5 MCP JSON stub | ✅ | F003.2 |

**Подзадачи v1.1.0:** 13/13 ✅ (F002.0–F002.6 + F003.0–F003.5)

### v1.2.0 AI
- [ ] F005: MCP Integration — JSON-RPC сервер

---

## Статистика

Взвешенный прогресс: **✅ = 100%**, **⚠️ = 50%**, **❌ = 0%** → `(✅ + 0.5×⚠️) / Всего`.

| Release | Всего | ✅ | ⚠️ | ❌ | Прогресс |
|---------|-------|----|----|----|----------|
| v1.0.0 | 5 | 5 | 0 | 0 | 100% |
| v1.1.0 | 2 | 2 | 0 | 0 | 100% |
| v1.2.0 | 3 | 0 | 0 | 3 | 0% |
| v1.4.0 | 1 | 0 | 0 | 1 | 0% |
| backlog | 1 | 0 | 0 | 1 | 0% |
| v2.0.0 | 3 | 0 | 0 | 3 | 0% |
| v3.0.0 | 1 | 0 | 0 | 1 | 0% |
| **Всего** | **16** | **7** | **0** | **9** | **44%** |

---

*Обновлено: 2026-05-24*

---

## Внедрение MVP

### Структура проекта
```
fletta/
├── crates/              # Rust core (signature, clustering, healing, fletta-core)
├── sdk/typescript/      # TypeScript SDK
├── adapters/playwright/ # Playwright adapter
├── test-app/           # FixtureConf pages (Conference demo)
├── e2e/conference/     # PoC gates CP001–CP005 (CONF-*)
├── e2e/context/        # C002/C003/C004 context layer demos + verify-context.mjs
├── crates/context/     # fletta-context (timeline, correlation, WebSocket model)
├── crates/rca/         # fletta-rca (classifier, report, MCP stub)
└── .github/workflows/  # CI pipeline
```

### Реализовано
- [x] Rust workspace: `signature`, `clustering`, `healing`, `fletta-core` (`crates/core`)
- [x] F000 P0: WASM `healJson`, `FlettaCore`, CI wasm-pack
- [x] TypeScript SDK: `HealingEngine` → WASM (`core.ts` + `core-fallback.ts` dev-only)
- [x] Playwright adapter + JUnit/JSON reporter
- [x] Conference E2E (CP001–CP005 gates) + `verify-reports.mjs`
- [x] CI: Rust tests, WASM build, Conference E2E, Context Layer E2E + RCA verify, JUnit artifact upload

### Ожидает (v1.0.1 / v1.4.0)

| Приоритет | Подзадача | Release |
|-----------|-----------|---------|
| MVP-C | Benchmark overhead &lt; 10% | v1.0.1 |
| P1 | FFI + cbindgen (F000) | v1.4.0 |
| P2 | JSON-RPC CLI (F000, Python F015) | v1.4.0 / backlog |
| P3 | Custom adapter guide + standalone examples | v1.4.0+ |

### Подзадачи v1.0.0 (закрыто)

| Приоритет | Подзадача | Статус |
|-----------|-----------|--------|
| P0.1 | `crates/core` Rust API | ✅ |
| P0.2 | WASM bindings + wasm-pack | ✅ |
| P0.3 | SDK → WASM, Conference E2E | ✅ |
| P0.4 | CI: Rust + WASM + Conference | ✅ |
| MVP-A | CP005 JUnit в CI (F008) | ✅ |
| P1 | FFI + cbindgen (F000) | v1.4.0 |
| P2 | JSON-RPC CLI (F000, Python F015) | v1.4.0 / backlog |
| P3 | Custom adapter guide + standalone examples | v1.4.0+ |
