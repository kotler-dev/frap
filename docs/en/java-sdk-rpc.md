# Java SDK via JSON-RPC

This document describes the JSON-RPC transport for Java (and other JVM/Python) SDKs to communicate with the Rust frap-core without WASM or JNI.

## Overview

The `frap-core-rpc` binary provides an NDJSON (newline-delimited JSON) interface over stdin/stdout. This allows any language to call frap healing and RCA analysis via subprocess communication.

## Building the Binary

```bash
cd crates
cargo build --release -p frap-core --bin frap-core-rpc
```

Binary location: `crates/target/release/frap-core-rpc`

## Protocol

### Request Format

Each line is a JSON object:

```json
{"id": <any>, "method": "heal|analyze_rca", "params": {...}}
```

**Heal request:**
```json
{
  "id": 1,
  "method": "heal",
  "params": {
    "primary_selector": "[data-testid='pay-btn']",
    "original_signature": {
      "path": [{"tag": "button", "role": "submit", "depth": 0}],
      "prefix": "button:submit",
      "stable_attrs": {"data-testid": "pay-btn"},
      "text_content": "Pay",
      "children_hash": 0,
      "depth": 1
    },
    "dom_snapshot": {
      "html": "<button data-testid='checkout-pay'>Pay</button>",
      "elements": [{
        "selector": "[data-testid='checkout-pay']",
        "tag": "button",
        "attributes": {"data-testid": "checkout-pay"},
        "text_content": "Pay",
        "path": ["button:submit"]
      }]
    },
    "min_confidence": 0.85
  }
}
```

**Analyze RCA request:**
```json
{
  "id": 2,
  "method": "analyze_rca",
  "params": {
    "timeline": {
      "events": [
        {"kind": "ui", "timestamp_ms": 1000, "trace_id": "t1", "element": "[data-testid='btn']", "action": "click"},
        {"kind": "ui", "timestamp_ms": 2000, "trace_id": "t1", "element": "[data-testid='btn']", "action": "failure", "detail": "Not found"}
      ]
    },
    "failure_at_ms": 0,
    "window_ms": 30000
  }
}
```

### Response Format

```json
{"id": 1, "result": "<JSON string with HealResult or RcaReport>"}
{"id": 2, "error": {"code": "...", "message": "..."}}
```

## Environment Variables

- `FRAP_CORE_BIN` — Path to the `frap-core-rpc` binary. Default for development: `crates/target/release/frap-core-rpc` relative to repo root.

## Java Client Architecture

```java
public class FrapCoreClient implements AutoCloseable {
    private final Process process;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private int nextId = 1;

    public HealResult heal(HealRequest request) {
        // Serialize request, send NDJSON line, parse response
    }

    public RcaReport analyzeRca(ContextTimeline timeline, long failureAtMs) {
        // Similar pattern
    }

    @Override
    public void close() {
        process.destroy();
    }
}
```

## Testing

```bash
# Run smoke tests
./scripts/smoke-frap-core-rpc.sh

# Test with specific binary
./scripts/smoke-frap-core-rpc.sh /path/to/frap-core-rpc
```

## Comparison with Other Transports

| Transport | Use Case | Status |
|-----------|----------|--------|
| WASM | TypeScript/Node.js in browser or Node | ✅ Ready |
| JSON-RPC (this doc) | Java, Python, any subprocess-capable language | ✅ Ready (v1.1+) |
| JNI (FFI) | Production Java on-prem, minimal latency | 🚧 Planned (v1.4) |

## Migration Path

1. **Development/POC**: Use JSON-RPC (this document)
2. **Production**: Migrate to JNI when available (drop-in replacement at `FrapCoreClient` interface)

The JSON contract (`HealRequest`, `HealResult`, `RcaReport`) remains identical across all transports.
