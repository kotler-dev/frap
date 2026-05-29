# Frap Knowledge Base

Связующий индекс фич и кейсов. Используется для навигации и понимания связей.

---

## Quick Navigation

| Файл | Назначение | Для кого |
|------|----------|----------|
| [positioning.md](./positioning.md) | One-liner, конкуренты, vs Playwright MCP, формулировки | PM, DevRel, InSourceHub |
| [pains.md](./pains.md) | Боли, границы scope, copy для сайта/презентаций/конференций | PM, DevRel, маркетинг |
| [audience.md](./audience.md) | ЦА: банк, Selenium/Java, Playwright, AI-native QA | PM, adoption |
| [benchmark.md](./benchmark.md) | PoC gates, CP001–CP005, метрики качества | Разработчики, QA |
| [integrations.md](./integrations.md) | Playwright plugin, JUnit, CI — не замена раннера | Разработчики |
| [integrations-selenium-java.md](./integrations-selenium-java.md) | JUnit 5 + WebDriver + PO; Selenide P1 (банк) | Java QA, архитекторы |
| [monetization.md](./monetization.md) | OSS + enterprise tiers | PM, бизнес |
| [features.md](./features.md) | Index → [project/feature/](../project/feature/) | Разработчики, PM |
| [cases.md](./cases.md) | Index → [project/cases/](../project/cases/) | QA, демо |
| [roadmap.md](./roadmap.md) | Narrative (status SSOT: [FEATURES.md](../project/FEATURES.md)) | Разработчики, менеджмент |
| [talk-topics.md](./talk-topics.md) | Темы для выступлений | DevRel, спикеры |
| [index.md](./index.md) | Связи и маппинг | Все |

---

## PoC Case Mapping (приоритет)

| PoC Case | Features | Gate doc |
|----------|----------|----------|
| CP001 Happy path | F008 | [benchmark.md](./benchmark.md) |
| CP002 Refactor heal | F001, F008 | benchmark |
| CP003 Safe fail | F001 | benchmark |
| CP004 Role locator | F008 | benchmark |
| CP005 CI export | F008 | benchmark + [integrations.md](./integrations.md) |

**Порядок реализации:** CP001 → CP002 → CP003 → CP004 → CP005 → C001 demo.

---

## Feature-to-Case Mapping

Какие кейсы демонстрируют какие фичи:

| Feature | Cases | Покрытие |
|---------|-------|----------|
| F001 Self-Healing | CP002, CP003, C001, C003, C006 | PoC + core demo |
| F002 Unified Context | C002, C003 | Требуется для RCA и flaky diagnosis |
| F003 RCA | C002, C003 | Демо с API timeout и race condition |
| F004 Page Object | C004 | Генерация, начало работы с проектом |
| F005 MCP Integration | C005 | AI-генерация тестов |
| F006 Multi-Platform | C006 | Android/iOS (v3) |
| F008 Playwright Adapter | C001, C004 | Главная интеграция для MVP |
| F009 Feedback Loop | C001, C003 | Обучение на исправлениях |
| F010 Test Health Score | C003 | Метрика стабильности |
| F011 AI-Agent Testing | C007, C008 | Тестирование агентов, MCP, A2A |

---

## Case Dependencies

Какие кейсы зависят от других:

```
C001 (Payment Button)
    ├── base: F001
    └── used by: C002, C003 (тот же flow для усложнения)

C002 (API Timeout RCA)
    ├── base: C001 (тот же payment flow)
    ├── requires: F002, F003
    └── demo order: запускать после C001

C003 (Flaky Diagnosis)
    ├── base: C001
    ├── requires: F001, F002, F003
    └── demo order: последний в серии

C004 (Page Object Gen)
    ├── standalone
    └── requires: F004

C005 (LLM Generation)
    ├── standalone
    └── requires: F005

C006 (Mobile)
    ├── base: C001 (аналогичный сценарий)
    └── requires: F006
```

---

## Demo Sequences

Рекомендуемые последовательности для разных аудиторий:

### InSourceHub / банк (5 минут)
1. CP002 — heal после рефакторинга testid
2. CP003 — безопасный fail (доверие ИБ)
3. CP005 — артефакт в GitLab CI
4. [audience.md](./audience.md) — кто подключает следующим

### Pitch Deck (3 минуты)
1. CP002 или C001 — self-healing (вводная)
2. C002 — RCA показывает ценность контекста (климакс, v2)

### Technical Demo (10 минут)
1. C004 — генерация Page Object (начало работы)
2. C001 — запись и healing (core функция)
3. C002 — RCA при падении (глубина)
4. C003 — анализ flaky тестов (зрелость)

### LLM/AI Demo (5 минут)
1. C005 — генерация теста из текста
2. C002 — RCA репорт для агента

---

## Development Priority

В каком порядке реализовывать кейсы для проверки фич:

| Этап | Цель | Кейсы | Фичи | Время |
|------|------|-------|------|-------|
| **MVP Week 1** | Self-healing + Playwright | CP001–CP005, C001 | F001, F008 | 1 неделя |
| **MVP Week 2-3** | Интеграция и фидбек | C001, C004 | F001, F004, F009 | 2 недели |
| **Context Layer** | RCA реальное | C002, C003 | F002, F003, F010 | Месяц 2 |
| **AI Integration** | MCP работает | C005 | F005 | Месяц 3 |
| **Scale** | Java SDK, CI plugins | Новые | F006, F007 | Месяцы 4-6 |
| **Multi-Platform** | Android/iOS | C006 | F006 | Месяцы 6-12 |

### Архитектура: Rust core + TS SDK

**Первый шаг:** Rust core с WASM сборкой для Node.js
**Второй шаг:** TypeScript SDK с Playwright adapter
**Третий шаг:** Java SDK (по запросу пользователей)

---

## Tag Index

Для быстрого поиска по сценариям:

| Тег | Кейсы | Когда использовать |
|-----|-------|------------------|
| `poc` | CP001–CP005 | Быстрая проверка PoC |
| `healing` | CP002, C001, C003, C006 | Self-healing |
| `rca` | C002, C003 | Глубокий анализ причин |
| `flaky` | C003 | Проблема нестабильных тестов |
| `api-failure` | C002 | Падение не из-за UI |
| `generation` | C004, C005 | Автоматическое создание тестов |
| `ai` | C005 | Интеграция с LLM |
| `ai-agent` | C007, C008 | Тестирование AI-агентов |
| `mcp` | C005, C007 | MCP интеграция |
| `a2a` | C008 | A2A протокол, мультиагенты |
| `mobile` | C006 | Не-веб платформы |

---

## How to Use This KB

**Для разработчика:**
1. Смотри [features.md](./features.md) — что строим
2. Находи связанный кейс в [cases.md](./cases.md) — как проверить
3. Реализуй фичу → прогони кейс → отметь статус

**Для QA/тестировщика:**
1. Смотри [cases.md](./cases.md) — какие сценарии есть
2. Ищи по тегу нужный тип тестирования
3. Используй демо-скрипты как regression suite

**Для презентаций:**
1. [pains.md](./pains.md) — открывающие слайды, секция «Проблема» на сайте
2. Выбирай Demo Sequence выше по времени
3. Каждый кейс имеет готовый скрипт и проверку успеха

---

## Status Tracking

**SSOT:** [project/cases/README.md](../project/cases/README.md) and [project/FEATURES.md](../project/FEATURES.md). Do not maintain duplicate status tables here.
