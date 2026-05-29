# SDK Strategy: TypeScript, Java, Python

## Status

Accepted (target architecture). Реализация по языкам — поэтапно; TypeScript — reference.

## Принцип

**Тонкие SDK + толстое ядро.** Все языковые пакеты вызывают один и тот же Rust Core; различаются только transport и hooks фреймворка.

## Decision rationale

Почему выбран `Rust Core + thin SDK` вместо language-only реализации:

- Один алгоритмический код для TS/Java/Python без расхождения поведения healing/scoring/clustering.
- Explainable parity: одинаковые `confidence/diff/candidates` во всех интеграциях.
- On-prem portability: один Core собирается как WASM и native артефакты.
- SDK остаются интеграционным слоем (hooks, transport, reports), а не местом для бизнес-логики алгоритмов.

Это решение зафиксировано в [ADR-001](./ADR-001-core-language-strategy.md).

```
┌─────────────────────────────────────────────────────────────┐
│  Framework hooks (per language)                              │
│  @frap/playwright │ JUnit Extension │ pytest plugin       │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│  Language SDK (thin)                                           │
│  config • snapshot • heal() • events • report writers          │
└────────────────────────────┬────────────────────────────────┘
                             │
         ┌───────────────────┼───────────────────┐
         ▼                   ▼                   ▼
    WASM (TS/Node)      FFI/JNI (Java)     subprocess JSON-RPC
         │                   │              (Python fallback / CLI)
         └───────────────────┴───────────────────┘
                             ▼
                    Frap Core (Rust)
```

## Общий контракт SDK (все языки)

Минимальный surface, который **обязан** повторять каждый SDK:

| API | Назначение |
|-----|------------|
| `configure(options)` | `minConfidence`, `reportDir`, policy flags |
| `captureSnapshot(context)` | DOM / page source → формат для Core |
| `heal(originalSelector, signature?, snapshot)` | → `HealResult` (healed, selector, confidence, diff, candidates) |
| `build_element_map(snapshot)` | → element map (discovery); `discover` = adapter + this |
| `generate_page_object(map)` | → Page Object sources (Java Playwright in 1.0.0) |
| `emitEvent(event)` | healing attempt / resolution / safe-fail → JSONL |
| `writeReports()` | junit / allure / frap-debug (где применимо) |

Типы результата — как в reference SDK: `sdk/typescript/src/core.ts` (`HealResult`, `Signature`, `Candidate`).

## Transport по языку

| SDK | Transport | Почему | Статус |
|-----|-----------|--------|--------|
| **TypeScript** | WASM in-process | Playwright/Node, MVP | ⏸️ frozen for Maven/Java release |
| **Java** | Bundled `frap-core-rpc` in JAR | Maven Central 1.0.0: heal + discovery + PO gen | ✅ core + Playwright; WebDriver v1.4 |
| **Python** | (1) JSON-RPC subprocess к Core CLI, (2) позже `ctypes`/FFI | pytest, быстрый старт без JNI | ❌ backlog |

**Правило:** алгоритмы только в Core; SDK не дублирует scoring/clustering.

## Reconsideration criteria

Решение пересматривается только при одном из условий:

1. Нельзя обеспечить целевой performance/latency через доступные transport-слои (WASM/FFI/RPC).
2. Ограничения платформы делают интеграцию Rust Core непрактичной для целевого сегмента.
3. Стоимость сопровождения единого Core становится выше, чем стоимость проверяемой multi-impl стратегии.

До наступления этих условий язык-специфичная реализация алгоритмов в SDK считается anti-pattern.

## Адаптер vs SDK

| Слой | Пример | Ответственность |
|------|--------|-----------------|
| **SDK** | `@frap/sdk-java` | FFI, типы, config, events |
| **Adapter** | `frap-junit5`, `frap-selenium` | JUnit Extension, `WebDriver` wrapper, Allure/JUnit export |
| **CLI** | `frap` | discover/analyze без тестового раннера |

Один Java-проект банка обычно подключает: **SDK + `frap-junit5`** (и опционально thin wrapper над `WebDriver`).

## Порядок реализации

| Этап | Что | Зачем |
|------|-----|-------|
| 1 | F000 Core: FFI + JSON-RPC стабильны | Общая база для Java и Python |
| 2 | TypeScript SDK + Playwright adapter | Reference, CP001–CP005 |
| 3 | Java: SDK + JUnit 5 Extension (S1) | Банковский стек |
| 4 | Python: SDK через JSON-RPC + pytest hook | Параллельные команды, быстрый PoC |
| 5 | Унификация report schema | Один `frap-events.jsonl` / JUnit properties во всех SDK |

## Карточки фич

| SDK / Adapter | Feature card |
|---------------|--------------|
| TypeScript SDK | [F013](../feature/F013-typescript-sdk.md) |
| Playwright adapter | [F008](../feature/F008-playwright-adapter.md) |
| Java SDK & UI adapters | [F014](../feature/F014-java-sdk-ui-adapters.md) |
| Python SDK & adapters | [F015](../feature/F015-python-sdk-adapters.md) |

## Связанные документы

- [F000: Core Platform API](../feature/F000-core-platform-api.md)
- [integrations-selenium-java.md](../../docs/integrations-selenium-java.md) — банковский стек
- [integrations.md](../../docs/integrations.md) — обзор интеграций
- [platform-agnostic-core.md](./platform-agnostic-core.md)
