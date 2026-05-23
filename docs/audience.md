# Целевая аудитория (ЦА)

Стратегия adoption: **open source → InSourceHub в банке → масштаб на множество команд** с разным стеком (Legacy Selenium/Java, Playwright, AI-native QA).

---

## North Star

**Внутренний bank-grade open source**, который можно:

1. Опубликовать в **InSourceHub** с понятной лицензией и security review.
2. Подключить **без миграции** существующих тестов (adapter/plugin).
3. Масштабировать на **десятки команд** с разными фреймворками через единое ядро и отчёты.

Внешний GitHub и конференции — параллельный канал, не блокер для внутреннего старта.

---

## Сегменты ЦА

### S1: Legacy Selenium / Java (приоритет для банка)

**Кто:** Команды с годами e2e на Selenium 4, Page Object, JUnit/TestNG, Jenkins/GitLab CI.

**Боли:** ([pains.md](./pains.md#s1-legacy-selenium--java-enterprise-банк))

- Хрупкие XPath/CSS, дорогой рефакторинг POM.
- Healenium тяжёлый (ML, proxy, PostgreSQL) или не проходит security.
- Нет объяснимого отчёта «что починили и почему».

**Что даёт fletta:**

- Java SDK / JUnit listener (roadmap P4) — **не новый раннер**.
- Экспорт healing events в JUnit XML / Allure.
- On-prem, без внешних API.

**Сообщение:** «Стабилизируйте Selenium без ML и без облака — с аудитом в CI.»

**Интеграция:** [integrations-selenium-java.md](./integrations-selenium-java.md) (минимальный путь); обзор — [integrations.md](./integrations.md).

---

### S2: Playwright / TypeScript (MVP wedge)

**Кто:** Новые и мигрирующие команды, e2e на Playwright, GitHub Actions / GitLab.

**Боли:** ([pains.md](./pains.md#s2-playwright--typescript-mvp-wedge))

- Даже getByRole ломается при смене текста/i18n/структуры.
- Flaky из-за таймингов; непонятно UI vs API.
- playwright-healer тянет AI или не даёт enterprise audit.

**Что даёт fletta:**

- `@fletta/playwright` — custom selector `fletta:` + отчёты.
- PoC за неделю, минимальный порог входа.

**Сообщение:** «Добавьте одну строку в конфиг — healing в CI с explainable diff.»

**Интеграция:** Playwright adapter (F008) — **первый релиз**.

---

### S3: AI-native QA (тестирование агентов и MCP)

**Кто:** Команды, которые тестируют продукты через LLM-агентов, MCP tools, мультиагентные flows.

**Боли:** ([pains.md](./pains.md#s3-ai-native-qa-mcp-агенты))

- Playwright MCP умеет **действовать**, но не **регрессировать и аудировать** стабильность тестов в CI.
- Нет детерминированного отчёта по tool calls агента.
- Смена модели ломает поведение агента незаметно.

**Что даёт fletta (v3, F011):**

- Capture MCP tool calls, assertions на порядок/аргументы.
- `fletta/analyze` для RCA после падения агентного сценария.
- Совместно с Playwright MCP: MCP пишет, fletta **стабилизирует и проверяет**.

**Сообщение:** «Тестируйте не только приложение, но и поведение агента — детерминированно.»

**Кейсы:** C007, C008 — после MVP.

---

## Матрица: сегмент × фаза продукта

| Сегмент | PoC/MVP | v2 | v3+ |
|---------|---------|-----|-----|
| S1 Java/Selenium | Документация + roadmap | JUnit export, listener | Full Java SDK |
| S2 Playwright | **Adapter, CP001–CP003** | Unified context, RCA | MCP analyze |
| S3 AI-native QA | — | MCP generate (C005) | F011 agent audit |

---

## InSourceHub: как «затащить» в банк

### Требования к публикации (чек-лист)

- [ ] Лицензия: Apache-2.0 / MIT (согласовать с юристами банка)
- [ ] SBOM, зависимости без критичных CVE
- [ ] Нет телеметрии наружу по умолчанию
- [ ] Документация: security model, data handling (селекторы, DOM snapshots — что хранится)
- [ ] Quick start &lt; 15 мин на demo-проекте
- [ ] Контакт maintainer (внутренний чат / issue tracker)

### План rollout внутри банка

| Этап | Действие | Метрика |
|------|----------|---------|
| 0 | PoC на demo + CP001–CP003 | Демо записано |
| 1 | InSourceHub карточка + README RU | 1 пилотная команда (Playwright) |
| 2 | Митап T005, 2–3 команды | 5–10 сценариев в CI |
| 3 | Java/JUnit пилот (S1) | 1 legacy-команда |
| 4 | MCP + AI-native пилот (S3) | 1 команда agent QA |

### Роли внутри банка

| Роль | Интерес |
|------|---------|
| QA automation lead | Меньше поддержки POM, flaky RCA |
| Архитектор / ИБ | OSS, on-prem, audit trail |
| Platform / CI | GitLab/Jenkins plugin, JUnit XML |
| AI CoE | MCP tools, agent regression |

---

## Anti-personas (не ЦА на старте)

- Команды **без e2e** — сначала им нужен Playwright, не fletta.
- Стартапы, готовые платить за Testim/Mabl «из коробки» — другой GTM.
- «Только генерация тестов из текста» без CI — это Playwright MCP + LLM, fletta подключается позже.

---

## ICP для первого релиза (зафиксировано)

**Primary ICP (MVP):** Playwright-команды в банке (S2), CI на GitLab/Jenkins, требование on-prem и explainable healing.

**Secondary (следующий квартал):** Legacy Selenium/Java (S1).

**Tertiary:** AI-native QA (S3) — после F005/F011.

---

## Связанные документы

- [pains.md](./pains.md) — боли, scope, copy для сайта и презентаций
- [positioning.md](./positioning.md)
- [integrations.md](./integrations.md)
- [roadmap.md](./roadmap.md)
- [talk-topics.md](./talk-topics.md) — T005 InSourceHub
