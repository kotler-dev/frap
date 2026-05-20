# Feature: Unified Context (F002)

## Meta

- **Epic**: Core → Context Layer
- **Roll-up target**: ## v1.1.0 (Context Layer)
- **Status**: draft
- **Target release**: v1.1.0
- **Created**: 2026-05-20
- **Related cases**: C002, C003

## Goal

Объединение трёх источников данных (UI, logs, network) в единый временной ряд событий для RCA.

## User workflow

1. Тест запускается с capture-all режимом
2. Система записывает: DOM-события, console logs, HTTP-запросы
3. При падении строится timeline событий ±5 сек от failure
4. Корреляция по trace_id и временным меткам
5. RCA-анализ использует timeline для классификации причины

## Scope

### In
- Перехват network: HTTP запросы/ответы, WebSocket
- Перехват logs: console, application logs
- Корреляция событий по времени и trace_id
- Timeline API: доступ к событиям в окне времени
- Сериализация в JSON для отчётов

### Out
- RCA классификация (в F003)
- Storage/Persistence (enterprise feature)
- Визуализация timeline (dashboard v2+)

## Acceptance criteria

- [ ] Network capture: запросы/ответы с таймингами
- [ ] Console capture: логи с уровнями (info, warn, error)
- [ ] Timeline API: получить события в диапазоне [t-5s, t+5s]
- [ ] Корреляция: trace_id связывает UI, network, logs
- [ ] C002: timeline показывает API timeout перед UI failure
- [ ] C003: timeline показывает различия между flaky runs
- [ ] Performance overhead < 20%

## Implementation notes (sketch)

### Модули
```
crates/context/
├── src/
│   ├── network.rs    # HTTP/WebSocket capture
│   ├── logs.rs       # Console/application logs
│   ├── timeline.rs   # Timeline структура и API
│   └── correlation.rs # Trace ID matching
```

### Timeline структура
```rust
struct Timeline {
    events: Vec<Event>,
}

enum Event {
    Ui { timestamp, element, action },
    Network { timestamp, request, response, duration },
    Log { timestamp, level, message, source },
}
```

### Интеграция с Playwright
- Playwright уже имеет network interception
- Консоль: `page.on('console', ...)`
- Объединение в единый timeline через адаптер

### Риски и зависимости
- Производительность: полный capture может быть тяжёлым
- Privacy: логи могут содержать чувствительные данные
- Объём данных: нужна агрегация/сэмплирование

## Verification / Test plan

### Manual smoke
```bash
# C002: API Timeout RCA
fletta replay --name "payment-flow" --capture-all
# Expected: timeline содержит POST /api/payment-intent TIMEOUT перед UI failure

# C003: Flaky diagnosis
for i in {1..10}; do
  fletta replay --name "cart-flow" --capture-all
done
fletta analyze --aggregate --name "cart-flow"
# Expected: корреляция с /api/cart latency > 500ms
```

### Automation
- Юнит-тесты: корреляция событий
- Интеграционные: C002, C003

## Related docs

- [F003-rca.md](./F003-rca.md) — RCA на основе timeline
- [cases.md](../../docs/cases.md) — C002, C003
