# frap-core

Public Rust API aggregating `signature`, `clustering`, and `healing`.

## Tests

```bash
cd crates && cargo test -p frap-core
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

## JSON-RPC Binary (Java SDK)

For language SDKs without WASM support (Java, Python), frap-core provides an NDJSON-RPC binary.

### Building

```bash
cd crates && cargo build --release -p frap-core --bin frap-core-rpc
```

Binary location: `target/release/frap-core-rpc`

### Protocol

The binary reads newline-delimited JSON (NDJSON) requests from stdin and writes responses to stdout.

**Methods:** `heal`, `analyze_rca`, `build_element_map`, `filter_element_map`, `generate_page_object`

**Request format:**
```json
{"id":1,"method":"heal","params":{"primary_selector":"...","original_signature":{},"dom_snapshot":{},"min_confidence":0.85}}
{"id":2,"method":"analyze_rca","params":{"timeline":{"events":[...]},"failure_at_ms":0,"window_ms":30000}}
```

**Response format:**
```json
{"id":1,"result":"{\"healed\":true,...}"}
{"id":2,"error":{"code":"...","message":"..."}}
```

### Environment Variables

- `FRAP_CORE_BIN` — path to the binary (default: `target/release/frap-core-rpc` relative to repo root in development)

### Testing

```bash
# Smoke tests
./scripts/smoke-frap-core-rpc.sh

# Unit tests for binary
cd crates && cargo test -p frap-core --bin frap-core-rpc
```
