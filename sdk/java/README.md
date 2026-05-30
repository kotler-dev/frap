# Frap Java SDK

Self-healing selectors and Page Object generation for Java automation.

## Maven Coordinates

```xml
<!-- Core: healing, discovery, Page Object generation -->
<dependency>
    <groupId>io.github.kotler-dev</groupId>
    <artifactId>frap-core-java</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Playwright adapter: browser integration -->
<dependency>
    <groupId>io.github.kotler-dev</groupId>
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

See module READMEs:
- [frap-core-java](./frap-core-java/README.md)
- [frap-core-native](./frap-core-native/README.md)
- [frap-playwright adapter](../../adapters/playwright-java/README.md)

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

examples/java/playwright/        # Full demo project
```

---

## Verification

For maintainers and contributors (run from repository root):

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

## Documentation

- [frap-core-java](./frap-core-java/README.md) — Core RPC client API
- [frap-playwright adapter](../../adapters/playwright-java/README.md) — Playwright integration
- [Demo project](../../examples/java/playwright/) — Runnable example
- [Verification Matrix](./VERIFICATION.md) — Test levels and acceptance criteria
- [Release Checklist](./MAVEN_RELEASE_CHECKLIST.md) — Maven Central publication
- [Rust Core RPC](../../crates/core/README.md) — Native RPC binary

---

## Version

**1.0.0** — Available on Maven Central

- ✅ Playwright Java adapter
- ✅ Self-healing selectors
- ✅ Page discovery and clustering
- ✅ Page Object generation
- ⚠️ WebDriver/Selenium — roadmap v1.4
- ⚠️ Windows bundled binary — build locally
