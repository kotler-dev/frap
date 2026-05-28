# Frap Core Java

Core Java SDK with JSON-RPC client for Frap self-healing selectors.

## Quick Start

```java
// Create client (auto-detects FRAP_CORE_BIN or uses default path)
try (FrapCoreClient client = FrapRpcClient.create()) {
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
<!-- RPC client (subprocess) -->
<dependency>
    <groupId>io.github.kotlerdev.frap</groupId>
    <artifactId>frap-core-java</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- OR: Native client (JNI) for production -->
<dependency>
    <groupId>io.github.kotlerdev.frap</groupId>
    <artifactId>frap-core-native</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Transport Selection

| Transport | Module | Class | Latency | Use Case |
|-----------|--------|-------|---------|----------|
| JSON-RPC | `frap-core-java` | `FrapRpcClient` | ~5-15ms | Development, CI |
| JNI Native | `frap-core-native` | `FrapNativeClient` | ~0.5-2ms | Production |

Both implement `FrapCoreClient` interface - drop-in replacement:

```java
// Auto-select transport
FrapCoreClient client;
try {
    client = FrapNativeClient.create();  // Fast native
} catch (UnsatisfiedLinkError e) {
    client = FrapRpcClient.create();     // Fallback to RPC
}
```

## Requirements

- Java 17+
- frap-core-rpc binary (for RPC transport)
- OR: Native library (for JNI transport)

## Building Binaries

### RPC Binary
```bash
cd ../../../crates
cargo build --release -p frap-core --bin frap-core-rpc
```

### Native Library
```bash
cd ../../../crates
cargo build --release -p frap-core --features ffi
```

## Configuration

### Environment Variables

| Variable | Description |
|----------|-------------|
| `FRAP_CORE_BIN` | Path to frap-core-rpc binary |
| `frap.native.lib` | Path to native library (JNI) |
| `FRAP_REPORT_DIR` | Default report directory |

### Programmatic

```java
FrapConfig config = FrapConfig.defaults()
    .withMinConfidence(0.90)
    .withReportDir("./build/frap-reports");
```

## Architecture

This module provides:

- **DTOs** - Jackson-annotated records mirroring TypeScript types
- **FrapCoreClient** - Interface for both transports
- **FrapRpcClient** - JSON-RPC over subprocess
- **FrapNativeClient** - JNI native calls (in `frap-core-native`)
- **ContextTimeline** - Event timeline utilities for RCA

## Testing

```bash
# Requires frap-core-rpc binary built first
cd ../../../crates && cargo build --release -p frap-core --bin frap-core-rpc

# Run tests
cd sdk/java
mvn test

# Run with native library (if available)
cd ../../../crates && cargo build --release -p frap-core --features ffi
mvn test -Dfrap.native.lib=../../../crates/target/release/libfrap_core.dylib
```

## See Also

- [../frap-core-native](../frap-core-native) - JNI native client
- [docs/en/java-sdk-rpc.md](../../../docs/en/java-sdk-rpc.md) - RPC protocol
- [crates/core](../../../crates/core) - Rust core with FFI
