# Java SDK Maven Central Release

This guide covers releasing and using the Frap Java SDK from Maven Central.

## Maven Coordinates

```xml
<dependency>
    <groupId>io.github.kotlerdev.frap</groupId>
    <artifactId>frap-core-java</artifactId>
    <version>1.1.0</version>
</dependency>
```

## Platform Support

| OS | Architecture | Status | Binary |
|----|--------------|--------|--------|
| Linux | x86_64 | ✅ Bundled | `frap-core-rpc-linux-x86_64` |
| macOS | x86_64 | ✅ Bundled | `frap-core-rpc-macos-x86_64` |
| macOS | aarch64 (Apple Silicon) | ✅ Bundled | `frap-core-rpc-macos-aarch64` |
| Windows | x86_64 | 🚧 Planned | - |

The native binary (`frap-core-rpc`) is **automatically extracted** from the JAR on first use.

## Quick Start

### 1. Add Dependency

```xml
<dependencies>
    <!-- Core SDK -->
    <dependency>
        <groupId>io.github.kotlerdev.frap</groupId>
        <artifactId>frap-core-java</artifactId>
        <version>1.1.0</version>
    </dependency>
    
    <!-- Playwright integration (optional) -->
    <dependency>
        <groupId>io.github.kotlerdev.frap</groupId>
        <artifactId>frap-playwright</artifactId>
        <version>1.1.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 2. Use in Test

```java
import io.github.kotlerdev.frap.core.client.FrapRpcClient;
import io.github.kotlerdev.frap.core.dto.*;

// Binary auto-extracted from JAR on first use
try (FrapRpcClient client = FrapRpcClient.create()) {
    HealRequest request = new HealRequest(
        "[data-testid='pay-btn']",
        signature,
        snapshot,
        0.85
    );
    HealResult result = client.heal(request);
    System.out.println("Healed: " + result.selector());
}
```

### 3. Playwright Integration

```java
import io.github.kotlerdev.frap.playwright.wrapper.Frap;
import io.github.kotlerdev.frap.playwright.wrapper.FrapLocator;

// Wrap locator with auto-healing
FrapLocator button = Frap.withFrap(
    page.locator("[data-testid='pay-btn']"),
    page
);

// Click with automatic healing if element moved
button.click();

// Check if healing occurred
if (Frap.isHealed(button)) {
    HealResult result = Frap.getLastHealResult(button);
    System.out.println("Healed to: " + result.selector());
}
```

## How It Works

### Auto-Extract

When `FrapRpcClient.create()` is called:

1. Check `FRAP_CORE_BIN` environment variable (for development)
2. Check development paths (repo structure)
3. Extract bundled binary from JAR to temp directory:
   - Detect platform (Linux/macOS, x86_64/aarch64)
   - Copy from `/META-INF/native/frap-core-rpc-{platform}`
   - Make executable
   - Delete on JVM exit

### No Rust Compilation Required

Users get a working library without installing Rust or compiling anything.

## Development (With Local Binary)

For development or custom builds:

```bash
# Set environment variable
export FRAP_CORE_BIN=/path/to/frap-core-rpc

# Or use relative path in repo
cd frap
cargo build --release -p frap-core --bin frap-core-rpc
# FrapRpcClient.create() will find it automatically
```

## Troubleshooting

### "Bundled binary not found"

```
IOException: Bundled binary not found: /META-INF/native/frap-core-rpc-linux-x86_64
```

**Cause**: Your platform is not supported (e.g., Windows, Linux ARM64).

**Solution**: Build binary locally:

```bash
cd crates
cargo build --release -p frap-core --bin frap-core-rpc
export FRAP_CORE_BIN=target/release/frap-core-rpc
```

### "Cannot run program"

```
IOException: Cannot run program "/tmp/frap-rpc-.../frap-core-rpc": error=13, Permission denied
```

**Cause**: Binary not executable.

**Solution**: Report an issue — this should be handled automatically.

## Release Checklist

See [MAVEN_RELEASE_CHECKLIST.md](../../sdk/java/MAVEN_RELEASE_CHECKLIST.md) for maintainers.
