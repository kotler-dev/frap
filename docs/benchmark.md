# PoC, MVP и Benchmark

Как **быстро проверить** работоспособность и **доказать качество** fletta: корневые кейсы, метрики, структура benchmark-репозитория, gates по фазам.

---

## Принцип

> Не «магия self-healing», а **измеримые** precision/recall на фиксированном наборе UI-диффов + **безопасный fail** при низком confidence.

---

## Conference demo (актуальный прогон)

Автоматизированные gates CP001–CP003 (и расширения) выполняются в проекте **FixtureConf**:

```bash
./scripts/test.sh conference
```

Кейсы `CONF-*`, отчёт: `e2e/fletta-reports/conference/`. См. [e2e/conference/README.md](../e2e/conference/README.md) и [project/cases/conference/CASES.md](../project/cases/conference/CASES.md).

---

## Корневые кейсы PoC (быстрая проверка)

Исторические ID `CP00N`. Реализация — в Conference demo (`CONF-*`).

### CP001: Happy path — primary селектор работает

**Цель:** Убедиться, что fletta **не ломает** стабильные тесты.

| Шаг | Действие |
|-----|----------|
| 1 | Тест с `[data-testid="pay-btn"]`, UI без изменений |
| 2 | `fletta` adapter включён |
| 3 | Прогон |

**Успех:** Healing **не вызывался**; latency overhead &lt; 10% vs baseline.

**Фичи:** F008  
**Время:** &lt; 30 мин настройки

---

### CP002: UI refactor — heal успешен (ядро ценности)

**Цель:** Доказать core self-healing (аналог C001, минимальный сценарий).

| Шаг | Действие |
|-----|----------|
| 1 | Записать/зафиксировать сценарий с `data-testid="pay-btn"` |
| 2 | Деплой версии с `data-testid="checkout-pay"` (тот же текст/роль) |
| 3 | Replay |

**Успех:**

- Тест **PASSED**
- В отчёте: `healed: true`, `confidence >= 0.85`, diff селектора
- Primary обновлён опционально (policy)

**Фичи:** F001, F008  
**Связь:** C001

---

### CP003: Безопасный fail — низкий confidence

**Цель:** Доказать отсутствие **ложного зелёного** клика.

| Шаг | Действие |
|-----|----------|
| 1 | Тот же сценарий CP002 |
| 2 | На странице **две** похожие кнопки «Оплатить» (ловушка) |
| 3 | Replay с `minConfidence=0.85` |

**Успех:**

- Тест **FAILED** (не healed)
- Отчёт: top-3 кандидата с scores, причина отказа
- Ни один клик по элементу с score &lt; порога

**Фичи:** F001 (policy)

---

### CP004: Playwright best practice — role locator

**Цель:** fletta **дополняет**, не конкурирует с `getByRole`.

| Шаг | Действие |
|-----|----------|
| 1 | Тест: `page.getByRole('button', { name: 'Оплатить' })` |
| 2 | Меняем только `data-testid`, role+name стабильны |

**Успех:** Тест проходит **без** healing (контроль).

| Шаг | Действие |
|-----|----------|
| 3 | Меняем видимый текст кнопки |
| 4 | Replay с fletta fallback на сигнатуру |

**Успех:** Healing срабатывает или корректный fail с объяснением.

---

### CP005: Экспорт и CI-артефакт

**Цель:** Интеграция в pipeline, не замена раннера.

| Шаг | Действие |
|-----|----------|
| 1 | Прогон CP002 с healing |
| 2 | Экспорт: JUnit XML / JSON report / Playwright trace attachment |

**Успех:**

- CI видит step `fletta-heal` с diff
- Артефакт скачивается из GitLab/Jenkins job

**Фичи:** F008 + export (см. [integrations.md](./integrations.md))

---

## Кейсы MVP (недели 2–3)

| ID | Название | Зачем | Связь |
|----|----------|-------|-------|
| C001 | Payment Button Refactor | Демо для людей | CP002 расширенный |
| C004 | Element map generation | Onboarding новых страниц | Export TS/Java |
| CP006 | Feedback learn | Одна ручная коррекция → лучший score | F009 |

---

## Кейсы v2 (не блокируют PoC)

| ID | Название | Gate |
|----|----------|------|
| C002 | API Timeout RCA | Не пытается heal UI при network timeout |
| C003 | Flaky diagnosis | Корреляция с latency |

---

## Benchmark repository (публичный или internal)

Структура репозитория `fletta-benchmark` (отдельно или `benchmarks/` в monorepo):

```
benchmarks/
├── apps/
│   ├── checkout-v1/          # data-testid=pay-btn
│   └── checkout-v2/          # data-testid=checkout-pay
├── scenarios/
│   ├── cp001-happy.json
│   ├── cp002-refactor.json
│   └── cp003-ambiguous.json
├── tests/
│   └── playwright/
│       └── payment.spec.ts
├── results/
│   └── .gitkeep              # CI commits JSON metrics
└── README.md
```

### Набор UI-диффов (целевой MVP benchmark)

| # | Тип изменения | Ожидание |
|---|---------------|----------|
| 1 | data-testid rename | heal |
| 2 | class rename (структура та же) | heal |
| 3 | wrapper div added | heal |
| 4 | i18n text change | heal или fail с report |
| 5 | duplicate similar buttons | **fail** (CP003) |

### Сравнение с baseline (обязательно в README benchmark)

| Вариант | Что меряем |
|---------|------------|
| Playwright only (role/testId) | % passed без healing |
| Playwright + fletta | % passed + heal count |
| (опционально) playwright-healer | heal count, API calls, determinism |

---

## Метрики

### PoC gates (неделя 1)

| Метрика | Порог |
|---------|-------|
| CP001–CP003 pass | 3/3 |
| False positive heals | **0** на CP003 |
| Reproducibility | 2 прогона CP002 → идентичный healed target + score |
| p95 overhead per step | &lt; 50ms (цель, уточнять на железе) |

### MVP gates (недели 2–3)

| Метрика | Порог |
|---------|-------|
| Benchmark UI-диффов | ≥ 4/5 heal, 1/5 safe fail |
| Precision (heal) | ≥ 95% на наборе |
| Recall (heal) | ≥ 80% на наборе (честно ниже на i18n) |
| Bank pilot teams | ≥ 1 команда, ≥ 3 сценария в CI |
| Time to integrate | &lt; 15 min по доке |

### v2 gates

| Метрика | Порог |
|---------|-------|
| C002 RCA | Correct class: `network_timeout` |
| C003 flaky | Correct endpoint correlation |

---

## Демо-приложение (минимум)

Docker Compose:

- `checkout-v1` — стабильная кнопка
- `checkout-v2` — renamed testid
- `checkout-ambiguous` — две кнопки «Оплатить»
- `backend-slow` — для C002 (v2)

Один URL, переключение версий через env `APP_VERSION`.

---

## Процесс прогона benchmark в CI

```yaml
# Псевдо-пipeline
- run: docker compose up -d checkout-v1
- run: npx playwright test
- run: docker compose up -d checkout-v2
- run: npx playwright test  # with @fletta/playwright
- run: node scripts/collect-metrics.js
- assert: metrics.precision >= 0.95
```

Артефакты: `results/run-<sha>.json`, JUnit для GitLab.

---

## Что показывать InSourceHub / митапу

1. **CP002** live: сломали → починили → diff на экране.
2. **CP003** live: ловушка → fail → top-3 candidates (безопасность).
3. Таблица метрик из последнего CI прогона benchmark.

---

## Связь с cases.md

| PoC | Полный кейс |
|-----|-------------|
| CP001–CP003 | C001 + политики |
| CP004 | — (специфичен для Playwright) |
| CP005 | C001 + export |
| CP006 | C001 + F009 |

Полные сценарии: [cases.md](./cases.md).

---

## Статус (обновлять вручную)

| Кейс | Статус |
|------|--------|
| CP001 | `concept` |
| CP002 | `concept` |
| CP003 | `concept` |
| CP004 | `concept` |
| CP005 | `concept` |

Статусы: `concept` → `implemented` → `validated` → `in-benchmark-ci`
