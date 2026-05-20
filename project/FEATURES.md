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
- **release=backlog** — пока не запланировано

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
| [F001: Self-Healing Selectors](./feature/F001-self-healing.md) | ❌ | Critical | v1.0.0 | Ядро продукта — сигнатуры и кластеризация |
| [F008: Playwright Adapter](./feature/F008-playwright-adapter.md) | ❌ | Critical | v1.0.0 | Интеграция через custom selectors |

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

## v3.0.0 (AI-Agent Testing)

| Фича | Статус | Severity | Release | Примечание |
|------|--------|----------|---------|------------|
| [F011: AI-Agent Testing & Audit](./feature/F011-ai-agent-testing.md) | ❌ | Low | v3.0.0 | Тестирование MCP tool calls |

---

## Критичные фичи для релизов

### v1.0.0 MVP
- [ ] F001: Self-Healing Selectors — core алгоритм
- [ ] F008: Playwright Adapter — интеграция

### v1.1.0 Context
- [ ] F002: Unified Context — timeline
- [ ] F003: RCA — классификация причин

### v1.2.0 AI
- [ ] F005: MCP Integration — JSON-RPC сервер

---

## Статистика

| Release | Всего | ✅ | ⚠️ | ❌ | Прогресс |
|---------|-------|----|----|----|----------|
| v1.0.0 | 2 | 0 | 0 | 2 | 0% |
| v1.1.0 | 2 | 0 | 0 | 2 | 0% |
| v1.2.0 | 3 | 0 | 0 | 3 | 0% |
| v2.0.0 | 3 | 0 | 0 | 3 | 0% |
| v3.0.0 | 1 | 0 | 0 | 1 | 0% |
| **Всего** | **11** | **0** | **0** | **11** | **0%** |

---

*Обновлено: 2026-05-20*
