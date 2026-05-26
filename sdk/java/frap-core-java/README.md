# Frap Core Java

Core Java SDK with JSON-RPC client for Frap self-healing selectors.

## Quick Start

```java
// Create client (auto-detects FRAP_CORE_BIN or uses default path)
try (FrapCoreClient client = FrapCoreClient.create()) {
    // Build DOM snapshot from your test framework
    DOMSnapshot snapshot = captureSnapshot(page);

    // Create heal request
    HealRequest request = new HealRequest(
        "[data-testid='pay-btn']",           // Original selector
        originalSignature,                     // From page capture
        snapshot,
        0.85                                   // Min confidence
    );

    // Heal
    HealResult result = client.heal(request);

    if (result.healed()) {
        System.out.println("Healed to: " + result.selector());
        System.out.println("Confidence: " + result.confidence());
    }
}
```

## Maven Coordinates

```xml
<dependency>
    <groupId>io.frap</groupId>
    <artifactId>frap-core-java</artifactId>
    <version>1.1.1-SNAPSHOT</version>
</dependency>
```

## Requirements

- Java 17+
- frap-core-rpc binary (see below)

## Building frap-core-rpc Binary

```bash
cd ../../../crates
cargo build --release -p frap-core --bin frap-core-rpc
```

Binary will be at: `crates/target/release/frap-core-rpc`

## Configuration

### Environment Variables

- `FRAP_CORE_BIN` - Path to frap-core-rpc binary (optional)

### Programmatic

```java
FrapConfig config = FrapConfig.defaults()
    .withMinConfidence(0.90)
    .withReportDir("./build/frap-reports");
```

## Architecture

This module provides:

- **DTOs** - Jackson-annotated records mirroring TypeScript types
- **FrapCoreClient** - Thread-safe JSON-RPC client over subprocess
- **ContextTimeline** - Event timeline utilities for RCA

## Testing

```bash
# Requires frap-core-rpc binary built first
cd ../../../crates && cargo build --release -p frap-core --bin frap-core-rpc

# Run tests
cd sdk/java
mvn test
```

## Transport Options

| Transport | Class | Use Case |
|-----------|-------|----------|
| JSON-RPC (subprocess) | `FrapCoreClient` | Development, quick start |
| JNI (future) | `FrapNativeClient` | Production, minimal latency |

Both implement the same interface - drop-in replacement.

## See Also

- [docs/en/java-sdk-rpc.md](../../../docs/en/java-sdk-rpc.md) - RPC protocol details
- [crates/core](../../../crates/core) - Rust core implementation
