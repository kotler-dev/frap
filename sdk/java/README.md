# Frap Java SDK

Self-healing selectors and Page Object generation for Java automation.

## Quick Links

| I want to... | Document |
|--------------|----------|
| Get started in 5 minutes | [Getting Started](../../docs/en/java-getting-started.md) |
| See complete API reference | [API Reference](../../docs/en/java-api-reference.md) |
| See what's in 1.0.0 (Maven Central) | [Capability Matrix](../../project/release/java-sdk-1.0.0-matrix.md) |
| Understand Maven Central setup | [Maven Central Guide](../../docs/en/java-maven-central.md) |
| Use JSON-RPC directly | [RPC Protocol](../../docs/en/java-sdk-rpc.md) |
| See full examples | [Demo Project](../../internal/demo/showcase/java-playwright) |

---

## Maven Coordinates

```xml
<!-- Core: healing, discovery, Page Object generation -->
<dependency>
    <groupId>io.github.kotlerdev.frap</groupId>
    <artifactId>frap-core-java</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Playwright adapter: browser integration -->
<dependency>
    <groupId>io.github.kotlerdev.frap</groupId>
    <artifactId>frap-playwright</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

---

## What It Does

1. **Self-healing selectors** — When a CSS selector fails, frap finds the element by structure and reports the new selector with confidence score.

2. **Page discovery** — Analyze any page and get a structured map of elements grouped by clusters (lists, forms, navigation).

3. **Page Object generation** — Generate compilable Java Page Object classes from discovered elements.

4. **Explainable results** — Every decision comes with: new selector, confidence score, diff description, and ranked candidates.

---

## 30-Second Example

```java
import io.github.kotlerdev.frap.playwright.wrapper.Frap;

// Original selector outdated after UI refactor
FrapLocator button = Frap.withFrap(
    page.locator("[data-testid='pay-btn']"),  // Old selector
    page
);

button.click();  // Automatically finds and uses the new selector

// Check what happened
if (Frap.isHealed(button)) {
    System.out.println("Healed to: " + 
        Frap.getLastHealResult(button).selector());
}
```

---

## Modules

| Module | Description | Maven Central |
|--------|-------------|---------------|
| `frap-core-java` | Core client with JSON-RPC transport | ✅ 1.0.0 |
| `frap-playwright` | Playwright adapter, healing, discovery | ✅ 1.0.0 |
| `frap-core-native` | JNI native client (experimental) | ❌ Repo only |

---

## Platform Support

| OS | Architecture | Status |
|----|--------------|--------|
| Linux | x86_64 | ✅ Bundled binary |
| macOS | x86_64 | ✅ Bundled binary |
| macOS | aarch64 (Apple Silicon) | ✅ Bundled binary |
| Windows | x86_64 | ⚠️ Build locally, set `FRAP_CORE_BIN` |

No Rust/Cargo installation required for supported platforms — binary auto-extracted from JAR.

---

## Repository Structure

```
sdk/java/
├── pom.xml                       # Parent POM
├── frap-core-java/              # Core RPC client
│   ├── README.md
│   └── src/...
├── frap-core-native/            # JNI experimental (not on Central)
│   └── README.md
└── smoke-consumer/              # Minimal usage example
    └── src/main/java/...

adapters/playwright-java/        # Playwright adapter
└── README.md

internal/demo/showcase/java-playwright/  # Full demo project
```

---

## Verification

For maintainers and contributors:

```bash
# Level 1: Rust contracts
cd crates && cargo test -p frap-core

# Level 2: Java unit tests
cd sdk/java && mvn -P java-unit verify

# Level 3: Playwright E2E
./scripts/run-java-e2e.sh

# Level 4: Smoke consumer
cd sdk/java/smoke-consumer && mvn compile exec:java
```

See [VERIFICATION.md](./VERIFICATION.md) for full matrix.

---

## Documentation Index

### User Documentation
- [Getting Started](../../docs/en/java-getting-started.md) — 5-minute quick start
- [API Reference](../../docs/en/java-api-reference.md) — Complete Java API
- [Maven Central Guide](../../docs/en/java-maven-central.md) — Release usage
- [RPC Protocol](../../docs/en/java-sdk-rpc.md) — NDJSON protocol for custom integrations

### Maintainer Documentation
- [Verification Matrix](./VERIFICATION.md) — Test levels and acceptance criteria
- [Release Checklist](./MAVEN_RELEASE_CHECKLIST.md) — Maven Central publication

### Related
- [Feature F014: Java SDK](../../project/feature/F014-java-sdk-ui-adapters.md)
- [SDK Strategy](../../project/architecture/sdk-strategy.md)
- [Rust Core RPC](../../crates/core/README.md)

---

## Version

**1.0.0** — Available on Maven Central

- ✅ Playwright Java adapter
- ✅ Self-healing selectors
- ✅ Page discovery and clustering
- ✅ Page Object generation
- ⚠️ WebDriver/Selenium — roadmap v1.4
- ⚠️ Windows bundled binary — build locally