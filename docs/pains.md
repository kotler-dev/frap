# Боли и границы ценности Fletta

Единый источник формулировок **проблем**, которые решает Fletta, и **границ** — что остаётся на стороне вашего test framework. Использовать для сайта, презентаций, конференций, InSourceHub и sales enablement.

**Связанные документы:** [positioning.md](./positioning.md) (рынок и конкуренты), [audience.md](./audience.md) (сегменты S1–S3), [messaging.md](./messaging.md) (one-liners и возражения), [integrations.md](./integrations.md) (как подключаем).

---

## Граница ответственности (scope)

Fletta — **слой вокруг UI-структуры и селекторов**, не замена test lifecycle.

| В зоне Fletta | Вне зоны Fletta (ваш фреймворк / команда) |
|---------------|-------------------------------------------|
| Структура UI, element map, кластеризация | Тестовые данные, фикстуры, БД |
| Стабильные идентификаторы и сигнатуры | API-контракты, моки, contract tests |
| Healing / resolution при drift UI | Оркестрация suite, параллель, шардинг |
| Explainable diff, audit в CI | Page Object **бизнес-логика**, шаги сценария |
| Discovery, drift detection, RCA (roadmap) | Окружения, секреты, деплой стендов |
| Grounding для LLM (MCP) | Полная генерация тестов «из требований» без CI |

**Короткая формулировка для слайда:**

> Fletta снимает рутину вокруг **селекторов и анализа DOM** — не весь test lifecycle (данные, API, окружения, оркестрация, ваши абстракции).

---

## Универсальные боли (все сегменты)

| ID | Боль | Как проявляется | Что даёт Fletta | Статус |
|----|------|-----------------|-----------------|--------|
| P01 | **Ручной reverse engineering UI** | Часы на XPath/CSS, устаревшие PageObject | `discover` → element map за секунды | MVP |
| P02 | **Локаторы ломаются при рефакторинге** | Падает CI после смены testid/DOM | Deterministic resolution + explainable diff | MVP |
| P03 | **Healing без объяснения** | «Починилось», но непонятно что и почему | Confidence score, top-k кандидатов, audit trail | MVP |
| P04 | **Record-replay даёт хрупкий код** | Codegen/Selenium IDE — не maintainable POM | Element map → генерация POM (опция) | v1.2 |
| P05 | **Self-healing = чёрный ящик или ML** | Healenium/Testim не проходят security / ИБ | NO ML in core, on-prem, воспроизводимость | MVP |
| P06 | **Непонятно: UI vs сеть vs тайминг** | Flaky, долгий разбор падения | Unified context + RCA (roadmap) | v1.1+ |
| P07 | **Drift UI незаметен до массового падения** | Рефакторинг фронта ломает десятки тестов | Drift report, сравнение element maps | v2 |
| P08 | **AI-агент «видит» DOM, но не структуру** | Хаотичные клики, нет регресса в CI | MCP grounding: element map + audit | v1.2 / v3 |

---

## Боли по сегментам

Сегменты — см. [audience.md](./audience.md). Ниже — расширенные формулировки для маркетинга и докладов.

### S1: Legacy Selenium / Java (enterprise, банк)

| ID | Боль | Сообщение для аудитории |
|----|------|-------------------------|
| S1-P01 | Годы e2e на XPath/CSS, каждый рефакторинг POM — недели | «Стабилизируйте Selenium без смены раннера» |
| S1-P02 | Healenium тяжёлый: ML, proxy, PostgreSQL | «Healing без ML и без облака» |
| S1-P03 | Security review блокирует облачные healing-сервисы | «Bank-grade: on-prem, deterministic core» |
| S1-P04 | Нет отчёта в JUnit/Allure: что починили | «Audit healing events в привычном CI» |
| S1-P05 | Страшно менять Page Object структуру | «Extension/listener — healing на findElement» |

**One-liner сегмента:** Получи структурированную карту UI для PageObject с устойчивыми селекторами — встраивается в Selenium/JUnit, deterministic, on-prem.

---

### S2: Playwright / TypeScript (MVP wedge)

| ID | Боль | Сообщение для аудитории |
|----|------|-------------------------|
| S2-P01 | getByRole/testId ломаются при i18n и смене текста | «Структурный анализ, не только роль» |
| S2-P02 | playwright-healer / AutoHeal — эвристики или AI без audit | «Explainable healing в CI» |
| S2-P03 | Высокий порог: «ещё один фреймворк» | «Одна строка в playwright.config» |
| S2-P04 | Flaky: неясно UI или API | «Tri-plane context» (roadmap) |

**One-liner сегмента:** Добавьте healing в CI с explainable diff — без миграции с Playwright.

---

### S3: AI-native QA (MCP, агенты)

| ID | Боль | Сообщение для аудитории |
|----|------|-------------------------|
| S3-P01 | Playwright MCP умеет действовать, не стабилизировать регресс | «Grounding layer для агента» |
| S3-P02 | Нет детерминированного audit tool calls | «Capture + assertions на порядок/аргументы» (F011) |
| S3-P03 | Смена модели ломает поведение незаметно | «Детерминированный отчёт по сценарию агента» |
| S3-P04 | LLM генерирует тесты по raw DOM | «Element map вместо скриншота/Dump» |

**One-liner сегмента:** Дай LLM структурированный доступ к UI — Fletta даёт агенту надёжные руки и глаза.

---

## Боли, которые Fletta сознательно не закрывает

Использовать как **anti-pitch** — чтобы не разочаровать ожидания.

| Ожидание | Реальность |
|----------|------------|
| «Напишите все e2e за нас» | Fletta даёт структуру и стабильность; сценарии — ваша команда или LLM снаружи |
| «Замените JUnit/TestNG/Playwright» | Integration, not replacement — раннер остаётся |
| «Почините API-тесты и БД» | RCA может указать сеть (roadmap), но execution layer — ваш |
| «Уберите flaky полностью» | Помогает отделить UI drift от таймингов; race conditions — отдельная дисциплина |
| «Как Testim из коробки без кода» | OSS engine + adapters; нужна интеграция в CI |

**Anti-personas:** см. [audience.md](./audience.md#anti-personas-не-ца-на-старте).

---

## Матрица: боль → возможность → доказательство

Для презентаций и демо — что показать live.

| Боль (ID) | Возможность Fletta | Демо / кейс |
|-----------|-------------------|-------------|
| P01, P04 | Structure discovery | `fletta discover`, C004 |
| P02, P03 | Self-healing + explainability | CP002, CP003, C001 |
| P05 | Deterministic, no ML | CP003 safe-fail, positioning vs Healenium |
| P06 | Unified context, RCA | C002, C003 (v1.1+) |
| P07 | Drift detection | analyze workflow (strategy Layer 3) |
| P08, S3-* | MCP grounding | C005, C007 (roadmap) |
| S1-P04 | CI export | CP005, integrations JUnit/Allure |

---

## Готовые блоки для каналов

### Сайт — секция «Проблема» (RU)

**Заголовок:** UI меняется быстрее, чем вы успеваете чинить тесты

- Команды тратят **часы** на разбор DOM и обновление XPath/CSS в Page Object.
- После рефакторинга фронта падает CI — непонятно, **что** изменилось и **можно ли** починить автоматически.
- Self-healing-инструменты часто **не объясняют** решение или требуют **облако и ML** — не проходят security в банке и enterprise.
- AI-агенты и codegen дают **хрупкий код**, без структурированной карты UI для maintainable тестов.

**Подзаголовок:** Fletta — не новый test framework

Мы не заменяем Playwright, Selenium или JUnit. Мы снимаем рутину **структуры UI, стабильных локаторов, healing при drift и audit в CI** — остальное остаётся в вашем стеке.

---

### Сайт — секция «Решение» (коротко)

| Было | Стало с Fletta |
|------|----------------|
| Ручной разбор DOM | Element map за секунды |
| Падение без объяснения | Confidence + diff в отчёте |
| ML / облако для healing | Deterministic core, on-prem |
| Сломанный селектор = стоп CI | Resolution при drift (с policy) |

---

### Презентация — открывающий слайд «3 боли»

1. **Время** — reverse engineering UI для каждой новой страницы.
2. **Доверие** — healing и AI без audit trail в regulated среде.
3. **Масштаб** — один рефакторинг UI → лавина падений в CI.

*Мост:* Fletta — structure engine и explainable resolution поверх вашего раннера.

---

### Конференция — hook (30 сек, устно)

> Сколько часов ваша команда потратила в прошлом квартале не на новые фичи, а на то, чтобы **найти правильный селектор** после очередного рефакторинга? Fletta не пишет тесты за вас — она даёт **структурированную карту UI**, **устойчивые идентификаторы** и **объяснимый healing**, который можно положить в JUnit XML и показать ИБ. Playwright и Selenium остаются; меняется только слой вокруг DOM.

---

### InSourceHub / внутренний питч (банк)

**Боль:** Legacy Selenium, дорогой POM, Healenium не проходит review.

**Ценность:** Open-source deterministic engine, adapter без смены тестов, healing events в GitLab/Jenkins artifacts.

**Не обещаем:** миграцию с Java, замену внутреннего test framework, облачные API.

---

## EN copy (website / conference abstract)

**Problem**

Teams spend hours reverse-engineering the DOM for Page Objects. UI refactors break CI; self-healing tools are often opaque or cloud/ML-dependent. AI agents lack a structured, auditable view of the UI.

**Scope**

Fletta is a **structure and locator layer** — not a test runner. It does not replace your data setup, API tests, environments, or orchestration.

**Solution**

Deterministic UI structure discovery, stable element identifiers, explainable resolution on drift, and CI-friendly audit — on-prem, NO ML in core.

---

## Как обновлять этот документ

1. Новая боль — добавить строку в таблицу с ID `Pxx` или `Sx-Pxx`.
2. Появилась фича — обновить колонку «Статус» и матрицу «боль → демо».
3. Не дублировать длинные тексты в `positioning.md` / `audience.md` — ссылаться сюда.

---

## Связанные документы

- [positioning.md](./positioning.md) — Problem, конкуренты, one-liner
- [audience.md](./audience.md) — сегменты S1–S3, ICP, anti-personas
- [messaging.md](./messaging.md) — возражения, demo script
- [integrations.md](./integrations.md) — как подключаем без замены раннера
- [talk-topics.md](./talk-topics.md) — темы докладов
- [index.md](./index.md) — демо-последовательности по аудитории
