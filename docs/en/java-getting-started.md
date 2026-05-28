# Getting Started with Frap Java SDK

Quick start guide for using Frap Java SDK from Maven Central.

**Time to first test:** ~5-10 minutes  
**Prerequisites:** Java 17+, Maven

---

## 1. Add Dependencies

Add to your `pom.xml`:

```xml
<dependencies>
    <!-- Core SDK (required) -->
    <dependency>
        <groupId>io.github.kotlerdev.frap</groupId>
        <artifactId>frap-core-java</artifactId>
        <version>1.0.0</version>
    </dependency>
    
    <!-- Playwright adapter (for browser tests) -->
    <dependency>
        <groupId>io.github.kotlerdev.frap</groupId>
        <artifactId>frap-playwright</artifactId>
        <version>1.0.0</version>
        <scope>test</scope>
    </dependency>
    
    <!-- Playwright itself -->
    <dependency>
        <groupId>com.microsoft.playwright</groupId>
        <artifactId>playwright</artifactId>
        <version>1.44.0</version>
        <scope>test</scope>
    </dependency>
    
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

**Install Playwright browsers (once):**

```bash
mvn exec:java -Dexec.mainClass="com.microsoft.playwright.CLI" \
    -Dexec.args="install chromium"
```

---

## 2. Platform Support

| OS | Architecture | Status |
|----|--------------|--------|
| Linux | x86_64 | ✅ Works out of the box |
| macOS | Intel (x86_64) | ✅ Works out of the box |
| macOS | Apple Silicon (aarch64) | ✅ Works out of the box |
| Windows | x86_64 | ⚠️ Build binary locally (see Troubleshooting) |

The native binary is **automatically extracted** from the JAR on first use. No Rust/Cargo installation required.

---

## 3. Quick Test

Create `src/test/java/FrapQuickTest.java`:

```java
import com.microsoft.playwright.*;
import io.github.kotlerdev.frap.playwright.wrapper.Frap;
import io.github.kotlerdev.frap.playwright.wrapper.FrapLocator;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

public class FrapQuickTest {
    static Playwright playwright;
    static Browser browser;
    Page page;

    @BeforeAll
    static void beforeAll() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @AfterAll
    static void afterAll() {
        browser.close();
        playwright.close();
        Frap.clearClient();
    }

    @BeforeEach
    void beforeEach() {
        page = browser.newPage();
    }

    @Test
    void testSelfHealing() {
        // HTML with changed data-testid (simulating UI refactor)
        page.setContent("""
            <button data-testid='pay-btn-v2'>Pay Now</button>
            """);

        // Original selector (outdated) wrapped with healing
        FrapLocator button = Frap.withFrap(
            page.locator("[data-testid='pay-btn']"),
            page
        );

        // Click works even though selector changed
        button.click();

        // Verify healing occurred
        assertThat(Frap.isHealed(button)).isTrue();
        System.out.println("Healed to: " + 
            Frap.getLastHealResult(button).selector());
    }
}
```

Run:

```bash
mvn test -Dtest=FrapQuickTest
```

---

## 4. Discovery and Page Object Generation

### Discover Page Structure

```java
import io.github.kotlerdev.frap.core.dto.*;

@Test
void testDiscovery() {
    page.setContent("""
        <div class="catalog">
          <article data-testid="product-card">
            <h3>Phone</h3>
            <button class="buy">Buy</button>
          </article>
          <article data-testid="product-card">
            <h3>Tablet</h3>
            <button class="buy">Buy</button>
          </article>
        </div>
        """);

    // Build element map with clusters
    ElementMap map = Frap.discover(page);

    // Check for list clusters (repeating elements)
    long listClusters = map.clusters().stream()
        .filter(c -> c.clusterType() == ClusterType.LIST)
        .filter(c -> c.elementIds().size() >= 2)
        .count();

    assertThat(listClusters).isGreaterThanOrEqualTo(1);
    System.out.println("Found " + map.elements().size() + " elements");
}
```

### Generate Page Object

```java
import java.nio.file.Path;

@Test
void testGeneratePageObject() throws Exception {
    page.setContent("""
        <div class="catalog">
          <article data-testid="product-card">
            <h3>Phone</h3>
            <button class="buy">Buy</button>
          </article>
        </div>
        """);

    // Generate Java Page Object
    List<Path> files = Frap.generatePageObject(
        page,
        Path.of("target/generated-sources"),
        GenerateOptions.javaPlaywright("CatalogPage", "com.example.pages")
    );

    // Files are written, ready to use
    System.out.println("Generated: " + files);
}
```

---

## 5. Path A: Core Only (No Browser)

For standalone use without Playwright:

```xml
<dependency>
    <groupId>io.github.kotlerdev.frap</groupId>
    <artifactId>frap-core-java</artifactId>
    <version>1.0.0</version>
</dependency>
```

```java
import io.github.kotlerdev.frap.core.client.FrapRpcClient;
import io.github.kotlerdev.frap.core.dto.*;

public class CoreOnlyExample {
    public static void main(String[] args) throws Exception {
        // Binary auto-extracted from JAR
        try (var client = FrapRpcClient.create()) {
            
            // Build DOM snapshot from your source
            DOMSnapshot snapshot = new DOMSnapshot(
                "<button data-testid='ok'>OK</button>",
                List.of(
                    new DOMElementInfo(
                        "[data-testid='ok']",
                        "button",
                        Map.of("data-testid", "ok"),
                        "OK",
                        List.of("button:-"),
                        null
                    )
                )
            );

            // Build element map
            ElementMap map = client.buildElementMap(
                snapshot, 
                MapOptions.defaults()
            );

            System.out.println("Mapped " + map.elements().size() + " elements");
            
            // Generate Page Object
            GeneratedArtifact po = client.generatePageObject(
                map,
                GenerateOptions.javaPlaywright("MyPage", "com.example")
            );
            
            System.out.println(po.files().get(0).content());
        }
    }
}
```

---

## 6. Path B: Full Playwright with Reports

For comprehensive testing with healing reports:

```java
import io.github.kotlerdev.frap.playwright.extension.FrapExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(FrapExtension.class)  // Automatic reports
class FullTest {
    // ... tests with Frap.withFrap()
    // Reports generated in target/frap-reports/
}
```

**Generated reports:**
- `frap-report.json` — Healing events summary
- `frap-debug.html` — Human-readable debug view
- `junit.xml` — JUnit XML with frap properties

---

## 7. Troubleshooting

### "Bundled binary not found"

**Linux/macOS:** Should not happen — report an issue if it does.

**Windows:** Not bundled in 1.0.0. Build locally:

```bash
# Prerequisites: Rust toolchain (rustup.rs)
git clone https://github.com/kotler-dev/frap.git
cd frap/crates
cargo build --release -p frap-core --bin frap-core-rpc

# Set environment variable
set FRAP_CORE_BIN=target\release\frap-core-rpc.exe
```

Then run your tests with the environment variable set.

### Slow first run

First call to `FrapRpcClient.create()` extracts the bundled binary (~100ms). Subsequent calls reuse the cached binary.

### No healing occurring

Check confidence threshold (default 0.85). Lower for development:

```java
var options = new WithFrapOptions()
    .minConfidence(0.70)
    .debug(true);  // Enable debug reports

Frap.withFrap(locator, page, options).click();
```

---

## 8. Next Steps

- **API Reference:** [java-api-reference.md](./java-api-reference.md)
- **Maven Central Guide:** [java-maven-central.md](./java-maven-central.md)
- **RPC Protocol:** [java-sdk-rpc.md](./java-sdk-rpc.md)
- **Demo Project:** See `internal/demo/showcase/java-playwright` in the repository for full examples including conference tests and context capture.

---

## What's Not in 1.0.0

| Feature | Status | Notes |
|---------|--------|-------|
| WebDriver/Selenium | v1.4 roadmap | Playwright only in 1.0.0 |
| Windows bundled binary | Not included | Build locally |
| JNI/Native client | Experimental | `frap-core-native` not on Central |
| CLI `frap discover` | Not available | Use Java API or build from repo |

---

## Version

This guide is for **Frap Java SDK 1.0.0** on Maven Central.