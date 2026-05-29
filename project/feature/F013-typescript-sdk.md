# Feature: TypeScript SDK (F013)

## Meta

- **Epic**: SDK → TypeScript
- **Roll-up target**: ## MVP v1.0.0
- **Status**: done
- **Target release**: v1.0.0
- **Created**: 2026-05-23
- **Related cases**: CP001, CP002, CP003, CP004, CP005, C001, C004

## Goal

Языковой SDK для Node.js/TypeScript: единый контракт вызова Rust Core (WASM), типы `HealResult` / `Signature`, конфигурация, события и debug-отчёты. Reference implementation для Java и Python SDK.

Архитектурный guardrail: SDK не дублирует алгоритмы healing/scoring/clustering; вся алгоритмика живёт в Core (см. [ADR-001](../architecture/ADR-001-core-language-strategy.md)).

## User workflow

1. Пакет `@frap/sdk` подключается как зависимость (monorepo: `file:../sdk/typescript` или npm publish).
2. Адаптер (F008) или custom код вызывает `HealingEngine.heal()` с DOM snapshot.
3. SDK возвращает структурированный результат (healed, confidence, diff, candidates).
4. События пишутся в `frap-events.jsonl`; опционально debug HTML (F012).

## Scope

### In
- Пакет `sdk/typescript/` (`@frap/sdk`)
- Типы: `HealResult`, `Signature`, `Candidate`, `DOMSnapshot`, `HealingSemantics`
- `HealingEngine`: extract signature, heal, clustering helpers
- Конфигурация: `FrapConfig` (minConfidence, reportDir, debug)
- Debug tracer и запись отчётов (`debug.ts`, F012)
- Контракт API — эталон для [sdk-strategy.md](../architecture/sdk-strategy.md)

### Out
- Playwright hooks, custom selectors, reporter (это **F008**)
- Публикация в npm registry (до стабилизации MVP)
- Browser-only bundle без Node (backlog)

## Acceptance criteria

- [x] Пакет собирается: `cd sdk/typescript && npm run build`
- [x] Типы экспортируются из `src/index.ts`
- [x] `HealingEngine.heal()` возвращает `HealResult` с confidence и candidates
- [x] E2e используют SDK через `file:../sdk/typescript`
- [x] F008 adapter зависит от `@frap/sdk`
- [x] WASM Core подключён: `healJson` из `sdk/typescript/wasm/`
- [x] Документация: API reference в `sdk/typescript/README.md`

### Implementation Status

| Component | Status | Location |
|-----------|--------|----------|
| Package | ✅ | `sdk/typescript/package.json` |
| Core engine (WASM) | ✅ | `sdk/typescript/src/core.ts` |
| TS fallback (dev) | ✅ | `sdk/typescript/src/core-fallback.ts` |
| Config | ✅ | `sdk/typescript/src/config.ts` |
| Debug / F012 | ✅ | `sdk/typescript/src/debug.ts` |
| Healing semantics | ✅ | `sdk/typescript/src/healing-semantics.ts` |

## Implementation notes (sketch)

### Структура

```
sdk/typescript/
├── src/
│   ├── index.ts
│   ├── core.ts           # HealingEngine, HealResult
│   ├── config.ts
│   ├── debug.ts
│   └── healing-semantics.ts
├── package.json
└── tsconfig.json
```

### Зависимости

- **F001** — алгоритмы healing (в Core / временно в TS)
- **F000** — целевой transport: WASM in-process
- **F008** — основной потребитель adapter layer

### Риски

- Дублирование логики Core в TS до завершения F000 WASM
- Расхождение контракта с будущими F014/F015 — держать sync через sdk-strategy

## Verification / Test plan

### Manual smoke

```bash
cd sdk/typescript && npm install && npm run build
cd ../../e2e && npm test   # косвенно через @frap/sdk
```

### Automation

- E2E CP001–CP003 через Playwright adapter + SDK
- Unit tests SDK (backlog)

## Related docs

- [F008: Playwright Adapter](./F008-playwright-adapter.md)
- [F000: Core Platform API](./F000-core-platform-api.md)
- [sdk-strategy.md](../architecture/sdk-strategy.md)
- [integrations.md](../../docs/integrations.md)
