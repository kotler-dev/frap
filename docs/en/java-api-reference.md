# Java SDK API Reference (1.0.0)

Complete reference for Frap Java SDK public API.

## Overview

| Module | Maven Coordinates | Description |
|--------|-------------------|-------------|
| `frap-core-java` | `io.github.kotler-dev:frap-core-java:1.0.0` | Core client with JSON-RPC transport |
| `frap-playwright` | `io.github.kotler-dev:frap-playwright:1.0.0` | Playwright adapter with healing, discovery, PO generation |

---

## Core Client (`frap-core-java`)

### Interface: `FrapCoreClient`

Main entry point for all frap operations. Implementations: `FrapRpcClient` (JSON-RPC).

```java
public interface FrapCoreClient extends AutoCloseable {
    HealResult heal(HealRequest request) throws IOException;
    RcaReport analyzeRca(ContextTimeline timeline, long failureAtMs) throws IOException;
    ElementMap buildElementMap(DOMSnapshot snapshot, MapOptions options) throws IOException;
    ElementMap filterElementMap(ElementMap map, FilterSpec spec) throws IOException;
    GeneratedArtifact generatePageObject(ElementMap map, GenerateOptions options) throws IOException;
    boolean isAlive();
    long pid();
    void close();
}
```

### Class: `FrapRpcClient`

JSON-RPC implementation with bundled binary auto-extraction.

| Method | Description |
|--------|-------------|
| `static FrapRpcClient create()` | Create client with auto-detected binary (bundled, FRAP_CORE_BIN, or dev paths) |
| `static FrapRpcClient create(String binaryPath)` | Create with explicit binary path |

**Binary Resolution Order:**
1. `FRAP_CORE_BIN` environment variable
2. Development paths (`crates/target/release/frap-core-rpc`)
3. Bundled binary extracted from JAR to temp directory

**Bundled Binaries (1.0.0):**
- Linux x86_64 (glibc): `META-INF/native/frap-core-rpc-linux-x86_64`
- Linux x86_64 (musl): `META-INF/native/frap-core-rpc-linux-x86_64-musl`
- macOS aarch64: `META-INF/native/frap-core-rpc-macos-aarch64`

**Not bundled:** macOS x86_64 (Intel), Windows — set `FRAP_CORE_BIN`.

---

## DTOs (Data Transfer Objects)

### Healing

#### `HealRequest`

```java
public record HealRequest(
    @JsonProperty("primary_selector") String primarySelector,
    @JsonProperty("original_signature") Signature originalSignature,
    @JsonProperty("dom_snapshot") DOMSnapshot domSnapshot,
    @JsonProperty("min_confidence") double minConfidence
) {}
```

| Field | Type | Description |
|-------|------|-------------|
| `primarySelector` | `String` | Original CSS selector (e.g., `[data-testid='pay-btn']`) |
| `originalSignature` | `Signature` | Structural signature from original element |
| `domSnapshot` | `DOMSnapshot` | Current page DOM state |
| `minConfidence` | `double` | Minimum confidence threshold (0.0-1.0, typically 0.85) |

#### `HealResult`

```java
public record HealResult(
    @JsonProperty("healed") boolean healed,
    @JsonProperty("selector") String selector,
    @JsonProperty("confidence") double confidence,
    @JsonProperty("diff") String diff,
    @JsonProperty("top_candidates") List<Candidate> topCandidates,
    @JsonProperty("original_signature") Signature originalSignature,
    @JsonProperty("semantics") HealingSemantics semantics  // May be null if no healing attempted
) {}
```

| Field | Type | Description |
|-------|------|-------------|
| `healed` | `boolean` | True if element was found and healed |
| `selector` | `String` | New selector (or original if not healed) |
| `confidence` | `double` | Match confidence score |
| `diff` | `String` | Human-readable description of changes |
| `topCandidates` | `List<Candidate>` | Ranked candidate elements |

#### `Signature`

Structural signature for element matching.

```java
public record Signature(
    @JsonProperty("path") List<DOMToken> path,
    @JsonProperty("prefix") String prefix,
    @JsonProperty("stable_attrs") Map<String, String> stableAttrs,
    @JsonProperty("text_content") String textContent,
    @JsonProperty("position_in_parent") Integer positionInParent,
    @JsonProperty("children_hash") int childrenHash,
    @JsonProperty("depth") int depth
) {}
```

#### `Candidate`

```java
public record Candidate(
    @JsonProperty("selector") String selector,
    @JsonProperty("signature") Signature signature,
    @JsonProperty("confidence") double confidence
) {}
```

### DOM Snapshot

#### `DOMSnapshot`

```java
public record DOMSnapshot(
    @JsonProperty("html") String html,
    @JsonProperty("elements") List<DOMElementInfo> elements
) {}
```

#### `DOMElementInfo`

```java
public record DOMElementInfo(
    @JsonProperty("selector") String selector,
    @JsonProperty("tag") String tag,
    @JsonProperty("attributes") Map<String, String> attributes,
    @JsonProperty("text_content") String textContent,
    @JsonProperty("path") List<String> path,
    @JsonProperty("position_in_parent") Integer positionInParent
) {}
```

### Element Discovery

#### `ElementMap`

Result of `buildElementMap` — a **flat catalog** of page elements after discover (not a site map, not a DOM tree API).

Two lists: `elements` and `clusters` (linked via `cluster_id`). There are **no** `parent_id` / `children` fields on `ElementNode`; hierarchy is captured inside each element's `signature.path` (ancestor chain `tag:role` recorded at snapshot time).

**Discover path (Playwright):** `Frap.discover(page)` → `SnapshotBuilder.build()` → `page.evaluate(...)` → `buildElementMap(...)`. **CDP is not used** on this path today.

| Concept | Meaning |
|---------|---------|
| `ElementNode.id` (`el-0`, …) | Catalog key for **this** discover run |
| `Signature` | Cross-run identity for heal/resolution when locators break |
| `position_in_parent` | Same-tag sibling index in snapshot — used in confidence scoring, **not** as `:nth-child()` in `recommended_selector` |

Traverse with stream/filter on `elements()` and `clusters()`. To re-identify an element after DOM changes, compare stored signatures against a **new** snapshot (heal), optionally scoped to the same cluster first.

```java
public record ElementMap(
    @JsonProperty("elements") List<ElementNode> elements,
    @JsonProperty("clusters") List<Cluster> clusters,
    @JsonProperty("metadata") MapMetadata metadata
) {}
```

#### `ElementNode`

```java
public record ElementNode(
    @JsonProperty("id") String id,
    @JsonProperty("selector") String selector,
    @JsonProperty("recommended_selector") String recommendedSelector,
    @JsonProperty("tag") String tag,
    @JsonProperty("signature") Signature signature,
    @JsonProperty("cluster_id") String clusterId,
    @JsonProperty("confidence") double confidence,
    @JsonProperty("locator") LocatorRecommendation locator
) {}
```

#### `Cluster`

```java
public record Cluster(
    @JsonProperty("id") String id,
    @JsonProperty("cluster_type") ClusterType clusterType,
    @JsonProperty("element_ids") List<String> elementIds,
    @JsonProperty("prefix_signature") String prefixSignature
) {}
```

#### `ClusterType`

```java
public enum ClusterType {
    SINGLE,   // One element — unique control (submit button, single field)
    LIST,     // ≥2 similar elements — repeating template (product cards, table rows)
    UNKNOWN   // Reserved
}
```

#### `MapOptions`

```java
public record MapOptions(
    @JsonProperty("url") String url,
    @JsonProperty("include_non_interactive") boolean includeNonInteractive,
    @JsonProperty("max_elements") Integer maxElements
) {
    public static MapOptions defaults() {
        return new MapOptions(null, true, null);
    }
}
```

#### `FilterSpec`

```java
public record FilterSpec(
    @JsonProperty("interactive_only") boolean interactiveOnly,
    @JsonProperty("min_cluster_size") int minClusterSize,
    @JsonProperty("include_tags") List<String> includeTags
) {}
```

### Page Object Generation

#### `GenerateOptions`

```java
public record GenerateOptions(
    @JsonProperty("language") String language,
    @JsonProperty("class_name") String className,
    @JsonProperty("package_name") String packageName,
    @JsonProperty("include_signatures") Boolean includeSignatures
) {
    public static GenerateOptions javaPlaywright(String className, String packageName) {
        return new GenerateOptions("java_playwright", className, packageName, true);
    }
}
```

| Language | Description |
|----------|-------------|
| `"java_playwright"` | Java with Playwright locators (1.0.0) |

#### `GeneratedArtifact`

```java
public record GeneratedArtifact(
    @JsonProperty("files") List<GeneratedFile> files
) {}
```

#### `GeneratedFile`

```java
public record GeneratedFile(
    @JsonProperty("path") String path,
    @JsonProperty("content") String content
) {}
```

### RCA (Root Cause Analysis)

**`analyze_rca`** — post-mortem analysis of a `ContextTimeline` (UI events, network, console/logs) around the failure moment. Core classifies the likely cause and returns `RcaReport`. This is **not** healing and **not** discover.

```java
RcaReport analyzeRca(ContextTimeline timeline, long failureAtMs) throws IOException;
```

| `PrimaryCause` | Typical signal |
|----------------|----------------|
| `UI_CHANGE` | Locator failure, DOM drift |
| `API_ERROR` | API request failed (5xx, timeout) |
| `INFRASTRUCTURE` | Network-level failure (DNS, connection) |
| `FLAKY` | Inconsistent failure pattern |
| `UNKNOWN` | Insufficient context |

Timeline is built by the Playwright adapter (`captureAll(true)`); Core only classifies the JSON stream.

#### `RcaReport`

```java
public record RcaReport(
    @JsonProperty("version") int version,
    @JsonProperty("primary_cause") PrimaryCause primaryCause,
    @JsonProperty("confidence") double confidence,
    @JsonProperty("timeline_excerpt") List<ContextEvent> timelineExcerpt,
    @JsonProperty("recommendation") String recommendation,
    @JsonProperty("details") CauseDetails details
) {}
```

#### `PrimaryCause`

```java
public enum PrimaryCause {
    UI_CHANGE,        // Element changed in DOM
    API_ERROR,        // API request failed
    INFRASTRUCTURE,   // Network-level failure
    FLAKY,            // Inconsistent failure pattern
    UNKNOWN           // Could not determine
}
```

#### `ContextTimeline`

```java
public record ContextTimeline(
    @JsonProperty("events") List<ContextEvent> events
) {}
```

#### `ContextEvent`

```java
public record ContextEvent(
    @JsonProperty("kind") String kind,          // "ui", "network", "console", "log"
    @JsonProperty("timestamp_ms") long timestampMs,
    @JsonProperty("trace_id") String traceId,
    @JsonProperty("element") String element,    // for UI events
    @JsonProperty("action") String action,      // "click", "failure", etc.
    @JsonProperty("detail") String detail       // Error message or details
) {}
```

### Locator Recommendation

#### `LocatorRecommendation`

```java
public record LocatorRecommendation(
    @JsonProperty("selector") String selector,
    @JsonProperty("strategy") String strategy,    // "data-testid", "css", "role", etc.
    @JsonProperty("confidence") double confidence
) {}
```

### DOM Token

#### `DOMToken`

```java
public record DOMToken(
    @JsonProperty("tag") String tag,
    @JsonProperty("role") String role,
    @JsonProperty("semantic_type") String semanticType,
    @JsonProperty("structural_class") String structuralClass,
    @JsonProperty("depth") int depth
) {}
```

---

## Playwright Adapter (`frap-playwright`)

### Class: `Frap`

Main API for Playwright integration.

#### Healing Methods

| Method | Description |
|--------|-------------|
| `static FrapLocator withFrap(Locator locator, Page page)` | Wrap locator with healing |
| `static FrapLocator withFrap(Locator locator, Page page, WithFrapOptions options)` | Wrap with options |
| `static FrapLocator withFrap(Page page, String selector)` | Create wrapped locator from selector |
| `static FrapLocator withFrap(Page page, String selector, WithFrapOptions options)` | Create with options |
| `static HealResult getLastHealResult(Locator locator)` | Get healing result from last action |
| `static boolean isHealed(Locator locator)` | Check if locator was healed |

#### Discovery Methods

| Method | Description |
|--------|-------------|
| `static ElementMap discover(Page page)` | Build element map with defaults |
| `static ElementMap discover(Page page, MapOptions options)` | Build with custom options |
| `static List<Path> generatePageObject(Page page, Path outputDir, GenerateOptions options)` | Discover, generate PO, write files |

#### Utility Methods

| Method | Description |
|--------|-------------|
| `static void clearSignatures()` | Clear cached signatures |
| `static void clearClient()` | Close and clear RPC client |

### Class: `FrapLocator`

Playwright `Locator` wrapper with healing on failure.

```java
public class FrapLocator {
    public void click();
    public void click(ClickOptions options);
    public void fill(String value);
    public void press(String key);
    public Locator first();
    public Locator nth(int index);
    public Locator locator(String selector);
    // ... other Locator methods delegated
}
```

### Configuration

#### `WithFrapOptions`

```java
public class WithFrapOptions {
    public WithFrapOptions minConfidence(double confidence);
    public WithFrapOptions healPolicy(HealPolicy policy);
    public WithFrapOptions captureAll(boolean capture);
    public WithFrapOptions debug(boolean debug);
    public WithFrapOptions reportDir(Path dir);
}
```

#### `HealPolicy`

```java
public enum HealPolicy {
    ALLOW,       // Always heal if confident enough
    DENY,        // Never heal (fail on selector not found)
    EXPECT_HEAL  // Expect healing (fail if no healing needed)
}
```

#### `HealTrigger`

What triggered the healing attempt.

```java
public enum HealTrigger {
    ELEMENT_NOT_FOUND,    // Original selector returned no elements
    VISIBLE_CHECK_FAILED, // Element found but not visible/interactable
    STALE_ELEMENT,        // Element became stale during interaction
    RETRY,                // Explicit retry path
    EXPLICIT              // User explicitly requested healing check
}
```

#### `HealOutcome`

Classified result of a healing attempt for reports and CI gates.

```java
public enum HealOutcome {
    HEALED,          // Successfully healed to new selector
    REJECTED,        // Heal attempted but rejected (ambiguous, low confidence)
    UNEXPECTED_HEAL, // Healed when policy was DENY
    NO_HEAL          // No healing needed (original selector worked)
}
```

#### `HealingSemantics`

Complete semantic classification of a healing event including trigger, policy, and outcome.

```java
public record HealingSemantics(
    HealTrigger trigger,   // What triggered healing (failure, retry, etc.)
    HealPolicy policy,     // Policy in effect when healing was attempted
    HealOutcome outcome    // Final classified outcome
) {}
```

**Accessing semantics after healing:**

```java
HealResult result = Frap.getLastHealResult(locator);
if (result.semantics() != null) {
    System.out.println("Outcome: " + result.semantics().outcome());
    // e.g., UNEXPECTED_HEAL when policy was DENY but healing occurred
}
```

#### `SnapshotBuilder`

```java
public class SnapshotBuilder {
    public SnapshotBuilder(Page page);
    public DOMSnapshot build();
}
```

### Extension

#### `FrapExtension`

JUnit 5 extension for automatic setup and reporting.

```java
@ExtendWith(FrapExtension.class)
class MyTest {
    // Automatic: client lifecycle, report generation
}
```

### Context Capture

#### `FrapContext`

```java
public class FrapContext {
    public static String attach(Page page, ContextCaptureOptions options);
    public static void record(ContextEvent event);
}
```

#### `ContextCaptureOptions`

```java
public class ContextCaptureOptions {
    public ContextCaptureOptions captureNetwork(boolean capture);
    public ContextCaptureOptions captureConsole(boolean capture);
    public ContextCaptureOptions captureWebSocket(boolean capture);
    public ContextCaptureOptions traceId(String traceId);
}
```

---

## Configuration

### Environment Variables

| Variable | Module | Description |
|----------|--------|-------------|
| `FRAP_CORE_BIN` | Core | Path to `frap-core-rpc` binary |
| `FRAP_REPORT_DIR` | Playwright | Default report directory |
| `FRAP_MIN_CONFIDENCE` | Playwright | Default confidence threshold |

### Class: `FrapConfig`

```java
public class FrapConfig {
    public static FrapConfig defaults();
    public FrapConfig withMinConfidence(double confidence);
    public FrapConfig withReportDir(String dir);
}
```

---

## RPC to Java Mapping

| RPC Method | Java Method | DTOs |
|------------|-------------|------|
| `heal` | `FrapCoreClient.heal(HealRequest)` | `HealRequest` → `HealResult` |
| `analyze_rca` | `FrapCoreClient.analyzeRca(ContextTimeline, long)` | `ContextTimeline` → `RcaReport` |
| `build_element_map` | `FrapCoreClient.buildElementMap(DOMSnapshot, MapOptions)` | → `ElementMap` |
| `filter_element_map` | `FrapCoreClient.filterElementMap(ElementMap, FilterSpec)` | → `ElementMap` |
| `generate_page_object` | `FrapCoreClient.generatePageObject(ElementMap, GenerateOptions)` | → `GeneratedArtifact` |

---

## Reports

Playwright adapter generates reports in configured directory (default: `target/frap-reports/conference`):

| File | Description |
|------|-------------|
| `frap-report.json` | Summary of all healing events |
| `frap-events.jsonl` | Streaming events (newline-delimited JSON) |
| `junit.xml` | JUnit XML with frap properties |
| `frap-debug.html` | Human-readable debug report |
| `frap-debug-explorer.html` | Explorer view for multiple tests |
| `debug-reports/*.html` | Per-test detailed reports |

---

## Platform Support

| OS | Architecture | Bundled | Notes |
|----|--------------|---------|-------|
| Linux | x86_64 | ✅ | glibc 2.31+ |
| macOS | x86_64 | ✅ | 11.0+ |
| macOS | aarch64 | ✅ | Apple Silicon |
| Windows | x86_64 | ❌ | Use `FRAP_CORE_BIN` with custom build |
| Linux | ARM64 | ❌ | Use `FRAP_CORE_BIN` with custom build |

---

## See Also

- [java-getting-started.md](./java-getting-started.md) — Quick start guide
- [java-maven-central.md](./java-maven-central.md) — Maven Central usage
- [java-sdk-rpc.md](./java-sdk-rpc.md) — RPC protocol details
- [crates/core/README.md](../../crates/core/README.md) — Rust Core reference