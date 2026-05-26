# Fletta — Обзор проекта

Краткое введение в проект: что это, для чего, ключевые возможности и отличия.

---

## Что такое Fletta

**Fletta** — deterministic engine для автоматического извлечения структуры UI: парсит деревья элементов (DOM, ViewTree, accessibility), кластеризует компоненты детерминированными алгоритмами, генерирует устойчивые идентификаторы для Page Object и тестов.

**Без ML в core, без облачных API** — bank-grade deterministic engine.

**Метафора:** Fletta даёт AI-агенту и тестировщикам надёжные руки и глаза — структурированный доступ к UI с автоматическим восстановлением селекторов при изменениях.

---

## Ключевые возможности

| Возможность | Суть | Ценность |
|-------------|------|----------|
| **Structure Discovery** | Извлечение полной структуры UI в element map за секунды | Не анализировать DOM вручную часами |
| **Stable Identifiers** | Генерация устойчивых сигнатур элементов | Селекторы переживают рефакторинг UI |
| **Self-Healing Resolution** | Автоматическое восстановление при изменении структуры | Тесты не падают при обновлении фронта |
| **PageObject Generation** | Генерация кода из element maps | TypeScript/Java/Kotlin классы без boilerplate |
| **AI Agent Grounding** | MCP tools для LLM-агентов | Структурированный доступ к UI вместо «угадывания» |
| **Drift Detection** | Обнаружение изменений структуры до падения теста | Проактивное обновление тестов в CI |
| **Root Cause Analysis** | Классификация причин падения (UI/API/timing) | Минуты на диагностику вместо часов дебага |
| **Explainable Reports** | Отчёты с confidence scores и diff | Понятный audit trail для enterprise |

---

## Сценарии использования

### Для QA-автоматизации
- **Быстрое создание Page Object** для новых страниц — `frap discover` даёт готовую структуру
- **Стабилизация flaky тестов** — resolution находит элементы даже после рефакторинга
- **Drift detection в CI** — узнать об изменении UI до падения теста

### Для разработчиков
- **Reverse engineering legacy UI** — структурированная карта незнакомых страниц
- **Поддержка тестов при рефакторинге** — element map показывает, что изменилось

### Для AI-агентов и MCP
- **Grounding layer** — LLM получает структурированный element map вместо raw DOM
- **Надёжные действия** — stable identifiers не ломаются при minor UI changes
- **Audit trail** — лог всех действий агента для replay и анализа

---

## Почему Fletta

### Deterministic (NO ML in core)
- Алгоритмические методы: Drain3 clustering, weighted signature matching
- Reproducible results: одинаковый input → одинаковый output
- NO GPU, NO cloud API calls, NO training data

### On-prem / Bank-grade
- Работает в air-gapped окружениях
- Нет утечки данных в облако
- Проходит security review в regulated индустриях

### Explainable by design
- Confidence scores для каждого идентификатора
- Diff reports при drift: «было → стало, почему»
- Не "black box" — всё объяснимо и аудируемо

### Platform-agnostic
- Web (Chrome/CDP) — сегодня
- Android (UIAutomator), iOS (XCUITest) — roadmap
- Один element map format для всех платформ

### Integration, not replacement
- Дополняет Playwright/Selenium, не заменяет
- Adapter pattern: минимальные изменения существующих тестов
- Экспорт в JUnit XML, Allure, и другие форматы

### AI-ready через MCP
- `frap/discover` — element map для grounding
- `frap/analyze` — RCA для decision support
- `frap/resolve` — стабильное выполнение действий

---

## Архитектура: 3 слоя

```
┌─────────────────────────────────────────────────────────────┐
│ Layer 3: Observability & Feedback                           │
│   Drift Detection • RCA • Health Score • Audit Trail      │
│   [v2.0+]                                                   │
├─────────────────────────────────────────────────────────────┤
│ Layer 2: Generation Layer                                     │
│   PageObject Gen • Test Scenarios • Semantic API          │
│   [v1.2]                                                    │
├─────────────────────────────────────────────────────────────┤
│ Layer 1: Universal Element Discovery                          │
│   Core (Rust) • Sources • Element Map • NO ML             │
│   [v1.0 MVP]                                                │
└─────────────────────────────────────────────────────────────┘
```

**Layer 1 — Foundation:** Rust/WASM core с алгоритмами кластеризации и matching. Платформо-независимый, детерминированный.

**Layer 2 — Generation:** Генерация maintainable кода из element maps. Шаблоны для разных языков и фреймворков.

**Layer 3 — Intelligence:** Мониторинг структуры, обнаружение drift, root cause analysis, audit trail.

---

## Быстрый старт

```bash
# Дискаверинг UI — element map за секунды
frap discover --url https://example.com --output element-map.json

# Интеграция с Playwright — self-healing селекторы
# В playwright.config.ts:
selectors: { 'frap': '@frap/frap-playwright' }

// В тесте:
await page.click('frap://filters/category');
```

---

## Целевые аудитории

| Сегмент | Стек | Ключевая ценность |
|---------|------|-------------------|
| **S1: Enterprise QA** | Selenium/Java | Стабилизация legacy тестов без ML и облака |
| **S2: Modern teams** | Playwright/TS | Быстрый PageObject с устойчивыми селекторами |
| **S3: AI-native QA** | LLM agents/MCP | Grounding layer для надёжных AI-действий |

---

## Сравнение с альтернативами

| | Healenium | playwright-healer | **Fletta** |
|--|-----------|-------------------|------------|
| **Core** | ML-based | Heuristics + optional AI | **Deterministic (NO ML)** |
| **Output** | Fixed selector | Fixed selector | **Element map + stable IDs** |
| **Explainability** | Low | Medium | **Score + diff + audit** |
| **Platform** | Selenium only | Playwright only | **Platform-agnostic** |
| **On-prem** | Proxy+PostgreSQL | Depends | **WASM, zero dependencies** |
| **AI Integration** | No | No | **MCP grounding layer** |

---

## Связанные документы

- [FEATURES.md](./FEATURES.md) — полный список фич и статусы
- [Стратегия](../docs/strategy.md) — архитектурные слои и roadmap
- [Позиционирование](../docs/positioning.md) — one-liners, конкуренты
- [Messaging](../docs/messaging.md) — тезисы для презентаций
- [Аудитории](../docs/audience.md) — сегменты и боли

---

*Версия документа: 1.0.0*  
*Актуально для: MVP v1.0.0 →*