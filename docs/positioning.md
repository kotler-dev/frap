# Позиционирование fletta

Документ фиксирует место продукта на рынке, отличия от конкурентов, продуктовые формулировки и официальный one-liner. Основан на анализе рынка и внутренней стратегии (банк / InSourceHub / open source).

---

## One-liner (официальный)

> **fletta** — open-source explainable self-healing для Playwright и Selenium: один сценарий связывает UI, console и network; чинит локаторы детерминированными алгоритмами, а не LLM; отдаёт объяснимые diffs в CI и в MCP для агентов.

### Короткие варианты

| Контекст | Текст |
|----------|--------|
| Elevator (RU) | Self-healing тестов без ML: объяснимо, on-prem, встраивается в Playwright/Selenium. |
| Elevator (EN) | Explainable, algorithmic test healing — plug into Playwright/Selenium, no LLM required. |
| InSourceHub / банк | Внутренний open source для стабилизации e2e: без облачных API, с аудитом healing в CI. |
| GitHub README | Deterministic locator healing + unified UI/logs/network context for CI and MCP agents. |

### Что убрали из ранних формулировок

- «Ниша свободна» — заменено на **«ниша пересечена по частям, свободно пересечение OSS + no-ML + tri-plane + bank-grade»**.
- «Автоматически генерирует Page Object» как главный месседж — заменено на **element map / locator graph** (POM — опциональный экспорт).
- «Платформа тестирования» — заменено на **слой стабилизации и анализа поверх существующих раннеров**.

---

## Проблема (Problem)

Команды тратят значительную долю времени на поддержку селекторов и Page Object при изменениях UI. Рефакторинг фронтенда ломает e2e. Существующие self-healing решения часто:

- проприетарны и требуют облака (Testim, Mabl);
- используют ML без объяснимости (Healenium);
- лечат только UI, не отличая падение из-за API/логов;
- не подходят для regulated / on-prem (банк, compliance).

---

## Решение (Solution)

**fletta** — алгоритмическое ядро (Rust → WASM) + тонкие адаптеры:

1. **Self-healing** — primary-селектор сохраняется; fallback-цепочка по сигнатурам включается только при failure.
2. **Explainable healing** — score, diff DOM/сигнатуры, альтернативы; порог confidence; иначе fail + review.
3. **Unified context** (v2) — UI + console + network в одном timeline для RCA.
4. **MCP** (v2+) — агенты **анализируют и стабилизируют** тесты; fletta не заменяет Playwright как драйвер.

---

## Зачем fletta, если есть Playwright MCP + healer?

Это главный вопрос adoption. Ответ — **разные роли в стеке**.

| | Playwright MCP | playwright-healer / AutoHeal | **fletta** |
|---|----------------|------------------------------|------------|
| **Роль** | Агент **действует** в браузере (explore, codegen) | Чинит локаторы в рантайме (часто с AI/heuristics) | **Стабилизирует и объясняет** существующие тесты в CI |
| **Когда** | Разработка теста, ad-hoc сценарии | Падение локатора при прогоне | Регрессия, рефакторинг UI, audit, банк on-prem |
| **Детерминизм** | Зависит от LLM-клиента | Смешанно (эвристики + опционально AI) | **Всегда воспроизводимый** алгоритм + отчёт |
| **UI + логи + сеть** | Сеть/консоль в сессии агента | Обычно только DOM | **Один сценарий, один timeline** (v2) |
| **Legacy Selenium/Java** | Слабо | Healenium (ML, proxy) | Адаптер + JUnit (roadmap) |
| **Compliance** | Облачный LLM часто недопустим | Внешние API у AI-healers | **Без ML по умолчанию**, OSS, on-prem |

### One-liner для сравнения с MCP

> Playwright MCP помогает агенту **действовать** в браузере; fletta **стабилизирует и объясняет** уже написанные тесты при изменении UI, логов и API — детерминированно, в CI, без обязательного LLM.

### Совместное использование (не конкуренция)

```
Требования → LLM + Playwright MCP (написать черновик теста)
                    ↓
            Playwright/Selenium тест в репозитории
                    ↓
            fletta adapter в CI (healing + отчёты + RCA)
                    ↓
            При падении: fletta MCP analyze → агент предлагает фикс PR
```

**fletta не заменяет Playwright.** Используем Playwright как раннер; fletta — custom selector / hook + отчёты + (опционально) MCP tools `heal`, `analyze`, `export`.

---

## Конкурентный ландшафт

### Прямые (self-healing / POM)

| Продукт | Тип | Пересечение | Отличие fletta |
|---------|-----|-------------|----------------|
| [Healenium](https://github.com/healenium/healenium) | OSS + ML | Selenium healing | Без ML, без тяжёлого proxy/PostgreSQL по умолчанию, explainable |
| playwright-healer, AutoHeal | OSS / AI | Playwright healing | No-ML default, tri-plane, bank on-prem narrative |
| Testim, Mabl | Commercial | Smart locators | OSS, детерминизм, InSourceHub |
| Pomgo, Locator Labs | Tools | POM generation | Healing + CI, не только генерация |

### Смежные (не заменяем, интегрируемся)

| Продукт | Роль |
|---------|------|
| Playwright + getByRole/testId | Best practices — fletta **не мешает**, дополняет при поломке |
| Playwright MCP | Explore/codegen — fletta в **CI и audit** |
| Karate | API+UI сценарии — fletta как post-run healing layer (future) |
| Allure, ReportPortal | Отчёты — экспорт fletta healing events |

### Защищаемый wedge (moat)

1. **Explainable healing** — score, diff, policy (min confidence).
2. **Tri-plane сценарий** — UI + console + network (редко в OSS).
3. **Bank-first OSS** — InSourceHub, on-prem, без облачного inference.
4. **MCP для стабилизации**, не для «ещё одного браузерного драйвера».

---

## Продуктовые формулировки

Использовать в README, InSourceHub, презентациях.

| Было (слабо) | Стало (фиксировано) |
|--------------|---------------------|
| Генерирует Page Object | Генерирует **устойчивый element map / locator graph**; экспорт в POM/Java/TS — опция |
| Self-healing без ML | **Explainable healing**: score, diff, альтернативы; ML не требуется |
| Унифицированная платформа тестирования | **Слой стабилизации и RCA** поверх Playwright/Selenium |
| Замена селекторов | **Primary + fallback chain**; healing только при failure primary |
| AI генерирует тесты | **Алгоритмы чинят, LLM пишет/анализирует** (MCP) |
| Ниша свободна | **Пересечение редкое**: OSS + deterministic + tri-plane + enterprise audit |

### Value props по сегментам

См. [audience.md](./audience.md).

---

## Позиционирование по фазам

| Фаза | Позиция на рынке |
|------|------------------|
| PoC / MVP | «Playwright plugin с explainable self-healing на 3–5 UI-диффах» |
| v2 | «CI healing + RCA: UI vs API vs flaky» |
| v3 | «MCP tools для агентов + audit AI-agent tool calls» |
| Enterprise | «Policy, SSO, audit log healing, InSourceHub-ready» |

---

## Критерии «доказали качество» (PoC/MVP)

Детали: [benchmark.md](./benchmark.md).

**PoC (неделя 1):**

- CP001–CP003 проходят на demo-приложении
- 0 ложных «зелёных» кликов при confidence &lt; порога
- Отчёт healing воспроизводим (два прогона — один diff)

**MVP (недели 2–3):**

- Benchmark repo: ≥5 UI-рефакторингов, сравнение с baseline Playwright
- 3 команды в банке на Playwright adapter
- Документированный adapter install &lt; 15 минут

---

## Связанные документы

- [audience.md](./audience.md) — целевая аудитория
- [integrations.md](./integrations.md) — интеграция, не замена
- [benchmark.md](./benchmark.md) — PoC-кейсы и метрики
- [monetization.md](./monetization.md) — монетизация
- [cases.md](./cases.md) — полный каталог кейсов
