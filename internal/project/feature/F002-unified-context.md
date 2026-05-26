# Feature: Unified Context (F002)

## Meta

- **Epic**: Core → Context Layer
- **Roll-up target**: ## v1.1.0 (Context Layer)
- **Status**: done
- **Target release**: v1.1.0
- **Created**: 2026-05-20
- **Related cases**: C002, C003

## Goal

Объединение трёх источников данных (UI, logs, network) в единый временной ряд событий для RCA.

## User workflow

1. Тест запускается с capture-all режимом
2. Система записывает: DOM-события, console logs, HTTP-запросы, WebSocket
3. При падении строится timeline событий ±5 сек от failure
4. Корреляция по trace_id и временным меткам
5. RCA-анализ использует timeline для классификации причины

## Scope

### In
- Перехват network: HTTP запросы/ответы, WebSocket
- Перехват logs: console, page errors
- Корреляция событий по времени и trace_id
- Timeline API: доступ к событиям в окне времени
- Сериализация в JSON для отчётов

### Out
- RCA классификация (в F003)
- Storage/Persistence (enterprise feature)
- Визуализация timeline (dashboard v2+)

## Acceptance criteria

- [x] Network capture: запросы/ответы с таймингами
- [x] Console capture: логи с уровнями (info, warn, error)
- [x] Timeline API: получить события в диапазоне [t-5s, t+5s]
- [x] Корреляция: trace_id связывает UI, network, logs
- [x] C002: timeline показывает API timeout перед UI failure
- [x] C003: timeline показывает различия между flaky runs
- [x] Performance overhead < 20%

## Implementation notes (sketch)

### Модули
```
crates/context/
├── src/
│   ├── network.rs    # HTTP/WebSocket event model
│   ├── logs.rs       # Console/application logs
│   ├── timeline.rs   # Timeline структура и API
│   └── correlation.rs # Trace ID matching

adapters/playwright/src/context/
├── capture.ts        # HTTP + WebSocket + console capture
├── store.ts          # JSONL buffer
└── index.ts
```

### Timeline структура
```rust
struct Timeline {
    events: Vec<Event>,
}

enum Event {
    Ui { timestamp, element, action },
    Network { timestamp, request, duration, protocol },
    Log { timestamp, level, message, source },
}
```

### Интеграция с Playwright
- HTTP: `page.on('request'|'response'|'requestfailed')`
- WebSocket: `page.on('websocket')` → open/message/close
- Консоль: `page.on('console')`, `page.on('pageerror')`
- Объединение в единый timeline через `attachFrapContext`

## Subtasks

### F002.0 — Cases & fixtures (C002, C003)

- **Цель**: воспроизводимые сценарии API timeout и flaky cart.
- **Файлы**: `test-app/context/`, `e2e/context/`, `project/cases/`
- **Готово когда**: C002/C003 статус `validated`.

### F002.1 — Event model + `crates/context`

- **Цель**: `Event`, `Timeline`, serde JSON schema.
- **Файлы**: `crates/context/`, `crates/Cargo.toml`
- **Готово когда**: `cargo test -p frapcode-context`, round-trip JSON.

### F002.2 — Network capture (Playwright)

- **Цель**: HTTP request/response + duration в timeline.
- **Файлы**: `adapters/playwright/src/context/capture.ts`
- **Готово когда**: в отчёте есть network events с таймингами.

### F002.3 — Console / logs capture

- **Цель**: `page.on('console')` → Log events с level.
- **Готово когда**: error/warn из консоли в timeline.

### F002.4 — Correlation + window API

- **Цель**: `trace_id`, выборка `[t-5s, t+5s]`.
- **Файлы**: `crates/context/src/correlation.rs`, SDK `getTimelineWindow`
- **Готово когда**: юнит-тесты корреляции; C002 показывает timeout перед UI fail.

### F002.5 — Report serialization + config

- **Цель**: `captureAll` / `frap-context.json` рядом с `frap-report.json`.
- **Готово когда**: C002/C003 acceptance; overhead замерен (< 20% per AC).

### F002.6 — WebSocket capture

- **Цель**: WebSocket open/message/close в timeline с `protocol: websocket`.
- **Файлы**: `network.rs`, `capture.ts`, `test-app/context/ws-cart.html`, `e2e/context/c004-websocket.spec.ts`
- **Готово когда**: C004 e2e проходит; WS events в `frap-context.json`.

| ID | Зависит от | Release |
|----|------------|---------|
| F002.0 | — | v1.1.0 |
| F002.1 | F002.0 (fixtures helpful) | v1.1.0 |
| F002.2–F002.3 | F002.1 | v1.1.0 |
| F002.4–F002.5 | F002.2, F002.3 | v1.1.0 |
| F002.6 | F002.2 | v1.1.0 |

## Verification / Test plan

### Local smoke
```bash
./scripts/start.sh
./scripts/build.sh
./scripts/test.sh context          # C002, C003, C004 + verify-context.mjs
./scripts/bench-context.sh         # overhead < 20%
cargo test -p frapcode-context       # Rust unit tests
```

### Expected
- C002: POST `/api/payment-intent` 504 **before** UI `not_found` (UI wait 10s, API delay 8s)
- C003: fast cart `<300ms`, slow cart `≥500ms` в одном timeline
- C004: WebSocket `open` + `message` events with `protocol: websocket`
- `e2e/frap-reports/context/frap-context.json` содержит network + log + ui

### Automation
- Rust: `cargo test -p frapcode-context` (correlation, window, JSON round-trip)
- E2E: `./scripts/test.sh context` + `e2e/context/verify-context.mjs`
- CI: job `e2e-context` в `.github/workflows/ci.yml`

## Related docs

- [F003-rca.md](./F003-rca.md) — RCA на основе timeline
- [cases.md](../../docs/cases.md) — C002, C003
