# Feature: Root Cause Analysis (F003)

## Meta

- **Epic**: Feature → Analysis
- **Roll-up target**: ## v1.1.0 (Context Layer)
- **Status**: draft
- **Target release**: v1.1.0
- **Created**: 2026-05-20
- **Related cases**: C002, C003

## Goal

Автоматическое определение первопричины падения теста на основе Unified Context timeline.

## User workflow

1. Тест падает
2. fletta анализирует timeline событий ±5 сек от failure
3. Классификация причины: UI-изменение | API-ошибка | Инфраструктура | Flaky
4. RCA-репорт с обоснованием классификации
5. Для LLM-агентов: структурированный JSON для принятия решений

## Scope

### In
- Классификатор причин падения
- Анализ timeline: корреляция UI failure с network/logs
- RCA-репорт: причина, обоснование, рекомендации
- JSON API для LLM-агентов
- Экспорт в отчёты (JUnit, Allure)

### Out
- Автоматическое исправление (это фидбек-цикл, в F009)
- Исполнение рекомендаций (агент решает)

## Acceptance criteria

- [ ] Классификация: UI-change, API-error, Infrastructure, Flaky, Unknown
- [ ] C002: при API timeout классифицируется как API-error
- [ ] C003: при flaky тесте классифицируется как Flaky с паттерном
- [ ] RCA-репорт содержит: primary cause, confidence, timeline excerpt, recommendation
- [ ] JSON формат для MCP: `fletta/analyze` tool
- [ ] Экспорт в JUnit: `<failure message="...">` с RCA

## Implementation notes (sketch)

### Модули
```
crates/rca/
├── src/
│   ├── classifier.rs    # Классификация причин
│   ├── rules.rs         # Правила классификации
│   ├── report.rs        # Генерация RCA-репорта
│   └── mcp.rs           # JSON-RPC API
```

### Правила классификации
```rust
enum RootCause {
    UiChange { diff: ElementDiff },
    ApiError { endpoint: String, status: u16 },
    Infrastructure { component: String },
    Flaky { pattern: String, correlation: f64 },
    Unknown,
}
```

### Алгоритм
1. Получить timeline событий в окне [failure_time - 5s, failure_time]
2. Проверить network: были ли ошибки/таймауты?
3. Проверить logs: были ли error/critical?
4. Проверить UI: изменилась ли сигнатура элемента?
5. Если несколько кандидатов — выбрать наиболее вероятный
6. Вернуть RootCause с confidence

### MCP API
```json
{
  "method": "fletta/analyze",
  "params": { "run_id": "..." },
  "result": {
    "primary_cause": "api_error",
    "confidence": 0.92,
    "details": { "endpoint": "/api/payment", "status": 504 },
    "recommendation": "Check backend latency"
  }
}
```

### Риски и зависимости
- Зависит от F002 (Unified Context)
- Точность классификации: ложные срабатывания
- Сложные случаи: race conditions, cascading failures

## Verification / Test plan

### Manual smoke
```bash
# C002: API Timeout
fletta replay --name "payment-flow" --capture-all
# Test FAILED

fletta analyze --run-id <id>
# Expected: primary_cause = "api_error", endpoint = "/api/payment-intent"

# C003: Flaky
fletta analyze --aggregate --name "cart-flow" --runs 10
# Expected: primary_cause = "flaky", pattern = "api_cart_latency > 500ms"
```

### Automation
- Юнит-тесты: классификация по правилам
- Интеграционные: C002, C003
- Тесты на ложные срабатывания

## Related docs

- [F002-unified-context.md](./F002-unified-context.md) — timeline для RCA
- [cases.md](../../docs/cases.md) — C002, C003
