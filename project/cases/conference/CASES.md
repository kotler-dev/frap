# Conference demo — каталог кейсов (CONF-*)

Проект: **FixtureConf 2026 Spring** · отчёты: `internal/testing/frap-reports/conference/`

Миграция PoC: CP001 → `CONF-PW-REG-PASS`, CP002 → `CONF-SH-SCHED-PASS`, CP003 → `CONF-SH-CFP-FAIL`.

## Легенда FEAT

| Код | Фича |
|-----|------|
| SH | F001 Self-healing |
| PW | F008 Playwright adapter |
| DBG | F012 Debug trace |
| POL | F008 semantics (`healPolicy`) |
| RPT | F008 reporting |

## Матрица

### SH — Self-healing

| ID | Spec | Страница | Config | Ожидание |
|----|------|----------|--------|----------|
| CONF-SH-SCHED-PASS | schedule.spec | schedule-heal | `expect_heal`, debug, minConf 0.7 | heal → PASS |
| CONF-SH-CFP-FAIL | cfp.spec | cfp | debug, minConf 0.85 | ambiguous / FAIL, top candidates |
| CONF-SH-REG-FAIL | registration.spec | register | debug, minConf 0.95 | heal FAIL (нет подходящего) |

### PW — Adapter

| ID | Spec | Страница | Config | Ожидание |
|----|------|----------|--------|----------|
| CONF-PW-REG-PASS | registration.spec | register | debug | клик без healing |
| CONF-PW-SPK-PASS | speakers.spec | speakers | — | `getByRole`, без frap |
| CONF-PW-NAV-FAIL | navigation.spec | index | debug | сломанный селектор, FAIL |

### DBG — Debug

| ID | Spec | Config | Ожидание |
|----|------|--------|----------|
| CONF-DBG-MULTI-PASS | полный `conference` | несколько `debug: true` | index A + explorer B |
| CONF-DBG-SINGLE-PASS | dbg-single.spec | один `debug: true` | explorer stub |

### POL — Policy

| ID | Spec | Config | Ожидание |
|----|------|--------|----------|
| CONF-POL-REG-PASS | registration | `healPolicy: deny`, stable | no heal |
| CONF-POL-SCHED-WARN | schedule.spec | schedule-heal | `healPolicy: deny`, forced heal | `unexpected_heal` |

### RPT — Reporting

| ID | Spec | Ожидание |
|----|------|----------|
| CONF-RPT-RUN-PASS | `zzz-reporting.spec.ts` + `verify-reports.mjs` | events в spec; JSON/JUnit после `onEnd` в test.sh |
| CONF-RPT-RUN-FAIL | reporting | после `conference-fail`: rejected/unexpected > 0 |

## Success criteria (кратко)

- **PASS:** тест зелёный, отчёт отражает исход (healed / no healing / semantics).
- **FAIL:** тест падает намеренно, в debug — причина и top candidates.
- **WARN:** `unexpected_heal` при `healPolicy: deny`.

## Связь с benchmark

Gates CP001–CP003 покрываются кейсами CONF-* в этом проекте. См. [docs/benchmark.md](../../../docs/benchmark.md).
