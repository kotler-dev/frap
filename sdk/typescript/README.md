# @fletta/sdk

TypeScript SDK for fletta self-healing. Healing algorithms run in **Rust Core via WASM** (`healJson`).

## Build

```bash
# Requires: rust, wasm-pack, wasm32-unknown-unknown
npm install
npm run build    # build:wasm + tsc
```

WASM output: `wasm/` (gitignored; built in CI).

## API

```typescript
import { createHealingEngine, type HealResult, type DOMSnapshot } from '@fletta/sdk';

const engine = await createHealingEngine({ minConfidence: 0.85, reportDir: './fletta-reports' });
const result: HealResult = engine.heal(primarySelector, signature, domSnapshot);
```

- `HealingEngine.heal()` — calls `fletta_core` WASM (`HealRequest` / `HealResult` JSON contract)
- `HealingEngine.extractSignature()` — snapshot helper (TypeScript; DOM capture stays in adapter)

## Dev fallback

If WASM is missing during development:

```bash
export FLETTA_TS_FALLBACK=1
```

Uses legacy TypeScript healing (`core-fallback.ts`). **Not for production.**
