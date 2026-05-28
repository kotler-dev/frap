# Frap + Playwright Java (demo)

Example project: wrap Playwright locators so tests survive small UI changes (renamed `data-testid`, refactored markup) with an explainable result — new selector, confidence score, HTML report.

**Packages:** `io.github.kotlerdev.frap.*`  
**Maven:** `groupId` `io.github.kotlerdev.frap`, artifact `frap-playwright`

Live demo walkthrough (2 slides + run): [SHOWCASE.md](SHOWCASE.md)  
Server and environment: [DEMO_SERVER.md](DEMO_SERVER.md)

## Why use it

- **Stable automation** — less time fixing selectors after each UI tweak.
- **Familiar API** — still Playwright `Locator`, wrapped as `FrapLocator`.
- **Explainable** — after a heal you read `HealResult`: was it healed, new selector, confidence, top candidates.
- **Reports** — `frap-debug.html` shows steps, candidate clusters, and the final decision.

## How it works (simple)

1. Wrap a locator: `Frap.withFrap(page.locator("..."), page)`.
2. If the element is missing, Frap snapshots interactive nodes and **compares signatures** to what the test “remembered”.
3. The engine **ranks candidates** (confidence). If two are too close (difference &lt; 0.1), it **refuses** to heal — avoids clicking the wrong button.
4. On success it runs the action with the new selector; in code use `Frap.getLastHealResult(locator)`.
5. With `debug: true`, the HTML report groups similar candidates by structural prefix — that is the **DOM clusters** block (details in [SHOWCASE.md](SHOWCASE.md)).

## Quick start

**Java 17+** and **Maven**.

From the repository root (recommended):

```bash
./scripts/run-java-e2e.sh
```

Or from this directory after [starting the demo server](DEMO_SERVER.md):

```bash
mvn test
```

## Commands

| Command | What it does |
|---------|----------------|
| `mvn test` | All E2E demo tests (`@Tag("e2e")`) |
| `mvn test -Dtest=ScheduleHealingTest` | Self-healing schedule scenario |
| `mvn test -Dtest=ScheduleHealingTest#testOpensTalkAfterRefactor` | Single healing test |
| `mvn test -Dtest=CfpAmbiguousHealTest` | Ambiguous heal refused (CFP) |
| `mvn test -DskipIT` | Skip E2E when environment is not ready |
| `mvn -q exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"` | Install browser once |

## Minimal example

```java
import io.github.kotlerdev.frap.core.dto.HealResult;
import io.github.kotlerdev.frap.playwright.wrapper.Frap;
import io.github.kotlerdev.frap.playwright.wrapper.FrapLocator;

FrapLocator button = Frap.withFrap(
    page.locator("[data-testid='talk-open-healing']"),
    page
);

button.click();

if (Frap.isHealed(button)) {
    HealResult r = Frap.getLastHealResult(button);
    System.out.println("New selector: " + r.selector());
    System.out.println("Confidence: " + r.confidence());
}
```

## Demo tests

| Class | What you learn |
|-------|----------------|
| `ScheduleHealingTest` | Heal after testid change on `schedule-heal.html`; heal policies |
| `CfpAmbiguousHealTest` | Two similar buttons — heal rejected, multiple candidates |
| `PaymentTimeoutTest` | Context capture and RCA on slow API (checkout scenario) |
| `ZzzReportingVerificationTest` | Report files exist after the suite |

Healing demo page: test looks for `talk-open-healing`, HTML has `talk-card-open-healing` — open `http://localhost:3000/conference/schedule-heal.html` in a browser while the server is running.

## Reports

After `mvn test`, open:

```bash
open target/frap-reports/conference/frap-debug.html
```

| File | Description |
|------|-------------|
| `frap-debug.html` | Debug UI: timeline, DOM clusters, top candidates, decision |
| `frap-debug-explorer.html` | Sidebar when several tests use `debug: true` |
| `debug-reports/*.json` | Per-test debug JSON |
| `frap-report.json` | Healing events summary |
| `frap-events.jsonl` | Event stream (one JSON object per line) |
| `junit.xml` | JUnit results |

Enable rich reports on a locator:

```java
Frap.withFrap(locator, page, options.debug(true));
```

## Your own page

Set the base URL if the app is not on `:3000`:

```bash
mvn test -Dtest.server.url=http://localhost:8080
```

Static file or custom server — see [DEMO_SERVER.md](DEMO_SERVER.md).

## Configuration

| Property / env | Default | Meaning |
|----------------|---------|---------|
| `frap.reportDir` | `target/frap-reports/conference` | Report output directory |
| `test.server.url` | `http://localhost:3000` | Demo app base URL |
| `frap.minConfidence` | `0.85` | Minimum confidence to accept a heal |

`FRAP_CORE_BIN` and manual core build — only in [DEMO_SERVER.md](DEMO_SERVER.md).

## Dependency

```xml
<dependency>
    <groupId>io.github.kotlerdev.frap</groupId>
    <artifactId>frap-playwright</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

Use `@ExtendWith(FrapExtension.class)` on test classes for automatic reporting at the end of the suite.
