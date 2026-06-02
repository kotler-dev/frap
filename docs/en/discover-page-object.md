# Discover and Page Object generation (Java)

How Frap builds a **page map**, groups **list clusters**, and generates a **Playwright Page Object** with locators that work in real browsers.

**Branch:** quality fixes for production DOM audits ship on `develop/java-v1.0.1` (GitHub issues [#7](https://github.com/kotler-dev/frap/issues/7)–[#15](https://github.com/kotler-dev/frap/issues/15)). Maven Central **1.0.0** artifacts are unchanged until the next release; use this branch or build from source for the latest behavior.

---

## Quick start

```java
import io.github.kotlerdev.frap.core.dto.*;
import io.github.kotlerdev.frap.playwright.wrapper.Frap;
import java.nio.file.Path;

// After page.navigate(...) or page.setContent(...)
ElementMap map = Frap.discover(page);

ElementNode first = map.elements().get(0);
System.out.println("Recommended: " + first.recommendedSelector());

// Signature strength (healing / clustering)
System.out.println("Signature confidence: " + first.confidence());
// Selector strategy strength (data-testid > id > … > structural)
System.out.println("Locator confidence: " + first.locator().confidence());

Path out = Path.of("target/generated");
Frap.generatePageObject(
    page,
    out,
    GenerateOptions.javaPlaywright("CatalogPage", "com.example.pages")
);
```

**How it runs:** `Frap.discover(page)` → `SnapshotBuilder` (`page.evaluate` in the browser) → `frap-core-rpc` `build_element_map` → JSON `ElementMap`. CDP is **not** used on this path.

---

## Recommended locators

Core picks the most stable locator per element. Rules consumers can rely on:

| Priority | Strategy | Example |
|----------|----------|---------|
| 1 | `data-testid` | `[data-testid="pay"]` |
| 2 | `id` (non-generated) | `#checkout` |
| 3 | `data-id` | `[data-id="row-1"]` |
| 4 | `name` | `[name="email"]` |
| 5 | `aria-label` | `button[aria-label="Close"]` |
| 6 | visible text | `button:has-text("Pay")` |
| 7 | structural | `button:nth-of-type(2)` |

**Playwright compatibility**

- Text locators use Playwright **`:has-text("...")`**, not jQuery **`:contains()`** (invalid in Playwright).
- Empty or whitespace-only text does **not** produce `has-text("")`; Core falls back to structural selectors.
- Dynamic or personal text (amounts, currency-heavy strings) and CSS leaked from `<style>` / `<script>` are **not** embedded in locators when detected in Core.

**Snapshot text (Java adapter)**

`SnapshotBuilder` records **visible** text via `innerText` (with `textContent` fallback), so inline CSS inside SVG/icons does not pollute `text_content`. TypeScript `wrapper.ts` uses the same pattern for healing snapshots.

---

## Clusters and `items(index)`

Repeated UI blocks (cards, list rows) are grouped into **LIST** clusters. The generated Page Object exposes one accessor per list:

```java
/** Cluster `cluster_...` (N elements) */
public Locator items(int index) {
    return page.locator("article[data-testid=\"product-card\"]").nth(index);
}
```

**Shared selector behavior**

- If every member shares the same `recommended_selector` (e.g. same `data-testid`), that selector is reused with `.nth(index)`.
- If members have different ids, Frap uses a **structural** descendant selector from the common path prefix (e.g. `div nav a`), not the first member’s unique `#id`.

**Dedup and names**

- Identical `recommended_selector` values produce **one** method, not many copies.
- Elements covered by a list `items(index)` are **not** emitted again as individual methods.
- Method names come from `data-testid`, `aria-label`, semantic `id`, `name`, or visible text — not synthetic `acssEl56` ids.

**Mega-list guard**

Clusters with **≥ 8** members where **every** `recommended_selector` is unique (typical “everything under `main`” on deep SPAs) are demoted to **Single** so Page Objects do not expose one useless `items(index)` over dozens of unrelated links. Smaller nav lists (2–7 items) still get `items(index)` with a structural selector when needed.

Different **leaf tags** (`<a>` vs `<button>`) under the same path prefix are never merged into one LIST.

---

## Filtering the map

```java
import io.github.kotlerdev.frap.core.FrapCoreClient;

ElementMap filtered = client.filterElementMap(
    map,
    new FilterSpec(true, 2, List.of("button", "a", "input"))
);
// interactive_only=true, min cluster size 2, tag allowlist
```

---

## Confidence: two scales

Each `ElementNode` exposes two numbers on purpose:

| Field | Meaning |
|-------|---------|
| `confidence()` | Structural **signature** strength (stable attrs, path) — used for healing and clustering |
| `locator().confidence()` | **Selector** reliability from locator strategy (`data-testid` highest, structural lowest) |

They are not bugs and should not be merged; both should be monotonic with element quality (better `data-testid` → higher on both scales).

---

## Maven coordinates

```xml
<dependency>
    <groupId>io.github.kotler-dev</groupId>
    <artifactId>frap-core-java</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>io.github.kotler-dev</groupId>
    <artifactId>frap-playwright</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

---

## Verify locally

```bash
# From repository root
cd crates && cargo test -p frap-core
cd adapters/playwright-java && mvn test -Dtest=SnapshotBuilderVisibleTextTest
./scripts/run-java-e2e.sh
```

Demo: [examples/java/playwright](../../examples/java/playwright/) (`DiscoveryPageObjectE2eTest`).

---

## Limits and roadmap

| Topic | Status |
|-------|--------|
| CLI `frap discover` | Not in Java SDK; build RPC/CLI from repo if needed |
| TypeScript Page Object export | Backlog (npm 1.1.x has healing, not discover/PoG) |
| `frap:` custom selectors in generated PO | Backlog |
| Selenium / WebDriver | F014 track B, v1.4+ |

For healing (not discover), see [Playwright Java adapter](../../adapters/playwright-java/README.md).
