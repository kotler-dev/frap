# @fletta/sdk

TypeScript SDK for fletta self-healing. Healing algorithms run in **Rust Core via WASM** (`healJson`).

## Installation

```bash
npm install @fletta/sdk
```

No Rust or WASM build required — the WASM binary is bundled with the package.

## Quick Start

```typescript
import { createHealingEngine, type HealResult, type DOMSnapshot } from '@fletta/sdk';

const engine = await createHealingEngine({ minConfidence: 0.85 });

// When a selector fails, heal it using the element signature
const result: HealResult = engine.heal(
  '[data-testid="pay-btn"]',
  signature,        // Original element signature (captured during recording)
  domSnapshot       // Current DOM snapshot
);

if (result.healed) {
  console.log(`Healed to: ${result.selector} (confidence: ${result.confidence})`);
}
```

## API

```typescript
import { createHealingEngine, type HealResult, type DOMSnapshot } from '@fletta/sdk';

const engine = await createHealingEngine({ minConfidence: 0.85, reportDir: './fletta-reports' });
const result: HealResult = engine.heal(primarySelector, signature, domSnapshot);
```

- `HealingEngine.heal()` — calls `fletta_core` WASM (`HealRequest` / `HealResult` JSON contract)
- `HealingEngine.extractSignature()` — snapshot helper (TypeScript; DOM capture stays in adapter)

## Development Build

If you need to build from source (for development or contributing):

```bash
# Requires: rust, wasm-pack, wasm32-unknown-unknown
npm install
npm run build    # build:wasm + tsc
```

WASM output: `wasm/` (gitignored; built in CI).

### Dev fallback

If WASM is missing during development:

```bash
export FLETTA_TS_FALLBACK=1
```

Uses legacy TypeScript healing (`core-fallback.ts`). **Not for production.**
