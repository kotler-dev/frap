# Conference demo — руководство по тестированию

Локальный demo-проект **FixtureConf 2026 Spring** (статические страницы в `internal/demo/site/conference/`). Вымышленные спикеры и программа, не связаны с реальными конференциями.

Один Playwright-проект → один каталог отчётов: `internal/testing/frap-reports/conference/`.

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

Из `internal/testing/` напрямую:

```bash
cd internal/testing && npm install
npx playwright test --config=playwright.conference.config.ts
npx playwright test --config=playwright.conference.config.ts --grep 'CONF-SH-SCHED-PASS'
```

## Артефакты

| Файл | Описание |
|------|----------|
| `internal/testing/frap-reports/conference/frap-debug.html` | Classic view (A): индекс или один отчёт |
| `internal/testing/frap-reports/conference/frap-debug-explorer.html` | Explorer (B): sidebar при 2+ debug-тестах; stub при одном |
| `internal/testing/frap-reports/conference/debug-reports/*.html` | Детальные debug-отчёты |
| `internal/testing/frap-reports/conference/frap-report.json` | Сводка healing events |
| `internal/testing/frap-reports/conference/junit.xml` | JUnit с `<healing>` |
| `internal/testing/frap-reports/conference/frap-events.jsonl` | Поток healing events (проверяется в `zzz-reporting.spec.ts`) |

`frap-report.json` / `junit.xml` пишутся в `reporter.onEnd` **после** завершения Playwright — полная проверка CONF-RPT-RUN-PASS в `conference/verify-reports.mjs` (вызывается из `./scripts/test.sh conference`).

Время в заголовке debug HTML — через системную команду `date` (локальная TZ, как в терминале); в JSON-артефактах — ISO UTC (`Z`).

Открыть после прогона:

```bash
open internal/testing/frap-reports/conference/frap-debug.html
open internal/testing/frap-reports/conference/frap-debug-explorer.html
```

## ID кейсов

Формат: `CONF-{FEAT}-{AREA}-{OUTCOME}`

- **FEAT:** `SH` (healing), `PW` (adapter), `DBG` (debug), `POL` (policy), `RPT` (reporting)
- **AREA:** `SCHED`, `REG`, `CFP`, `SPK`, `NAV`

Полная матрица: [internal/project/cases/conference/CASES.md](../../project/cases/conference/CASES.md).

## Как читать отчёт

- Группы в Classic A = вложенные `test.describe` (`Conference 2026 Spring > Schedule > …`).
- Теги success / warning / failure — исход healing.
- В деталях: `← All reports`, Prev/Next между debug-тестами.
- Только тесты с `debug: true` попадают в debug manifest.

## Пример кластера

Для кейса `CONF-SH-REG-FAIL` из `registration.spec.ts` (селектор `[data-testid="register-pay-legacy"]`) можно ожидать кластер такого вида:

```json
{
  "id": "cluster_registration_actions",
  "prefix": "register-",
  "element_count": 3,
  "elements": [
    {
      "selector": "[data-testid=\"register-submit\"]",
      "signature_preview": "button[data-testid=register-submit]",
      "text_content": "Register"
    },
    {
      "selector": "[data-testid=\"register-cancel\"]",
      "signature_preview": "button[data-testid=register-cancel]",
      "text_content": "Cancel"
    },
    {
      "selector": "[data-testid=\"register-email\"]",
      "signature_preview": "input[data-testid=register-email]",
      "text_content": ""
    }
  ]
}
```

Это учебный пример структуры (`id`, `prefix`, `element_count`, `elements`) в формате debug-модели, а не зафиксированный снимок конкретного прогона.

Пример связи кластера с решением (`healed/rejected`) через `top_candidates` и `confidence`:

```json
{
  "healing": {
    "healed": false,
    "selector": "[data-testid=\"register-pay-legacy\"]",
    "confidence": 0.72,
    "top_candidates": [
      {
        "selector": "[data-testid=\"register-submit\"]",
        "confidence": 0.72
      },
      {
        "selector": "[data-testid=\"register-cancel\"]",
        "confidence": 0.41
      }
    ]
  },
  "policy": {
    "min_confidence": 0.95,
    "result": "rejected"
  }
}
```

В этом примере лучший кандидат найден внутри кластера, но порог `min_confidence` не пройден, поэтому итог — `healed: false` и `result: rejected`.

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

## Cross-language contract: `id → data-id`

Общие фикстуры и ожидания для TS/Java/Rust:

| Asset | Path |
|-------|------|
| Fixture (HTML + JSON) | [fixtures/contract/clustering-id-migration](../../../fixtures/contract/clustering-id-migration/) |
| Playwright e2e | [clustering-id-migration.spec.ts](./clustering-id-migration.spec.ts) |
| Rust integration | `crates/core/tests/contract_clustering_id_migration.rs` |
| TypeScript (Jest) | `sdk/typescript/src/contract/clustering-id-migration.contract.test.ts` |
| Java (JUnit) | `ClusteringIdMigrationContractTest.java` |

Проверяются контрактные поля (`healed`, `confidence`, `top_candidates`), а не точная сериализация селектора SDK.

Запуск одного e2e-кейса:

```bash
cd internal/testing
npx playwright test --config=playwright.conference.config.ts --grep "CONF-CL-REG-PASS"
```

| Presentation slide | [internal/demo/index.html](../../demo/index.html) (slide «id → data-id») |
| Golden reports (generated) | `internal/testing/frap-reports/conference/` |
