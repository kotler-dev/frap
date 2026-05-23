# Feature: Unified Context (F002)

## Meta

- **Epic**: Core → Context Layer
- **Roll-up target**: ## v1.1.0 (Context Layer)
- **Status**: in-progress
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

## Subtasks

### F002.0 — Cases & fixtures (C002, C003)

- **Цель**: воспроизводимые сценарии API timeout и flaky cart.
- **Файлы**: `test-app/`, `e2e/conference/` (или `e2e/c002-*`), `project/cases/`, `docs/cases.md`
- **Готово когда**: C002/C003 статус `script-ready`, spec падает предсказуемо.

### F002.1 — Event model + `crates/context`

- **Цель**: `Event`, `Timeline`, serde JSON schema.
- **Файлы**: `crates/context/Cargo.toml`, `crates/context/src/{timeline,lib}.rs`, `crates/Cargo.toml`
- **Готово когда**: `cargo test -p fletta-context`, round-trip JSON.

### F002.2 — Network capture (Playwright)

- **Цель**: HTTP request/response + duration в timeline.
- **Файлы**: `adapters/playwright/src/context/` (или `network-capture.ts`), hook в `wrapper.ts` / reporter
- **Готово когда**: в отчёте есть network events с таймингами.

### F002.3 — Console / logs capture

- **Цель**: `page.on('console')` → Log events с level.
- **Файлы**: те же + типы в SDK при необходимости
- **Готово когда**: error/warn из консоли в timeline.

### F002.4 — Correlation + window API

- **Цель**: `trace_id`, выборка `[t-5s, t+5s]`.
- **Файлы**: `crates/context/src/correlation.rs`, TS API `getTimelineWindow(failureAt)`
- **Готово когда**: юнит-тесты корреляции; C002 показывает timeout перед UI fail.

### F002.5 — Report serialization + config

- **Цель**: `captureAll` / `fletta-context.json` рядом с `fletta-report.json`.
- **Файлы**: `adapters/playwright/src/reporter.ts`, `FlettaConfig`
- **Готово когда**: C002/C003 acceptance; overhead замерен (< 20% per AC).

| ID | Зависит от | Release |
|----|------------|---------|
| F002.0 | — | v1.1.0 |
| F002.1 | F002.0 (fixtures helpful) | v1.1.0 |
| F002.2–F002.3 | F002.1 | v1.1.0 |
| F002.4–F002.5 | F002.2, F002.3 | v1.1.0 |

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
