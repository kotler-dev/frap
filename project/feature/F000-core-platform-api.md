# Feature: Core Platform API (F000)

## Meta

- **Epic**: Core → Platform API
- **Roll-up target**: ## MVP v1.0.0
- **Status**: done (P0 WASM); P1 FFI → v1.4.0
- **Target release**: v1.0.0
- **Created**: 2026-05-22
- **Related cases**: CP001, CP002, CP003, F001, F008, F006

## Goal

Публичный API для standalone использования Rust Core без адаптеров. FFI, WASM, JSON-RPC интерфейсы для интеграции с произвольными фреймворками и языками программирования.

## User workflow

1. Пользователь хочет использовать frap в проекте на языке/фреймворке без официального адаптера
2. Пользователь интегрирует Core через один из доступных интерфейсов (WASM, FFI, JSON-RPC)
3. Пользователь получает DOM/ViewTree snapshot от своего фреймворка
4. Пользователь вызывает Core API для healing/анализа
5. Пользователь применяет результат к своему тестовому сценарию

## Scope

### In
- Rust публичный API (crates/signature/, crates/clustering/, crates/healing/)
- WASM bindings для Node.js/Deno/Browser
- FFI: C-header для нативной компиляции (cbindgen)
- JSON-RPC через stdin/stdout для внешних процессов
- Документация: как написать custom adapter
- Примеры standalone использования (CLI/скрипт)

### Out
- Конкретные адаптеры (это F008 Playwright, F006 Java/Selenium, и т.д.)
- Генерация кода под языки (backlog)
- Графический интерфейс

## Phased delivery

| Фаза | Release | Суть |
|------|---------|------|
| **P0 — WASM + Rust API** | v1.0.0 | `crates/core`, wasm-bindgen, SDK вызывает Core; закрывает F000/F013/F001 для Playwright |
| **P1 — FFI** | v1.4.0 | cdylib + cbindgen + C-header; блок для Java/JNI (F014) |
| **P2 — JSON-RPC CLI** | v1.4.0 / backlog | subprocess для Python (F015), CI без Node |
| **P3 — Standalone docs & examples** | v1.4.0+ | полный guide custom adapter, примеры вне Playwright |

Подзадачи P0–P3 — см. раздел **Subtasks** ниже.

## Acceptance criteria

### P0 — v1.0.0 MVP (критично)

- [x] `crates/core` — публичный Rust API (re-export signature / clustering / healing)
- [x] WASM: `wasm-pack build`, `healJson` → JSON (`crates/core/src/wasm.rs`)
- [x] CI: сборка WASM + `cargo test` для core
- [x] `@frap/frap-sdk`: `HealingEngine` вызывает WASM (`FLETTA_TS_FALLBACK=1` для dev)
- [x] Conference E2E (CP001–CP005 gates) на WASM-пути

### P1 — v1.4.0 Java (отложено)

- [ ] Core как `cdylib` / `staticlib` (feature `ffi`)
- [ ] C-header через cbindgen (`crates/core/frap.h`)

### P2 — JSON-RPC (Java 1.0.0)

- [x] Бинарь `frap-core-rpc` (stdin/stdout NDJSON)
- [x] Methods: `heal`, `analyze_rca`, `build_element_map`, `filter_element_map`, `generate_page_object`
- [x] Integration tests: Rust contracts + Java `FrapCoreClientTest`

### P3 — позже (отложено)

- [ ] Документация: "How to write custom adapter" (полная)
- [ ] Примеры standalone (CLI/скрипт без Playwright)
- [ ] wasm-opt / размер бандла

## Subtasks

### P0 — v1.0.0 (критичный путь MVP)

#### P0.1 — `crates/core` (Rust public API)

- **Цель**: единая точка входа в алгоритмы; типы heal in/out (serde).
- **Файлы**: `crates/core/Cargo.toml`, `crates/core/src/lib.rs`, `crates/Cargo.toml`, `crates/healing/Cargo.toml` (deps)
- **Готово когда**: `cargo test -p frap-core` (или имя crate), юнит-тесты heal на фикстурах DOM.

#### P0.2 — WASM bindings

- **Цель**: `wasm-bindgen` обёртки поверх `crates/core`.
- **Файлы**: `crates/core/src/wasm.rs`, `crates/core/Cargo.toml` (feature `wasm`), скрипт/README: `wasm-pack build --target bundler`
- **Готово когда**: артефакт в `sdk/typescript/wasm/` (или documented out-dir), smoke: heal из Node.

#### P0.3 — SDK на WASM

- **Цель**: убрать дублирование алгоритмов в TS; один источник истины — Rust.
- **Файлы**: `sdk/typescript/src/core.ts`, `sdk/typescript/package.json`, `sdk/typescript/README.md` (краткий API)
- **Готово когда**: `npm run build`, e2e CP001–CP003 без регрессий.

#### P0.4 — CI для Core + WASM

- **Цель**: сборка и тесты в pipeline.
- **Файлы**: `.github/workflows/ci.yml`, опционально `crates/core/README.md`
- **Готово когда**: green CI на PR с Rust + TS + e2e.

**Связанные подзадачи MVP (не F000, но блокируют ✅ релиза):**

| ID | Фича | Цель | Файлы | Release |
|----|------|------|-------|---------|
| MVP-A | F008 | CP005: JUnit XML артефакт в CI | `adapters/playwright/src/reporter.ts`, `e2e/`, `.github/workflows/ci.yml` | v1.0.0 |
| MVP-B | F001/F013 | Статус ✅ после P0.3 + MVP-A | `project/FEATURES.md`, карточки F001/F013 | v1.0.0 |
| MVP-C | PoC gates | Все CP001–CP005 в CI, overhead &lt; 10% (бенчмарк) | `docs/benchmark.md`, `e2e/`, CI | v1.0.0 или v1.0.1 |

### P1 — v1.4.0 (FFI для Java)

#### P1.1 — FFI layer + cbindgen

- **Файлы**: `crates/core/src/ffi.rs`, `crates/core/cbindgen.toml`, generated `frap.h`
- **Готово когда**: `cargo build --features ffi`, smoke C или bindgen test.

### P2 — v1.4.0 / backlog (JSON-RPC)

#### P2.1 — RPC subprocess

- **Файлы**: `crates/core/src/bin/rpc.rs` (или `crates/cli/`), `project/feature/F015-*.md`
- **Готово когда**: integration test stdin/stdout; Python SDK может вызывать без WASM.

### P3 — позже

- Полный `docs/integrations.md` / adapter guide, standalone examples, wasm-opt.

## Implementation notes (sketch)

### Структура Core
```
crates/
├── signature/          # DOM signature extraction
├── clustering/         # Drain3 algorithm for DOM
├── healing/            # Self-healing fallback chain
└── core/               # F000: Public API aggregation
    ├── src/
    │   ├── lib.rs      # Public exports
    │   ├── wasm.rs     # WASM bindings (wasm-bindgen)
    │   └── ffi.rs      # FFI C-API (cbindgen)
    └── cbindgen.toml
```

### Интерфейсы

#### WASM Bindings
```rust
#[wasm_bindgen]
pub fn heal_selector(
    dom_json: &str,
    original_selector: &str,
    original_signature: &str,
    min_confidence: f64,
) -> String; // JSON с результатом
```

#### FFI (C-API)
```c
// frap.h (generated by cbindgen)
typedef struct FlettaHealingResult {
    bool healed;
    const char* selector;
    double confidence;
    const char* diff;
} FlettaHealingResult;

FlettaHealingResult* frap_heal(
    const char* dom_json,
    const char* original_selector,
    const char* original_signature,
    double min_confidence
);

void frap_free_result(FlettaHealingResult* result);
```

#### JSON-RPC
```bash
# stdin → stdout
{"method": "heal", "params": {"dom": "...", "selector": "...", "min_confidence": 0.85}}
# → {"result": {"healed": true, "selector": "...", "confidence": 0.92}}
```

### Зависимости
- `wasm-bindgen` для WASM
- `cbindgen` для C-headers
- `serde_json` для JSON-RPC

### Риски
- Размер WASM бандла → оптимизация через wasm-opt
- FFI safety → тщательная работа с raw pointers
- Производительность JSON-RPC → streaming JSON parser

## Verification / Test plan

### Manual smoke
```bash
# 1. Собрать Core как cdylib
cd crates && cargo build --release --features ffi

# 2. Собрать WASM
wasm-pack build --target bundler

# 3. Запустить JSON-RPC echo
./frap-core rpc < test_input.json

# 4. Проверить C-header
test -f crates/core/frap.h
```

### Automation
- Unit tests для каждого интерфейса
- Integration test: CLI → JSON-RPC → результат
- WASM test в headless Chrome

## Related docs

- [F001: Self-Healing Selectors](./F001-self-healing.md) — Core algorithms
- [F008: Playwright Adapter](./F008-playwright-adapter.md) — Reference adapter implementation
- [Architecture: Core](../../project/architecture/README.md)
- [Integration guide](../../docs/integrations.md)
