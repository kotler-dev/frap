# Cases

Каталог сценариев для демонстрации, тестирования и разработки.

**Источник правды:** [`project/cases/`](../project/cases/) — полные спеки по каждому ID.

---

## Conference demo (CONF-*)

**FixtureConf 2026 Spring** — основной E2E. Прогон: `./scripts/test.sh conference`.

| Ресурс | Ссылка |
|--------|--------|
| Матрица кейсов | [project/cases/conference/CASES.md](../project/cases/conference/CASES.md) |
| Запуск и отчёты | [e2e/conference/README.md](../e2e/conference/README.md) |
| Demo HTML | [fixtures/fixtureconf/conference/](../fixtures/fixtureconf/conference/) |

| Было (CP) | Стало (CONF) |
|-----------|----------------|
| CP001 | CONF-PW-REG-PASS |
| CP002 | CONF-SH-SCHED-PASS |
| CP003 | CONF-SH-CFP-FAIL |

---

## PoC / benchmark (CP001–CP005)

Исторические gate ID; реализация в Conference demo. Детали: [benchmark.md](./benchmark.md).

---

## Scenario cases (C001–C010)

| ID | Название | Spec | Статус |
|----|----------|------|--------|
| C001 | Payment Button Refactor | [C001-payment-button.md](../project/cases/C001-payment-button.md) | concept |
| C002 | API Timeout RCA | [C002-api-timeout.md](../project/cases/C002-api-timeout.md) | validated |
| C003 | Flaky Diagnosis | [C003-flaky-cart.md](../project/cases/C003-flaky-cart.md) | validated |
| C004 | Page Object Generation | [C004-page-object-gen.md](../project/cases/C004-page-object-gen.md) | concept |
| C005 | LLM Test Generation | [C005-llm-test-generation.md](../project/cases/C005-llm-test-generation.md) | concept |
| C006 | Mobile Self-Healing | [C006-mobile-self-healing.md](../project/cases/C006-mobile-self-healing.md) | concept |
| C007 | AI-Agent Tool Call Audit | [C007-ai-agent-audit.md](../project/cases/C007-ai-agent-audit.md) | concept |
| C008 | Multi-Agent A2A | [C008-multi-agent-a2a.md](../project/cases/C008-multi-agent-a2a.md) | concept |
| C009 | CDP Recording & Playback | [C009-recording-cdp.md](../project/cases/C009-recording-cdp.md) | concept |
| C010 | Structural Regression | [C010-structural-regression.md](../project/cases/C010-structural-regression.md) | concept |

Полная таблица и legend: [project/cases/README.md](../project/cases/README.md).
