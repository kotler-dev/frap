# ADR-001: Core Language Strategy (Rust Core + Thin SDK)

## Status

Accepted

## Date

2026-05-28

## Context

frap должен обеспечивать одинаковый и explainable self-healing в нескольких экосистемах (TypeScript/Playwright, Java/JUnit-WebDriver, Python/pytest), а также поддерживать on-prem сценарии и постепенное расширение платформ.

Базовая дилемма:

1. Реализовать core-алгоритмы в каждом SDK-языке отдельно.
2. Держать один алгоритмический Core и подключать его через transport-слои.

## Decision

Выбран подход:

- Алгоритмы healing/scoring/clustering реализуются только в Rust Core.
- Языковые SDK остаются тонкими: transport, hooks фреймворка, config, events, report writers.
- Допустимые transport-слои: WASM (TS), FFI/JNI (Java), JSON-RPC subprocess (Python bootstrap).

## Consequences

### Positive

- Поведение и explainability-поля (`confidence`, `diff`, `candidates`) одинаковы во всех SDK.
- Нет дублирования алгоритмического кода между языками.
- Проще поддерживать детерминизм и no-ML-by-default политику.
- Один Core переиспользуется для новых адаптеров и платформ.

### Negative

- Выше initial complexity (WASM/FFI/RPC, packaging, CI).
- SDK-команды зависят от зрелости transport-интеграций.
- Порог входа для контрибьюторов в Core выше, чем при language-only.

## Non-goals

- Полный отказ от native интеграций в пользу одного transport.
- Переписывание алгоритмов в SDK для локальной оптимизации без системного обоснования.

## Guardrails

1. Изменения в healing/scoring/clustering вносятся в Core first.
2. SDK не должен содержать fork-логики ранжирования кандидатов.
3. Новые SDK обязаны следовать общему контракту из `sdk-strategy.md`.

## Revisit criteria

ADR подлежит пересмотру только при одном из сценариев:

1. Доказанная невозможность достичь SLO через WASM/FFI/RPC для целевых платформ.
2. Критические платформенные ограничения для Rust Core в ключевом сегменте пользователей.
3. Измеримо меньшая стоимость владения у multi-implementation подхода при сохранении explainability parity.

## Links

- [SDK Strategy](./sdk-strategy.md)
- [F013: TypeScript SDK](../feature/F013-typescript-sdk.md)
- [F014: Java SDK & UI Adapters](../feature/F014-java-sdk-ui-adapters.md)
- [F015: Python SDK & Adapters](../feature/F015-python-sdk-adapters.md)
