# Frap Playwright Java

Playwright adapter for Frap self-healing selectors.

## Testing

| Suite | Location | Command |
|-------|----------|---------|
| **Unit** | this module (`src/test`) | `mvn test` |
| **E2E** (browser + `frap-core-rpc` + test-app) | `examples/java/playwright` | `./scripts/run-java-e2e.sh` from repo root |

## Discovery and Page Object generation

```java
// After page.navigate(...)
ElementMap map = Frap.discover(page);
Path out = Path.of("target/generated");
Frap.generatePageObject(page, out, GenerateOptions.javaPlaywright("CatalogPage", "com.example.pages"));
```

Uses bundled `frap-core-rpc` from `frap-core-java` (no Rust toolchain for consumers).

## Quick Start

```java
@ExtendWith(FrapExtension.class)
class ConferenceTest {
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
    }

    @BeforeEach
    void beforeEach() {
        page = browser.newPage();
    }

    @Test
    void testSelfHealing() {
        page.navigate("http://localhost:3000/conference");

        // Wrap locator with Frap healing
        var button = Frap.withFrap(
            page.locator("[data-testid='pay-btn']"),  // Original selector
            page
        );

        // Click with automatic healing if element moved/changed
        button.click();

        // Check if healing occurred
        if (Frap.isHealed(button)) {
            System.out.println("Healed to: " + Frap.getLastHealResult(button).selector());
        }
    }

    @Test
    void testWithContextCapture() {
        // Enable context capture for RCA analysis
        var options = new WithFrapOptions()
            .captureAll(true)
            .debug(true);

        var button = Frap.withFrap(
            page.locator("[data-testid='pay-btn']"),
            page,
            options
        );

        button.click();
    }
}
```

## Configuration

```java
var options = new WithFrapOptions()
    .minConfidence(0.90)           // Higher confidence threshold
    .healPolicy("expect_heal")       // Fail if healing is unexpected
    .reportDir(Path.of("./build/frap-reports"))
    .debug(true);

var button = Frap.withFrap(
    page.locator("[data-testid='btn']"),
    page,
    options
);
```

## Environment Variables

- `FRAP_CORE_BIN` - Path to frap-core-rpc binary
- `FRAP_REPORT_DIR` - Default report directory
- `FRAP_MIN_CONFIDENCE` - Default confidence threshold

## Maven Coordinates

```xml
<dependency>
    <groupId>io.github.kotler-dev</groupId>
    <artifactId>frap-playwright</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

## Features

- **Automatic Healing**: When a selector fails, Frap finds the best matching element
- **Context Capture**: Records network, console, and UI events for RCA analysis
- **JUnit5 Integration**: `@ExtendWith(FrapExtension.class)` for automatic setup
- **Debug Reports**: HTML reports with healing details and context timeline

## API Reference

### `Frap.withFrap(locator, page, options)`

Wraps a Playwright locator with healing capabilities.

### `Frap.getLastHealResult(locator)`

Returns the `HealResult` from the last healing attempt (or null).

### `Frap.isHealed(locator)`

Returns true if the locator was healed in the last action.

### `FrapContext.attach(page, options)`

Attaches context capture (network, console, WebSocket) to a page.

## Reports

With `@ExtendWith(FrapExtension.class)` or `debug(true)`, frap generates:

| File | Description |
|------|-------------|
| `frap-report.json` | Summary of all healing events |
| `frap-debug.html` | Human-readable debug report |
| `junit.xml` | JUnit XML with frap properties |

Default location: `target/frap-reports/conference/`

## See Also

- [sdk/java](../../sdk/java) — Core SDK and documentation index
- [crates/core](../../../crates/core) — Rust core implementation
