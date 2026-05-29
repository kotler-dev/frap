# Frap Core Java

Core Java SDK with JSON-RPC client for Frap self-healing selectors.

## Quick Start (healing)

```java
// Maven Central users: no Rust/Cargo required — frap-core-rpc is bundled in the JAR
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

## Discovery and Page Object generation

```java
try (FrapCoreClient client = FrapRpcClient.create()) {
    DOMSnapshot snapshot = ...; // from Playwright SnapshotBuilder or your framework

    ElementMap map = client.buildElementMap(snapshot, MapOptions.defaults());
    ElementMap filtered = client.filterElementMap(map,
        new FilterSpec(true, 2, List.of("button", "a", "input")));

    GeneratedArtifact po = client.generatePageObject(filtered,
        GenerateOptions.javaPlaywright("CatalogPage", "com.example.pages"));

    Files.writeString(Path.of("target/generated/" + po.files().get(0).path()),
        po.files().get(0).content());
}
```

Persist snapshots locally with `SnapshotStore` (optional).

## Platform support (bundled RPC)

| OS | Architecture |
|----|----------------|
| Linux | x86_64 |
| macOS | x86_64, aarch64 |

Windows is not supported in 1.0.0 (set `FRAP_CORE_BIN` to a custom build if needed).

## Maven Coordinates (Maven Central 1.0.0)

```xml
<!-- RPC client (subprocess) - Available on Maven Central -->
<dependency>
    <groupId>io.github.kotler-dev</groupId>
    <artifactId>frap-core-java</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Playwright adapter - Available on Maven Central -->
<dependency>
    <groupId>io.github.kotler-dev</groupId>
    <artifactId>frap-playwright</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

**Not on Maven Central in 1.0.0:**
- `frap-core-native` (JNI) — Experimental, available in repository only. Build locally if needed.

## Transport Selection

| Transport | Module | Class | Latency | Use Case | Status |
|-----------|--------|-------|---------|----------|--------|
| JSON-RPC | `frap-core-java` | `FrapRpcClient` | ~5-15ms | Development, CI, Production | ✅ Stable in 1.0.0 |
| JNI Native | `frap-core-native` | `FrapNativeClient` | ~0.5-2ms | Low-latency production | 🚧 Experimental, not on Central |

Both implement `FrapCoreClient` interface - future drop-in replacement:

```java
// Currently: use RPC (bundled binaries, no setup required)
FrapCoreClient client = FrapRpcClient.create();

// Future (v1.4+): Auto-select with JNI fallback
FrapCoreClient client;
try {
    client = FrapNativeClient.create();  // Fast native (when available)
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

- [docs/en/java-getting-started.md](../../../docs/en/java-getting-started.md) — Quick start guide (5 minutes)
- [docs/en/java-api-reference.md](../../../docs/en/java-api-reference.md) — Complete API reference
- [docs/en/java-maven-central.md](../../../docs/en/java-maven-central.md) — Maven Central usage
- [docs/en/java-sdk-rpc.md](../../../docs/en/java-sdk-rpc.md) — RPC protocol details
- [../frap-core-native](../frap-core-native) — JNI native client (experimental, not on Central)
- [crates/core](../../../crates/core) — Rust core with FFI
- [sdk/java](../..) — SDK documentation index
