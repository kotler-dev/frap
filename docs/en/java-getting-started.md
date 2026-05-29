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
    
    <!-- AssertJ (used in examples) -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <version>3.25.3</version>
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

### Healing Policies

Control healing behavior with policies:

```java
import io.github.kotlerdev.frap.core.dto.HealResult;
import io.github.kotlerdev.frap.core.semantics.HealOutcome;

// Policy: expect_heal — fail if no healing was needed (detects unexpected stability)
var options = new WithFrapOptions()
    .healPolicy("expect_heal")
    .minConfidence(0.7);

FrapLocator button = Frap.withFrap(
    page.locator("[data-testid='pay-btn']"),
    page,
    options
);

button.click();

// Check semantics — useful for CI gates
HealResult result = Frap.getLastHealResult(button);
if (result.semantics() != null) {
    HealOutcome outcome = result.semantics().outcome();
    // Possible outcomes: HEALED, REJECTED, UNEXPECTED_HEAL, NO_HEAL
    assertThat(outcome).isEqualTo(HealOutcome.HEALED);
}
```

| Policy | Behavior |
|--------|----------|
| `allow` | Always heal if confident enough (default) |
| `deny` | Never heal — fail on selector not found |
| `expect_heal` | Expect healing to occur — fail if original selector still works |

### Ambiguous Elements — Safe Fail

When two elements are equally good candidates, Frap **rejects** healing to avoid clicking the wrong button:

```java
// Page has two "Submit" buttons with similar attributes
// Original selector points to removed element
FrapLocator submit = Frap.withFrap(
    page.locator("[data-testid='cfp-submit-missing']"),
    page,
    new WithFrapOptions().minConfidence(0.85)
);

// Throws PlaywrightException — healing refused
try {
    submit.click();
} catch (PlaywrightException e) {
    // Ambiguous: check top candidates
    HealResult result = Frap.getLastHealResult(submit);
    assertThat(result.healed()).isFalse();
    assertThat(result.topCandidates()).hasSizeGreaterThanOrEqualTo(2);
}
```

See full example: `CfpAmbiguousHealTest` in the [demo showcase](../../internal/demo/showcase/java-playwright).

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
    
    // Access recommended locators from discovered elements
    ElementNode firstElement = map.elements().get(0);
    System.out.println("Selector: " + firstElement.selector());
    System.out.println("Recommended: " + firstElement.recommendedSelector());
    System.out.println("Confidence: " + firstElement.confidence());
}
```

### Filter Element Map

Filter discovered elements interactively:

```java
import io.github.kotlerdev.frap.core.dto.FilterSpec;

// Filter to interactive elements only, minimum cluster size 2
ElementMap filtered = client.filterElementMap(
    map,
    new FilterSpec(true, 2, List.of("button", "a", "input"))
);

// filtered.elements() contains only buttons, links, and inputs
```

**How discover works (not CDP):** `Frap.discover(page)` runs `SnapshotBuilder.build()` → `page.evaluate(...)` in the browser → `client.buildElementMap(...)`. CDP is **not** used on this path. A standalone Chrome/CDP source is on the roadmap (separate transport, same Core API).

### Context capture and RCA

When a test fails, you need to know *why* — UI change, network timeout, or timing. Enable context capture:

```java
var options = new WithFrapOptions()
    .captureAll(true)   // UI + network + console → ContextTimeline
    .debug(true);

Frap.withFrap(page.locator("[data-testid='checkout']"), page, options).click();
```

After failure, Core analyzes the timeline via `analyze_rca`:

```java
RcaReport report = client.analyzeRca(timeline, failureAtMs);
// report.primaryCause() → UI_CHANGE | API_ERROR | INFRASTRUCTURE | FLAKY | UNKNOWN
```

**RCA** (Root Cause Analysis) is post-mortem classification around the failure moment — not healing, not discover. See [Glossary: RCA](../glossary.md#rca-root-cause-analysis).

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

| File | Description |
|------|-------------|
| `frap-report.json` | Summary of all healing events |
| `frap-events.jsonl` | Streaming events (newline-delimited JSON) |
| `frap-debug.html` | Human-readable debug view |
| `frap-debug-explorer.html` | Sidebar view when multiple tests use `debug: true` |
| `debug-reports/*.json` | Per-test detailed debug data |
| `frap-context.json` | Context timeline (when `captureAll(true)`) |
| `frap-rca.json` | RCA report (when failures analyzed) |
| `junit.xml` | JUnit XML with frap properties |

Default location: `target/frap-reports/conference/`

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
- **Capability Matrix:** [java-sdk-1.0.0-matrix.md](../../project/release/java-sdk-1.0.0-matrix.md) — Full coverage of code, tests, docs, and demo
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