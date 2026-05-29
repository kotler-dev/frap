# Frap Core Native (JNI)

JNI bindings for frap-core native library. Provides in-process calls to Rust core for production environments.

## Comparison: RPC vs Native

| Feature | `frap-core-java` (RPC) | `frap-core-native` (JNI) |
|---------|--------------------------|--------------------------|
| **Latency** | Higher (process spawn + IPC) | Lower (direct call) |
| **Setup** | Auto-detects binary | Requires native library |
| **Security** | Subprocess isolation | In-process (JNI) |
| **Use Case** | Development, CI | Production, high-frequency |

## Quick Start

### 1. Build Native Library

```bash
cd ../../../crates
cargo build --release -p frap-core --features ffi
```

Library location:
- Linux: `target/release/libfrap_core.so`
- macOS: `target/release/libfrap_core.dylib`
- Windows: `target/release/frap_core.dll`

### 2. Configure Java

```java
// Option 1: System property
System.setProperty("frap.native.lib", "/path/to/libfrap_core.so");

// Option 2: java.library.path
java -Djava.library.path=/path/to/libs -jar app.jar
```

### 3. Use FrapNativeClient

```java
import io.github.kotlerdev.frap.core.native_.FrapNativeClient;

// Auto-loads library from system property or JAR
try (FrapNativeClient client = FrapNativeClient.create()) {
    HealResult result = client.heal(request);
}
```

## Maven Coordinates

```xml
<dependency>
    <groupId>io.github.kotler-dev</groupId>
    <artifactId>frap-core-native</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Platform Support

| Platform | Library Name | Status |
|----------|--------------|--------|
| Linux x86_64 | `libfrap_core.so` | ✅ |
| Linux aarch64 | `libfrap_core.so` | ✅ |
| macOS x86_64 | `libfrap_core.dylib` | ✅ |
| macOS aarch64 | `libfrap_core.dylib` | ✅ |
| Windows x86_64 | `frap_core.dll` | 🚧 |

## Bundling in JAR

For distribution, bundle native libs in `META-INF/native/`:

```
META-INF/native/
  linux-x86_64/libfrap_core.so
  linux-aarch64/libfrap_core.so
  macos-x86_64/libfrap_core.dylib
  macos-aarch64/libfrap_core.dylib
```

`FrapNativeClient` automatically extracts and loads the correct library.

## Transport Selection

```java
// Auto-select based on availability
FrapCoreClient client;
try {
    client = FrapNativeClient.create();  // Try JNI first
} catch (UnsatisfiedLinkError e) {
    client = FrapRpcClient.create();      // Fallback to RPC
}

// Or use explicitly
if ("native".equals(System.getProperty("frap.transport"))) {
    client = FrapNativeClient.create();
} else {
    client = FrapRpcClient.create();
}
```

## Performance

Benchmarks on M1 Mac (Apple Silicon):

| Operation | RPC (ms) | Native (ms) | Speedup |
|-----------|----------|-------------|---------|
| heal() cold | 15-20 | 2-3 | 6-10x |
| heal() warm | 5-8 | 0.5-1 | 5-8x |

## FFI API (C Headers)

Generated with cbindgen:

```bash
cd ../../../crates/core
cbindgen --config cbindgen.toml --output frap.h
```

Header includes:
- `FrapCoreHandle*` - Opaque handle
- `frap_heal()` - Healing function
- `frap_analyze_rca()` - RCA analysis
- `frap_string_free()` - Memory management

## Troubleshooting

### Library not found
```
UnsatisfiedLinkError: no frap_core in java.library.path
```
**Fix**: Set `-Dfrap.native.lib=/absolute/path/to/lib`

### Version mismatch
```
UnsatisfiedLinkError: undefined symbol
```
**Fix**: Rebuild native library with matching version

## See Also

- [../frap-core-java](../frap-core-java) - RPC client
- [../../../crates/core](../../../crates/core) - Rust FFI implementation
