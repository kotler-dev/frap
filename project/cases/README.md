# Кейсы frap

Сценарии для демонстрации, тестирования и разработки. Публичный каталог: [docs/cases.md](../../docs/cases.md).

## Conference demo (основной E2E)

| Ресурс | Описание |
|--------|----------|
| [conference/CASES.md](./conference/CASES.md) | Матрица `CONF-*` |
| [e2e/conference/README.md](../../e2e/conference/README.md) | Как запускать тесты и открывать отчёты |
| [fixtures/fixtureconf/conference/](../../fixtures/fixtureconf/conference/) | Статические страницы FixtureConf |

```bash
./scripts/test.sh conference
```

## Структура

```
project/cases/
├── README.md
├── conference/CASES.md
├── C001-payment-button.md
├── C002-api-timeout.md          # validated
├── C003-flaky-cart.md           # validated
├── C004-page-object-gen.md
├── C005-llm-test-generation.md
├── C006-mobile-self-healing.md
├── C007-ai-agent-audit.md
├── C008-multi-agent-a2a.md
├── C009-recording-cdp.md
├── C010-structural-regression.md
```

## PoC Cases (legacy CP IDs)

| ID | CONF-* | Статус |
|----|--------|--------|
| CP001 | CONF-PW-REG-PASS | migrated → Conference |
| CP002 | CONF-SH-SCHED-PASS | migrated → Conference |
| CP003 | CONF-SH-CFP-FAIL | migrated → Conference |
| CP004 | CONF-PW-SPK-PASS | partial |
| CP005 | CONF-RPT-RUN-PASS | partial |

CP001–CP005 gates: [docs/benchmark.md](../../docs/benchmark.md).

## Demo Cases (C001–C010)

| ID | Название | Фичи | Статус |
|----|----------|------|--------|
| C001 | Payment Button | F001 | [concept](./C001-payment-button.md) |
| C002 | API Timeout RCA | F002, F003 | [validated](./C002-api-timeout.md) |
| C003 | Flaky Diagnosis | F001, F002, F003 | [validated](./C003-flaky-cart.md) |
| C004 | Page Object Gen | F004 | [concept](./C004-page-object-gen.md) |
| C005 | LLM Generation | F005 | [concept](./C005-llm-test-generation.md) |
| C006 | Mobile Self-Healing | F006 | [concept](./C006-mobile-self-healing.md) |
| C007 | AI-Agent Audit | F011 | [concept](./C007-ai-agent-audit.md) |
| C008 | Multi-Agent A2A | F011 | [concept](./C008-multi-agent-a2a.md) |
| C009 | CDP Recording & Playback | F001, F004, F008 | [concept](./C009-recording-cdp.md) |
| C010 | Structural Regression | F017 | [concept](./C010-structural-regression.md) |

## Статусы

- `concept` — идея, не начато
- `script-ready` — скрипт готов
- `validated` — проверено (E2E + fixtures)

## Как добавить кейс Conference

1. Добавить страницу в `fixtures/fixtureconf/conference/` при необходимости
2. Добавить spec в `e2e/conference/` с ID `CONF-FEAT-AREA-OUTCOME`
3. Обновить [conference/CASES.md](./conference/CASES.md) и [e2e/conference/README.md](../../e2e/conference/README.md)

## Legend

| Поле | Описание |
|------|----------|
| ID | C001… / CP001… / CONF-* |
| Фичи | [project/feature/](../feature/) |
| Сценарий | Пошаговое описание в файле кейса |
| Проверка успеха | Критерии в файле кейса + E2E при `validated` |
