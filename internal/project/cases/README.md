# Кейсы Frap

Сценарии для демонстрации, тестирования и разработки.

## Conference demo (основной E2E)

| Ресурс | Описание |
|--------|----------|
| [conference/CASES.md](./conference/CASES.md) | Матрица `CONF-*` |
| [internal/testing/conference/README.md](../../internal/testing/conference/README.md) | Как запускать тесты и открывать отчёты |
| [internal/demo/site/conference/](../../internal/demo/site/conference/) | Статические страницы FixtureConf |

```bash
./scripts/test.sh conference
```

## Структура

```
project/cases/
├── README.md
├── conference/
│   └── CASES.md
├── C002-api-timeout.md
├── C003-flaky-cart.md
├── poc/               # Исторические CP001–CP005 (документация gates)
└── demo/              # Полные демо (C001–C009)
```

## PoC Cases (legacy CP IDs)

| ID | CONF-* | Статус |
|----|--------|--------|
| CP001 | CONF-PW-REG-PASS | migrated → Conference |
| CP002 | CONF-SH-SCHED-PASS | migrated → Conference |
| CP003 | CONF-SH-CFP-FAIL | migrated → Conference |
| CP004 | CONF-PW-SPK-PASS | partial |
| CP005 | CONF-RPT-RUN-PASS | partial |

Файлы `test-app/cp00x-*.html` и `e2e/cp00x-*.spec.ts` удалены; используйте Conference demo.

## Demo Cases (полные сценарии)

| ID | Название | Фичи | Статус |
|----|----------|------|--------|
| C001 | Payment Button | F001 | concept |
| C002 | API Timeout RCA | F002, F003 | [validated](./C002-api-timeout.md) |
| C003 | Flaky Diagnosis | F001, F002, F003 | [validated](./C003-flaky-cart.md) |
| C004 | Page Object Gen | F004 | concept |
| C005 | LLM Generation | F005 | concept |
| C006 | Mobile Self-Healing | F006 | concept |
| C007 | AI-Agent Audit | F011 | concept |
| C008 | Multi-Agent A2A | F011 | concept |
| C009 | CDP Recording & Playback | F001, F004, F008 | concept |

## Статусы

- `concept` — идея, не начато
- `script-ready` — скрипт готов
- `validated` — проверено

## Как добавить кейс Conference

1. Добавить страницу в `internal/demo/site/conference/` при необходимости
2. Добавить spec в `internal/testing/conference/` с ID `CONF-FEAT-AREA-OUTCOME`
3. Обновить [conference/CASES.md](./conference/CASES.md) и [internal/testing/conference/README.md](../../internal/testing/conference/README.md)
