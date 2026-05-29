# Rust Core 0.1.0

Internal release of the Rust workspace that powers all SDKs.

**Status:** Internal / embedded (not published to crates.io)

## Workspace Structure

```
crates/
├── signature/       # DOM element signature extraction
├── clustering/      # Drain3-based DOM/log clustering
├── healing/         # Self-healing selector engine
├── core/            # WASM + native targets, RPC binary
├── context/         # Unified context (UI + logs + network)
└── rca/             # Root Cause Analysis
```

## Version

```toml
# crates/Cargo.toml
[workspace.package]
version = "0.1.0"
```

## Distribution

Core is not consumed directly. It ships embedded in:

| Target | Format | Consumers |
|--------|--------|-----------|
| `frap-core-rpc` | Native binary | Java SDK (bundled in JAR) |
| `wasm-node` | WASM + JS glue | npm `@frap/sdk` |
| `wasm-bundler` | WASM (browser) | Future: browser extensions |

## Build

```bash
# Native RPC binary (for Java bundling)
cd crates && cargo build --release -p frap-core --bin frap-core-rpc

# WASM for npm
cd crates/core && wasm-pack build --target nodejs --out-dir ../../sdk/typescript/wasm-node --features wasm
```

## RPC Interface (stable)

Methods exposed via JSON-RPC:

- `heal` — Self-healing selector resolution
- `build_element_map` — Discovery / clustering
- `filter_element_map` — Filtered element queries
- `generate_page_object` — PO source generation
- `analyze_rca` — Root cause analysis

See [sdk/java/docs/en/java-sdk-rpc.md](../../../docs/en/java-sdk-rpc.md) for protocol details.

## Versioning

- `0.1.x` — current stable
- Bump **minor** (0.2.0) when RPC contract changes
- Bump **patch** (0.1.1) for fixes

## Compatibility

| Core Version | npm SDK | Maven SDK |
|--------------|---------|-----------|
| 0.1.0 | 1.1.x | 1.0.x |

---

*Last updated: 2026-05-29*
