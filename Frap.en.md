# Frap — Design Document

> Deterministic DOM binding for stable selectors.
> *"When the DOM gets rough, Frap keeps your selectors tight."*

**Languages:** [English](Frap.en.md) · [Русский](Frap.md)

---

## Name

**Frap** — from the nautical term "to frap": to bind and tighten ropes together securely.

**CLI / selector:** `frap`, `frap:[data-testid="…"]`

**Site:** https://github.com/kotler-dev/frap (main entry point)

**Metaphor:** Your tool "binds" selectors to DOM structure so they do not "flap around" when the UI changes.

### Artifact cheat sheet

```
Brand / CLI:     frap
Site:            https://github.com/kotler-dev/frap

npm:             @frap/frap, @frap/frap-playwright
Maven:           io.github.kotlerdev:frap-core, :frap-selenide, …
PyPI:            frap-sdk, frap-playwright
Cargo:           frap-sdk (+ frap-context, frap-rca)
GitHub:          kotler-dev/frap
```

| Registry | SDK / core | Playwright | Selenide / Selenium |
|----------|------------|------------|---------------------|
| **npm** | `@frap/frap` | `@frap/frap-playwright` | — (later) |
| **Maven** | `io.github.kotlerdev:frap-core` | `…:frap-playwright` | `…:frap-selenide`, `…:frap-selenium` |
| **PyPI** | `frap-sdk` | `frap-playwright` | — |
| **Cargo** | `frap-sdk` | — | — |

Maven `groupId`: **`io.github.kotlerdev`** (GitHub `kotler-dev` → no hyphen in Java).  
Unscoped `npm i frap` is taken by another package; use **`@frap/…`**.  
On PyPI/crates.io the name **`frap`** is taken — core publishes as **`frap-sdk`**.

### One-liner per language

**TypeScript**

```bash
npm i @frap/frap @frap/frap-playwright
```

```
frap bind --selector '[data-testid="submit"]' --output submit.signature.json
import { frap } from '@frap/frap'
page.locator('frap:[data-testid="submit"]').click()
```

**Java (Selenide)**

```xml
<dependency>
  <groupId>io.github.kotlerdev</groupId>
  <artifactId>frap-selenide</artifactId>
</dependency>
```

```
frap bind --selector "[data-testid='submit']" --output submit.signature.json
import io.github.kotlerdev.frap.selenide.Frap;
Frap.bind($("[data-testid='submit']"));
```

**Python**

```bash
pip install frap-sdk frap-playwright
```

```
frap bind --selector '[data-testid="submit"]' --output submit.signature.json
from frap import Frap
await Frap.bind(page.locator('[data-testid="submit"]'))
```

**Rust**

```bash
cargo add frap-sdk
```

---

## Unified command vocabulary

### Concept: one DSL for all adapters

Each adapter (Selenide, Selenium, Playwright) supports a **top set** of commands with semantically identical behavior.

| Command | Semantics | Selenide | Selenium | Playwright |
|---------|-----------|----------|----------|------------|
| **bind** | Bind selector → extract signature | `Frap.bind($("#btn"))` | `Frap.bind(driver.findElement(By.id("btn")))` | `Frap.bind(page.locator("#btn"))` |
| **getSignature** | Get signature from binding | `.getSignature()` | `.getSignature()` | `.getSignature()` |
| **locate** | Resolve signature in current DOM | `sig.locate()` | `sig.locate(driver)` | `sig.locate(page)` |
| **rebind** | Refresh binding (re-scan DOM) | `.rebind()` | `.rebind(driver)` | `.rebind(page)` |
| **unbound** | Find unbound elements | `Frap.unbound($(".dynamic"))` | `Frap.unbound(driver, By.css(".dynamic"))` | `Frap.unbound(page, ".dynamic")` |
| **serialize** | Serialize signature to JSON | `.serialize()` / `.serializeTo(file)` | `.serialize()` / `.serializeTo(file)` | `.serialize()` / `.serializeTo(file)` |
| **deserialize** | Deserialize signature | `Frap.deserialize(json)` | `Frap.deserialize(json)` | `Frap.deserialize(json)` |

### Configuration (keys)

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `confidence` | `float` | `0.85` | Minimum score for healing |
| `maxCandidates` | `int` | `5` | Max candidates in report |
| `useVisual` | `boolean` | `false` | Use visual features |
| `debug` | `boolean` | `false` | Enable debug output |
| `healStrategy` | `string` | `"auto"` | Healing strategy: `auto`, `fail`, `report` |

### Recommended settings by scenario

| Parameter | Development | CI Strict | CI Permissive | Debugging |
|-----------|-------------|-----------|---------------|-----------|
| `confidence` | 0.85 | 0.95 | 0.80 | 0.70 |
| `maxCandidates` | 5 | 3 | 10 | 10 |
| `healStrategy` | `auto` | `fail` | `report` | `auto` |
| `debug` | `false` | `false` | `true` | `true` |

### Configuration example

```java
// Global configuration
Frap.config()
    .setConfidence(0.90)
    .setMaxCandidates(3)
    .setHealStrategy(HealStrategy.FAIL)
    .setDebug(true);

// Per-call override
Resolution res = sig.locate(driver, Frap.config()
    .setConfidence(0.95));
```

---

## Code examples

### Java (Selenium)

```java
import io.github.kotlerdev.frap.selenium.Frap;
import io.github.kotlerdev.frap.core.Binding;
import io.github.kotlerdev.frap.core.Signature;
import io.github.kotlerdev.frap.core.Resolution;

// Bind — extract signature
Binding binding = Frap.bind(driver.findElement(By.cssSelector("[data-testid='submit']")));
Signature signature = binding.getSignature();

// Persist signature for versioning
String json = signature.serialize();  // or serializeTo("submit.signature.json")

// Later — resolve (healing if needed)
Signature loaded = Frap.deserialize(json);
Resolution res = loaded.locate(driver);

if (res.isHealed()) {
    System.out.println("Selector healed: " + res.getHealedSelector());
    System.out.println("Confidence: " + res.getConfidence());
}

WebElement element = res.getElement();

// Refresh binding after DOM changes
binding.rebind(driver);
signature.serializeTo("submit-v2.signature.json");
```

### Java (Playwright)

```java
import io.github.kotlerdev.frap.playwright.Frap;
import io.github.kotlerdev.frap.core.Binding;
import io.github.kotlerdev.frap.core.Signature;
import io.github.kotlerdev.frap.core.Resolution;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;

// Bind — extract signature
Binding binding = Frap.bind(page.locator("[data-testid='submit']"));
Signature signature = binding.getSignature();

// Persist signature for versioning
String json = signature.serialize();  // or serializeTo("submit.signature.json")

// Later — resolve (healing if needed)
Signature loaded = Frap.deserialize(json);
Resolution res = loaded.locate(page);

if (res.isHealed()) {
    System.out.println("Selector healed: " + res.getHealedSelector());
    System.out.println("Confidence: " + res.getConfidence());
}

Locator element = res.getLocator();
element.click();

// Refresh binding after DOM changes
binding.rebind(page);
signature.serializeTo("submit-v2.signature.json");
```

### Kotlin (Selenide-style)

```kotlin
import io.github.kotlerdev.frap.selenide.frap

// Selenide extension
val element = $("[data-testid='submit']")
    .frap()                  // bind
    .getSignature()          // get signature
    .locate()                // resolve (healing if needed)

// Or explicitly:
val signature = $("#username").frap().getSignature()

// Serialization
signature.serializeTo("signatures/username.json")

// Load and resolve
val loaded = Frap.deserialize(File("signatures/username.json"))
val resolved = loaded.locate(driver)

// Debug
println(resolved.debug.candidates)  // why this element was chosen
println(resolved.debug.diff)        // what changed in the DOM

// Refresh binding
$("#username").frap().rebind()
```

### TypeScript (Playwright)

```typescript
import { frap } from '@frap/frap';

// Bind
const binding = await frap.bind(page.locator('[data-testid="submit"]'));
const signature = binding.getSignature();

// Serialize for CI
await signature.serializeTo('signatures/submit.json');

// Resolve with healing
const loaded = await frap.deserialize('signatures/submit.json');
const resolved = await loaded.locate(page);

if (resolved.healed) {
  console.log(`Healed! New selector: ${resolved.healedSelector}`);
  console.log(`Confidence: ${resolved.confidence}`);
  console.log('Candidates:', resolved.debug.candidates);
}

await resolved.element.click();

// Refresh binding after refactor
await binding.rebind(page);
await signature.serializeTo('signatures/submit-v2.json');
```

### Python (Playwright/Pytest)

```python
from frap import Frap, Signature

# Bind
binding = await Frap.bind(page.locator("[data-testid='submit']"))
signature = binding.get_signature()

# Serialize
signature.serialize_to("signatures/submit.json")

# Resolve (healing possible)
loaded = Frap.deserialize("signatures/submit.json")
resolved = await loaded.locate(page)

if resolved.healed:
    print(f"Selector healed: {resolved.healed_selector}")
    print(f"Confidence: {resolved.confidence}")
    print(f"Candidates: {resolved.debug.candidates}")

await resolved.element.click()

# Refresh binding
await binding.rebind(page)

# Page Object integration
from frap.pytest import frap_fixture

@frap_fixture
def login_page(page):
    return {
        "username": Frap.bind(page.locator("[name='username']")),
        "password": Frap.bind(page.locator("[name='password']")),
        "submit": Frap.bind(page.locator("[type='submit']")),
    }
```

---

## CLI utility

### Installation

```bash
# npm (Playwright / Node)
npm install @frap/frap @frap/frap-playwright
# CLI (if exposed as bin in @frap/frap)
npx frap --help

# PyPI
pip install frap-sdk frap-playwright

# Cargo (native performance)
cargo install frap-sdk

# Homebrew (when formula is available)
brew install frap
```

### Commands

```bash
# 🔗 bind — bind selector, extract signature
frap bind --selector "[data-testid='submit']" --output submit.signature.json

# 🔍 locate — resolve signature, find element
frap locate --signature submit.signature.json --url https://app.example.com

# 🔄 rebind — refresh binding (re-scan)
frap rebind --signature submit.signature.json --output submit-v2.signature.json

# 📊 scan — scan page, find bindable elements
frap scan --url https://app.example.com --output site-map.json

# 🔎 unbound — find unbound / unstable elements
frap unbound --url https://app.example.com --selector ".dynamic-class"

# 📈 report — generate stability report
frap report --signatures-dir ./signatures/ --output stability-report.html

# 🧪 test — verify all signatures resolve
frap test --signatures-dir ./signatures/ --url https://app.example.com

# 🗂️  diff — compare two DOM states
frap diff --signature-v1 submit-v1.signature.json --signature-v2 submit-v2.signature.json

# ⚙️  config — show / set configuration
frap config set confidence 0.90
frap config set maxCandidates 3
frap config show
```

### CLI usage examples

```bash
# 1. Quick start: scan and bind key elements
frap scan --url https://app.example.com \
  --selectors "[data-testid]" \
  --output onboarding.signatures/

# 2. CI: verify all elements resolve
frap test --signatures-dir ./signatures/ --url https://staging.example.com \
  --fail-on-healed \
  --report ci-report.json

# 3. Update after refactor
frap rebind --signatures-dir ./signatures/ --url https://app.example.com \
  --backup-dir ./signatures/backup/ \
  --dry-run  # preview changes first

# 4. Playwright integration
frap scan --url http://localhost:3000 --format playwright-po \
  --output tests/pages/LoginPage.frap.ts
```

---

## Core Concepts

### Signature Structure

```json
{
  "version": "1.0",
  "anchor": {
    "selector": "form.login",
    "features": { "tag": "form", "id": "login-form" }
  },
  "path": {
    "steps": [{ "index": 2, "tag": "button" }]
  },
  "features": {
    "text": "Submit",
    "attributes": { "type": "submit", "data-testid": "submit-btn" },
    "visualHash": "a1b2c3d4"
  },
  "fallback": {
    "originalSelector": "[data-testid='submit-btn']"
  }
}
```

### Resolution Strategy

1. **Exact Match** — anchor + path unchanged
2. **Anchor Drift** — anchor moved, path valid
3. **Structural Shift** — anchor stable, path changed
4. **Healing Required** — fuzzy matching via feature similarity
5. **Failure** — no candidates above confidence threshold

---

## Error Handling & Edge Cases

### Resolution Failures

| Scenario | Behavior | Recommendation |
|----------|----------|----------------|
| **Zero candidates** | `Resolution.failure()` — element not found | Check selector, run rebind |
| **Ambiguous match** | Multiple candidates with same score | Manual pick or raise confidence threshold |
| **Low confidence** | Score below threshold | Inspect `debug.candidates`, rebind |
| **Stale signature** | Signature version mismatch | Migrate via `rebind` |

### Configuration Behavior

```java
// Strict — fail on any healing
Frap.config().setHealStrategy(HealStrategy.FAIL);

// Report — continue but record healing
Frap.config().setHealStrategy(HealStrategy.REPORT);

// Auto (default) — transparent healing
Frap.config().setHealStrategy(HealStrategy.AUTO);
```

---

## Workflow Patterns

### 1. Initial Binding (Development)

```java
// Record stable selectors during development
Signature sig = Frap.bind(page.locator("#submit")).getSignature();
sig.serializeTo("signatures/login-submit.json");
```

### 2. CI Verification (Strict Mode)

```bash
frap test --signatures-dir ./signatures/ \
  --confidence 0.95 \
  --fail-on-healed \
  --url https://staging.example.com
```

### 3. Post-Refactor Update

```bash
# Preview changes
frap rebind --signatures-dir ./signatures/ \
  --url https://app.example.com \
  --dry-run

# Apply with backup
frap rebind --signatures-dir ./signatures/ \
  --url https://app.example.com \
  --backup-dir ./signatures/backup/
```

### 4. Debugging Failed Resolutions

```typescript
const resolved = await sig.locate(page);

if (!resolved.success) {
  console.log('Failed to locate element');
  console.log('Candidates considered:', resolved.debug.candidates);
  console.log('DOM changes since binding:', resolved.debug.diff);
  console.log('Suggested action:', resolved.debug.recommendation);
}
```

---

## Comparison: Traditional vs Frap

| Scenario | Traditional | With Frap |
|----------|-------------|-----------|
| Simple click | `page.click("#btn")` | `Frap.bind("#btn").getSignature().locate(page).click()` |
| Data-testid refactor | Broken tests | Auto-healing with report |
| Dynamic class names | Flaky selectors | Structural signature matching |
| Cross-version stability | Manual updates | `rebind` workflow |
| Debugging failures | Console screenshots | Structured diff + candidates |

---

## Terminology

| Term | Meaning |
|------|---------|
| **Frap** | Brand / product name |
| **frap** | CLI, `frap:[…]` selector, artifact prefix (`frap-sdk`, `frap-core`, …) |
| **github.com/kotler-dev/frap** | Main repository and documentation |
| **@frap/** | npm scope (org `frap` on npmjs.com) |
| **bind** | Bind a selector to DOM structure |
| **signature** | Element signature — structural features for identification |
| **anchor** | Stable parent element for a signature |
| **rebind** | Refresh binding (re-scan DOM) after changes |
| **locate** | Resolve signature to an element in current DOM |
| **resolution** | Process of resolving a signature to an element |
| **serialize** | Serialize signature to JSON |
| **deserialize** | Deserialize signature from JSON |
| **unbound** | Element without binding (potentially unstable) |
| **healed** | Element found via clustering when match is inexact |
| **confidence** | Healing confidence score (0.0–1.0) |

---

## API Summary

```
Frap.bind(selector) → Binding → getSignature() → Signature
                                      ↓
                              serialize() → JSON
                                      ↓
                              deserialize() → Signature
                                      ↓
                              locate(context) → Resolution
                                      ↓
                         ┌────────────┼────────────┐
                         ↓            ↓            ↓
                      success      healed       failure
                         ↓            ↓            ↓
                      element   candidates    debug info
                                + diff
                              + confidence
```

**One-liner:**
> *"Frap binds your selectors to structure — deterministic binding, automatic healing, LLM-ready grounding."*
