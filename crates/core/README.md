# frapcode-core

Public Rust API aggregating `signature`, `clustering`, and `healing`.

## Tests

```bash
cd crates && cargo test -p frapcode-core
```

## WASM (TypeScript SDK)

```bash
# from repo root
wasm-pack build crates/core \
  --target bundler \
  --out-dir sdk/typescript/wasm \
  --features wasm
```

Exports `healJson(input: string): string` — JSON [`HealRequest`](../core/src/lib.rs) in, JSON `HealResult` out.
