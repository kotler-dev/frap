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
| [F000: Core Platform API](./feature/F000-core-platform-api.md) | ⚠️ | Critical | v1.0.0 | **P0.1** `fletta-core` ✅; **P0.2** WASM; FFI/RPC → v1.4.0 |
| [F001: Self-Healing Selectors](./feature/F001-self-healing.md) | ⚠️ | Critical | v1.0.0 | Core реализован, тесты в CI |
| [F013: TypeScript SDK](./feature/F013-typescript-sdk.md) | ⚠️ | Critical | v1.0.0 | `sdk/typescript`, контракт API |
| [F008: Playwright Adapter](./feature/F008-playwright-adapter.md) | ⚠️ | Critical | v1.0.0 | Интеграция через wrapper API |
| [F012: Debug Trace Mode](./feature/F012-debug-trace-mode.md) | ✅ | Medium | v1.0.0 | Debug режим с HTML отчётом (Classic + Explorer) |

---

## v1.1.0 (Context Layer)

| Фича | Статус | Severity | Release | Примечание |
|------|--------|----------|---------|------------|
| [F002: Unified Context](./feature/F002-unified-context.md) | ❌ | High | v1.1.0 | UI + logs + network в timeline |
| [F003: Root Cause Analysis](./feature/F003-rca.md) | ❌ | High | v1.1.0 | Классификация причин падения |

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
- [ ] F000: Core Platform API — P0.1 ✅ `fletta-core`; далее P0.2 WASM + SDK ([F000](./feature/F000-core-platform-api.md#subtasks))
- [ ] F001: Self-Healing Selectors — core алгоритм
- [ ] F013: TypeScript SDK — reference API (`sdk/typescript`)
- [ ] F008: Playwright Adapter — интеграция
- [x] F012: Debug Trace Mode — developer experience

### v1.4.0 Java
- [ ] F014: Java SDK & UI Adapters — JUnit 5 + WebDriver; Selenide P1

### v1.1.0 Context
- [ ] F002: Unified Context — timeline
- [ ] F003: RCA — классификация причин

### v1.2.0 AI
- [ ] F005: MCP Integration — JSON-RPC сервер

---

## Статистика

Взвешенный прогресс: **✅ = 100%**, **⚠️ = 50%**, **❌ = 0%** → `(✅ + 0.5×⚠️) / Всего`.

| Release | Всего | ✅ | ⚠️ | ❌ | Прогресс |
|---------|-------|----|----|----|----------|
| v1.0.0 | 5 | 1 | 4 | 0 | 60% |
| v1.1.0 | 2 | 0 | 0 | 2 | 0% |
| v1.2.0 | 3 | 0 | 0 | 3 | 0% |
| v1.4.0 | 1 | 0 | 0 | 1 | 0% |
| backlog | 1 | 0 | 0 | 1 | 0% |
| v2.0.0 | 3 | 0 | 0 | 3 | 0% |
| v3.0.0 | 1 | 0 | 0 | 1 | 0% |
| **Всего** | **16** | **1** | **4** | **11** | **19%** |

---

*Обновлено: 2026-05-23*

---

## Внедрение MVP

### Структура проекта
```
fletta/
├── crates/              # Rust core (signature, clustering, healing, fletta-core)
├── sdk/typescript/      # TypeScript SDK
├── adapters/playwright/ # Playwright adapter
├── test-app/           # PoC test pages (CP001-CP003)
├── e2e/                # Playwright E2E tests
└── .github/workflows/  # CI pipeline
```

### Реализовано
- [x] Rust workspace: `signature`, `clustering`, `healing`, `fletta-core` (`crates/core`)
- [x] F000 P0.1: публичный API `FlettaCore`, `HealRequest`, `heal_json()` — `cargo test -p fletta-core`
- [x] Алгоритмы: сигнатуры, кластеризация (Drain3), healing fallback
- [x] TypeScript SDK с HealingEngine
- [x] Playwright adapter (wrapper API)
- [x] E2E тесты CP001, CP002, CP003
- [x] CI pipeline (GitHub Actions)
- [x] Тестовое приложение

### Ожидает финальной верификации
- [ ] Прохождение всех PoC кейсов в CI (CP001–CP005)
- [ ] CP005: JUnit XML артефакт в CI (F008)
- [ ] F000 P0.2–P0.4: WASM в SDK, CI, без дублирования алгоритмов в TS
- [ ] Performance: overhead < 10% (можно v1.0.1, если CP001–CP005 уже green)

### Подзадачи закрытия v1.0.0 (критично → позже)

| Приоритет | Подзадача | Release |
|-----------|-----------|---------|
| P0.1 | `crates/core` Rust API | v1.0.0 ✅ |
| P0.2 | WASM bindings + wasm-pack | v1.0.0 |
| P0.3 | SDK → WASM, e2e CP001–CP003 | v1.0.0 |
| P0.4 | CI: Rust + WASM + e2e | v1.0.0 |
| MVP-A | CP005 JUnit в CI (F008) | v1.0.0 |
| MVP-C | PoC gates + benchmark overhead | v1.0.0 / v1.0.1 |
| P1 | FFI + cbindgen (F000) | v1.4.0 |
| P2 | JSON-RPC CLI (F000, Python F015) | v1.4.0 / backlog |
| P3 | Custom adapter guide + standalone examples | v1.4.0+ |
