# Conference demo — руководство по тестированию

Локальный demo-проект **FixtureConf 2026 Spring** (статические страницы в `test-app/conference/`). Вымышленные спикеры и программа, не связаны с реальными конференциями.

Один Playwright-проект → один каталог отчётов: `e2e/fletta-reports/conference/`.

## Предусловия

```bash
./scripts/build.sh
./scripts/start.sh          # http://localhost:3000
```

## Запуск

| Команда | Что делает |
|---------|------------|
| `./scripts/test.sh conference` | Полный набор CONF-* + `verify-reports.mjs` (CONF-RPT-RUN-PASS) |
| `./scripts/test.sh conference-fail` | Только кейсы с `-FAIL` + проверка RPT |
| `./scripts/test.sh conference-dbg` | Кейсы с debug / healing |
| `./scripts/test.sh conference-single` | Один debug-тест → explorer stub |
| `./scripts/test.sh debug` | F012 unit-style specs (`debug-mode.spec.ts`) |

Из `e2e/` напрямую:

```bash
npx playwright test --config=playwright.conference.config.ts
npx playwright test --config=playwright.conference.config.ts --grep 'CONF-SH-SCHED-PASS'
```

## Артефакты

| Файл | Описание |
|------|----------|
| `fletta-reports/conference/fletta-debug.html` | Classic view (A): индекс или один отчёт |
| `fletta-reports/conference/fletta-debug-explorer.html` | Explorer (B): sidebar при 2+ debug-тестах; stub при одном |
| `fletta-reports/conference/debug-reports/*.html` | Детальные debug-отчёты |
| `fletta-reports/conference/fletta-report.json` | Сводка healing events |
| `fletta-reports/conference/junit.xml` | JUnit с `<healing>` |
| `fletta-reports/conference/fletta-events.jsonl` | Поток healing events (проверяется в `zzz-reporting.spec.ts`) |

`fletta-report.json` / `junit.xml` пишутся в `reporter.onEnd` **после** завершения Playwright — полная проверка CONF-RPT-RUN-PASS в `conference/verify-reports.mjs` (вызывается из `./scripts/test.sh conference`).

Открыть после прогона:

```bash
open e2e/fletta-reports/conference/fletta-debug.html
open e2e/fletta-reports/conference/fletta-debug-explorer.html
```

## ID кейсов

Формат: `CONF-{FEAT}-{AREA}-{OUTCOME}`

- **FEAT:** `SH` (healing), `PW` (adapter), `DBG` (debug), `POL` (policy), `RPT` (reporting)
- **AREA:** `SCHED`, `REG`, `CFP`, `SPK`, `NAV`

Полная матрица: [project/cases/conference/CASES.md](../../project/cases/conference/CASES.md).

## Как читать отчёт

- Группы в Classic A = вложенные `test.describe` (`Conference 2026 Spring > Schedule > …`).
- Теги success / warning / failure — исход healing.
- В деталях: `← All reports`, Prev/Next между debug-тестами.
- Только тесты с `debug: true` попадают в debug manifest.

## Страницы demo

| URL | Назначение |
|-----|------------|
| `/conference/index.html` | Хаб |
| `/conference/schedule-v1.html` | Стабильные `talk-open-*` |
| `/conference/schedule-heal.html` | Один доклад для heal (v2 testid) |
| `/conference/schedule-v2.html` | Refactor → `talk-card-open-*` |
| `/conference/register.html` | Регистрация |
| `/conference/cfp.html` | CFP, ambiguous кнопки |
| `/conference/speakers.html` | Role locators |
| `/conference/speaker.html?id=alexey` | Профиль спикера (не путать с `talk.html`) |
