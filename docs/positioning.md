# Позиционирование frap

Документ фиксирует место продукта на рынке, отличия от конкурентов, продуктовые формулировки и официальный one-liner. Основан на анализе рынка и внутренней стратегии (банк / InSourceHub / open source).

---

## One-liner (официальный)

> **frap** — deterministic engine для автоматического извлечения структуры UI: парсит деревья элементов (DOM, ViewTree, accessibility), кластеризует компоненты детерминированными алгоритмами, генерирует устойчивые идентификаторы для PageObject и тестов. Без ML в core, без облачных API — bank-grade deterministic. Внутренняя механика resolution обеспечивает стабильность при изменениях UI. AI-ready через MCP и optional enhancements tier: LLM получают структурированные element maps для генерации семантического кода.

### Короткие варианты по аудиториям

| Аудитория | One-liner |
|-----------|-----------|
| **Elevator (10 сек)** | Движок для извлечения структуры UI: парсит, кластеризует детерминированно, генерирует стабильные локаторы для PageObject — без ML в core, AI-ready через MCP |
| **Java разработчик** | Получи структурированную карту элементов страницы для генерации PageObject с устойчивыми селекторами — встраивается в Selenium/Playwright, deterministic, on-prem |
| **AI/LLM интегратор** | Дай LLM структурированный доступ к UI: element maps через MCP, стабильные идентификаторы, unified context для tool calls — frap даёт AI-агенту надёжные руки и глаза |
| **Enterprise/Банк** | Platform-agnostic engine для структурного анализа UI: on-prem, deterministic (NO ML dependencies), генерирует maintainable test code, bank-grade security |

---

## Architecture vs User Value

Чёткое разделение между внутренней архитектурой и пользовательской ценностью:

### Internal (для разработчиков/контрибьюторов)
- **Tree-agnostic Core** — алгоритмы кластеризации и matching работают с abstract tree interface
- **Clustering Algorithms** — Drain3-based deterministic grouping, NO ML training
- **Resolution Mechanism** — signature matching для стабильной идентификации при изменениях UI
- **Enhancement Adapters** — pluggable ML/LLM features (semantic naming, visual matching) как отдельные пакеты

### User-facing (ценность для пользователей)
- **Element Map** — структурированная карта UI с stable IDs и confidence scores
- **Stable Identifiers** — надёжные селекторы, которые переживают рефакторинг UI
- **Code Generation** — PageObject, тестовые сценарии на основе element maps
- **MCP Integration** — инструменты для LLM-агентов: discovery, grounding, context

---

## Immediate Value vs Long-term Vision

### Immediate Value (почему пробовать сейчас)

Конкретные боли, границы scope, формулировки для сайта и конференций — **[pains.md](./pains.md)** (единый источник).

Кратко сегодня:

1. **Структура UI за секунды** — `discover` → element map вместо часов ручного DOM.
2. **Grounding для AI** — element map через MCP; метафора: надёжные руки и глаза.
3. **On-prem, NO ML in core** — air-gapped, bank-grade, воспроизводимый результат.

### Long-term Vision (куда растём)

**Архитектурное видение:**

1. **Platform-agnostic Structure Engine**
   - Сегодня: Web DOM
   - Завтра: Android ViewTree, iOS UIView, JSON API schemas
   - Один element map format для всех платформ

2. **Enterprise Observability Layer**
   - Drift detection: "эта страница изменилась с прошлого раза" — см. [Structural Contract](./structural-contract.md) и [F017](../project/feature/F017-structural-contract.md)
   - Unified context: UI + network + logs в одном timeline
   - RCA: "почему тест упал?" с root cause classification

3. **AI-Native Testing Infrastructure**
   - Не "frap генерирует тесты через LLM"
   - А "frap даёт LLM инструменты для надёжного тестирования"
   - Агент использует MCP tools: `frap/discover`, `frap/analyze`

---

## AI Positioning: frap как Grounding Layer

### Что frap НЕ делает
- **frap ≠ AI testing tool** — мы не генерируем тесты из требований
- **frap ≠ LLM orchestrator** — мы не управляем диалогом с моделью
- **frap ≠ prompt engineering framework** — мы не шаблонизируем промпты

### Что frap делает для AI-агентов

**frap = grounding layer для AI-агентов:**
- **Глаза (discovery)** — структурированный element map вместо raw DOM
- **Руки (execution)** — stable resolution при изменении UI
- **Память (observability)** — audit trail действий для анализа

**Взаимодействие:**
```
AI Agent (orchestrator, снаружи)
    ↓ вызывает через MCP/API
frap Core (grounding layer, внутри)
    - discover(url) → element map
    - resolve(selector) → stable element
    - analyze(context) → RCA report
```

**Playwright MCP vs frap:**
| | Playwright MCP | frap |
|---|---|---|
| **Роль** | Агент **действует** в браузере | Агент **стабилен и объясним** |
| **Что делает** | Explore, click, navigate | Structure discovery, stable execution, audit |
| **Когда падает** | Селектор не найден | Находит по signature, объясняет разницу |
| **Детерминизм** | Зависит от LLM | Deterministic core, NO ML |

**Метафора:**
> **frap даёт AI-агенту надёжные руки и глаза** — стабильное зрение (element maps) и надёжные действия (resolution mechanism), deterministic и explainable.

---

## Проблема (Problem)

Полный каталог болей (универсальные, по сегментам S1–S3), граница «что не решаем», готовые блоки для сайта и слайдов — **[pains.md](./pains.md)**.

Кратко: команды тратят время на reverse engineering UI и поддержку локаторов; существующие record-replay и opaque self-healing не дают maintainable PageObject и audit; enterprise требует on-prem и объяснимости без облачного ML.

---

## Решение (Solution)

**frap** — platform-agnostic deterministic engine (Rust → WASM) для анализа UI-структур:

### Core Components

1. **Structure Discovery** — извлечение полного element tree с семантической кластеризацией
   - Platform adapters: Chrome/CDP, UIAutomator, XCUITest
   - Output: unified element map (JSON)

2. **Stable Identification** — генерация устойчивых идентификаторов на основе signature matching
   - Signature = устойчивые атрибуты + структурный путь
   - Confidence score по формуле, NO ML inference

3. **Code Generation** — PageObject, test scenarios на основе структурированных element maps
   - TypeScript/Java/Kotlin output
   - Semantic method names (с enhancement adapter для LLM naming)

4. **Robustness Layer** — resolution как internal mechanism
   - При изменении UI: signature matching → candidate ranking → stable fallback
   - Пользователь видит: "element map обновлён, diff: ..."

5. **AI Integration** — MCP tools для LLM
   - `frap/discover` — element map для grounding
   - `frap/analyze` — RCA для понимания падений
   - frap не генерирует тесты — даёт инструменты для генерации

### No-ML by Default

**Core (всегда deterministic):**
- Signature extraction — правила, не нейросети
- Clustering — Drain3 algorithm, hierarchical token matching
- Resolution — weighted attribute comparison, fixed formula

**Enhancements (опционально, M3+):**
- Semantic naming via LLM — отдельный пакет `frap-enhancements`
- Visual matching — OpenCV-based adapter
- Step generation — external LLM integration

---

## Зачем frap, если есть Playwright MCP + healer?

### Разные роли в стеке

| | Playwright MCP | playwright-healer / AutoHeal | **frap** |
|---|---|---|---|
| **Роль** | Агент **действует** в браузере (explore, codegen) | Чинит локаторы в рантайме (часто с AI/heuristics) | **Grounding layer** — structure discovery + stable execution |
| **Когда** | Разработка теста, ad-hoc сценарии | Падение локатора при прогоне | Структурный анализ, CI stabilization, audit |
| **Детерминизм** | Зависит от LLM-клиента | Смешанно (эвристики + опционально AI) | **Всегда воспроизводимый** алгоритм + отчёт |
| **Output** | Действия в браузере | Исправленный селектор | Element map + stable identifiers + audit trail |
| **AI Integration** | Действует через LLM | Пытается "починить" сам | Даёт **структуру** для надёжных AI-действий |

### One-liner для сравнения с MCP

> Playwright MCP помогает агенту **действовать** в браузере; frap **даёт агенту структуру** для надёжных действий — element maps, stable identifiers, deterministic resolution, explainable audit.

### Совместное использование (не конкуренция)

```
Требования → LLM анализирует (снаружи)
                ↓
        LLM вызывает frap/discover → получает element map
                ↓
        LLM генерирует test steps на основе структуры
                ↓
        Playwright MCP выполняет действия (внутри браузера)
                ↓
        frap resolution обеспечивает стабильность при изменениях UI
                ↓
        frap analyze даёт RCA если что-то пошло не так
```

**frap не заменяет Playwright.** Playwright = browser driver; frap = structure engine + observability layer.

---

## Конкурентный ландшафт

### Прямые (structure discovery / POM generation)

| Продукт | Тип | Пересечение | Отличие frap |
|---|---|---|---|
| [Healenium](https://github.com/healenium/healenium) | OSS + ML | Selenium healing | Без ML в core, без тяжёлого proxy/PostgreSQL, explainable |
| playwright-healer, AutoHeal | OSS / AI | Playwright healing | No-ML default, platform-agnostic core, bank on-prem |
| Testim, Mabl | Commercial | Smart locators | OSS, deterministic, element map export |
| Pomgo, Locator Labs | Tools | POM generation | Discovery + CI observability, не только генерация |

### Смежные (не заменяем, интегрируемся)

| Продукт | Роль |
|---|---|
| Playwright + getByRole/testId | Best practices — frap **не мешает**, дополняет структурным анализом |
| Playwright MCP | Explore/codegen — frap даёт **grounding** для стабильности |
| Karate | API+UI сценарии — frap как structure layer для UI части |
| Allure, ReportPortal | Отчёты — frap экспортирует discovery + resolution events |

### Защищаемый wedge (moat)

1. **Deterministic Core** — NO ML dependencies, bank-grade security, on-prem ready
2. **Platform-agnostic Architecture** — один element map format для Web/Android/iOS/API
3. **Explainable Structure** — element map с confidence scores, не "black box matching"
4. **MCP Grounding Layer** — даёт AI-агентам надёжный доступ к UI, не competing с AI tools
5. **Enterprise Audit Trail** — resolution history, drift detection, policy hooks

---

## Продуктовые формулировки

Использовать в README, InSourceHub, презентациях.

### Было (самонадеянно) → Стало (фиксировано)

| Было (самонадеянно) | Стало (фиксировано) |
|---|---|
| "Генерирует Page Object" | Генерирует **устойчивый element map / locator graph**; экспорт в POM/Java/TS — опция |
| "Self-healing без ML" | **Deterministic resolution**: signature matching, confidence scores, explainable diff |
| "Унифицированная платформа тестирования" | **Structure discovery engine** — анализирует UI для любых downstream tools |
| "Заменяет селекторы" | **Stable identification** — primary + fallback signatures, resolution при drift |
| "AI генерирует тесты" | **Алгоритмы дают структуру, LLM может генерировать** — frap = grounding layer |
| "Ниша свободна" | **Пересечение редкое**: OSS + deterministic + platform-agnostic + enterprise audit |

### Value props по сегментам

См. [audience.md](./audience.md).

---

## Позиционирование по фазам

| Фаза | Позиция на рынке |
|---|---|
| PoC / MVP (сейчас) | «Structure discovery engine: element map из любого UI за секунды» |
| v1.2 (generation layer) | «PageObject generation с stable identifiers — deterministic, explainable» |
| v2 (observability layer) | «CI structure drift detection + RCA: почему изменилась UI-структура» |
| v3 (AI integration) | «MCP grounding layer для AI-агентов — надёжные руки и глаза» |
| Enterprise | «Policy, SSO, audit log resolution, on-prem structure engine» |

---

## Критерии «доказали качество» (PoC/MVP)

Детали: [benchmark.md](./benchmark.md).

### PoC (неделя 1)

- CP001–CP003: element map coverage ≥ 90% для demo-приложения
- Повторяемость: два прогона discover → одинаковые element maps (±5%)
- 0 ложных «высоких confidence» при structural drift

### MVP (недели 2–3)

- Benchmark repo: ≥5 UI-рефакторингов, сравнение element map stability
- 3 команды в банке используют `frap discover` для PageObject
- Документированный discover → element map → generated code workflow < 15 минут

---

## Связанные документы

- [pains.md](./pains.md) — боли, scope, copy для сайта и конференций
- [audience.md](./audience.md) — целевая аудитория
- [integrations.md](./integrations.md) — интеграция, не замена
- [benchmark.md](./benchmark.md) — PoC-кейсы и метрики
- [monetization.md](./monetization.md) — монетизация (core OSS, enhancements tier)
- [cases.md](./cases.md) — полный каталог кейсов
- [strategy.md](./strategy.md) — 3 слоя архитектурного развития
- [glossary.md](./glossary.md) — терминология (Discover, Element Map, Resolution)
