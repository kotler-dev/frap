# Java SDK via JSON-RPC

This document describes the JSON-RPC transport for Java (and other JVM/Python) SDKs to communicate with the Rust frap-core without WASM or JNI.

## Overview

The `frap-core-rpc` binary provides an NDJSON (newline-delimited JSON) interface over stdin/stdout. This allows any language to call frap healing, RCA analysis, element discovery, and Page Object generation via subprocess communication.

**Status**: ✅ Ready in 1.0.0 (Maven Central)

## Building the Binary

For development or custom platforms (e.g., Windows):

```bash
cd crates
cargo build --release -p frap-core --bin frap-core-rpc
```

Binary location: `crates/target/release/frap-core-rpc`

For Maven Central users: the binary is **bundled** in the JAR for Linux x86_64 and macOS (x86_64/aarch64).

## Protocol

### Request Format

Each line is a JSON object:

```json
{"id": <any>, "method": "heal|analyze_rca|build_element_map|filter_element_map|generate_page_object", "params": {...}}
```

### Methods

#### 1. `heal` — Self-healing selector resolution

**Request:**
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

#### 2. `analyze_rca` — Root cause analysis

Post-mortem classification of a **ContextTimeline** (UI + network + console/logs) around the failure moment. Returns `RcaReport` with primary cause, confidence, timeline excerpt, and recommendation.

**Not** healing (no locator recovery) and **not** discover (no element map).

**Request:**
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

#### 3. `build_element_map` — Discovery and clustering

Analyzes DOM snapshot and returns element map with clustered groups (lists, forms, etc.).

**Playwright path:** snapshot comes from `page.evaluate(...)` via `SnapshotBuilder` — **not** direct CDP. Standalone Chrome/CDP source is roadmap.

**Request:**
```json
{
  "id": 3,
  "method": "build_element_map",
  "params": {
    "dom_snapshot": {
      "html": "<div class='catalog'><article data-testid='card'>...</article></div>",
      "elements": [
        {
          "selector": "[data-testid='card']",
          "tag": "article",
          "attributes": {"data-testid": "card"},
          "text_content": "Product",
          "path": ["div:-", "article:-"],
          "position_in_parent": 0
        }
      ]
    },
    "options": {
      "url": "https://example.com/catalog",
      "include_non_interactive": true,
      "max_elements": null
    }
  }
}
```

#### 4. `filter_element_map` — Filter elements by criteria

Filters element map by interactive tags, cluster size, etc.

**Request:**
```json
{
  "id": 4,
  "method": "filter_element_map",
  "params": {
    "element_map": { /* from build_element_map result */ },
    "spec": {
      "interactive_only": true,
      "min_cluster_size": 2,
      "include_tags": ["button", "a", "input"]
    }
  }
}
```

#### 5. `generate_page_object` — Page Object code generation

Generates Java Page Object source from element map.

**Request:**
```json
{
  "id": 5,
  "method": "generate_page_object",
  "params": {
    "element_map": { /* from build_element_map */ },
    "options": {
      "language": "java_playwright",
      "class_name": "CatalogPage",
      "package_name": "com.example.pages",
      "include_signatures": true
    }
  }
}
```

### Response Format

```json
{"id": 1, "result": "<JSON string with result>"}
{"id": 2, "error": {"code": "...", "message": "..."}}
```

Error codes:
- `PARSE_ERROR` — Invalid JSON
- `METHOD_NOT_FOUND` — Unknown method
- `INVALID_PARAMS` — Missing or invalid parameters

## Environment Variables

- `FRAP_CORE_BIN` — Path to the `frap-core-rpc` binary. Default for development: `crates/target/release/frap-core-rpc` relative to repo root.

## Java Client Architecture

```java
public interface FrapCoreClient extends AutoCloseable {
    HealResult heal(HealRequest request) throws IOException;
    RcaReport analyzeRca(ContextTimeline timeline, long failureAtMs) throws IOException;
    ElementMap buildElementMap(DOMSnapshot snapshot, MapOptions options) throws IOException;
    ElementMap filterElementMap(ElementMap map, FilterSpec spec) throws IOException;
    GeneratedArtifact generatePageObject(ElementMap map, GenerateOptions options) throws IOException;
    boolean isAlive();
    void close();
}

// RPC implementation
public class FrapRpcClient implements FrapCoreClient {
    public static FrapRpcClient create() throws IOException {
        // Auto-detects bundled binary or FRAP_CORE_BIN
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

| Transport | Use Case | Status | Availability |
|-----------|----------|--------|--------------|
| WASM | TypeScript/Node.js in browser or Node | ✅ Ready | npm `@frap/frap-sdk` |
| JSON-RPC (this doc) | Java, Python, any subprocess-capable language | ✅ Ready | Maven Central 1.0.0 |
| JNI (FFI) | Production Java on-prem, minimal latency | 🚧 Experimental | Repo only, not on Central |

## Migration Path

1. **Development/POC**: Use JSON-RPC (this document) — works out of the box with bundled binaries
2. **Production**: Optional migration to JNI when production-ready (drop-in replacement at `FrapCoreClient` interface)

The JSON contract (`HealRequest`, `HealResult`, `RcaReport`, `ElementMap`, etc.) remains identical across all transports.

## See Also

- [java-maven-central.md](./java-maven-central.md) — Maven Central usage guide
- [java-api-reference.md](./java-api-reference.md) — Complete Java API reference
- [crates/core/README.md](../../crates/core/README.md) — Rust Core RPC documentation
