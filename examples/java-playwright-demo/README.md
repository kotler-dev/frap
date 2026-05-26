# Frap Java Playwright Demo

Demo project showing Frap self-healing selectors with Playwright Java.

## Prerequisites

1. **Rust toolchain** - for building `frap-core-rpc` binary
2. **Java 17+** - for running Maven
3. **Maven** - for building and testing
4. **Node.js** (optional) - for running the test server

## Quick Start

### 1. Build frap-core-rpc Binary

```bash
cd ../../crates
cargo build --release -p frap-core --bin frap-core-rpc
```

### 2. Start Test Server

```bash
cd ../../test-app
node server.js &
```

Or with Maven (if you have Node.js):
```bash
cd ../..
npm install
node test-app/server.js &
```

### 3. Run Tests

```bash
# With test server running on localhost:3000
mvn test

# Or skip integration tests
mvn test -DskipIT
```

## Test Structure

### Conference Tests (`io.frap.demo.conference`)

- `ScheduleHealingTest` - Self-healing when data-testid changes
- `ReportingVerificationTest` - Verifies report artifacts (CP005)

### Context Tests (`io.frap.demo.context`)

- `PaymentTimeoutTest` - RCA analysis for API failures (C002)

## Report Artifacts

After running tests, the following artifacts are generated in `target/frap-reports/conference/`:

| File | Description |
|------|-------------|
| `frap-events.jsonl` | Healing events in JSONL format |
| `frap-report.json` | Aggregated report with summary |
| `frap-context.json` | Context timeline (if captureAll enabled) |
| `frap-rca.json` | RCA analysis report |
| `junit.xml` | JUnit XML test results |
| `debug-reports/*.json` | Per-test debug reports |

## Example Usage

```java
@Test
void testSelfHealing() {
    page.navigate("http://localhost:3000/conference/schedule-heal.html");

    // Wrap locator with Frap - automatically heals on selector change
    FrapLocator button = Frap.withFrap(
        page.locator("[data-testid='talk-open-healing']"),  // Old selector
        page
    );

    button.click();

    // Check if healing occurred
    if (Frap.isHealed(button)) {
        HealResult result = Frap.getLastHealResult(button);
        System.out.println("Healed to: " + result.selector());
        System.out.println("Confidence: " + result.confidence());
    }
}
```

## Configuration

### System Properties

- `frap.reportDir` - Report output directory (default: `target/frap-reports/conference`)
- `test.server.url` - Test server URL (default: `http://localhost:3000`)
- `frap.minConfidence` - Minimum healing confidence (default: 0.85)

### Environment Variables

- `FRAP_CORE_BIN` - Path to `frap-core-rpc` binary

## Comparison with TypeScript

| TypeScript | Java |
|------------|------|
| `withFrap(locator, page, options)` | `Frap.withFrap(locator, page, options)` |
| `getLastHealResult(locator)` | `Frap.getLastHealResult(locator)` |
| `attachFrapContext(page, { reportDir })` | `FrapContext.attach(page, new ContextCaptureOptions(reportDir))` |
| `@ExtendWith(FrapExtension)` | `@ExtendWith(FrapExtension.class)` |

## Troubleshooting

### Binary not found
```
FrapCoreClient Exception: frap-core-rpc binary not found
```
**Fix**: Build the binary with `cargo build --release -p frap-core --bin frap-core-rpc`

### Server not running
```
Timeout waiting for selector
```
**Fix**: Start the test server with `node test-app/server.js`

## See Also

- [../../sdk/java](../../sdk/java) - Core SDK
- [../../adapters/playwright-java](../../adapters/playwright-java) - Playwright adapter
- [../../docs/en/java-sdk-rpc.md](../../docs/en/java-sdk-rpc.md) - RPC protocol
