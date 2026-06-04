# Frap Java SDK

Self-healing selectors, page discovery, Page Object generation and an MCP server —
all on top of the native `frap-core-rpc` engine. This directory contains **two
independent Maven projects**:

| Project | Parent POM | Java | What it is |
|---------|-----------|------|------------|
| **Core SDK** | `io.github.kotler-dev:frap-sdk-parent` | 17 | The Java client for the frap engine + adapters |
| **MCP server** | `io.github.kotler-dev:frap-mcp-parent` | 21 | Spring AI MCP **sidecar** (stdio + http + http-local) |

**Released:** Maven Central **1.1.0** · **Development:** workspace **1.1.1-SNAPSHOT** on branch **`develop/java-v1.1.1`** — see [`DEVELOPMENT.md`](./DEVELOPMENT.md).

> They are separate reactors (different parent POMs, different Java levels). The
> MCP project depends on `frap-core-java` as a normal Maven artifact. **MCP is optional:**
> PoG, discover, and healing work on **JDK 17** without MCP. Run MCP as a **JDK 21 sidecar**
> (stdio or HTTP) for LLM agent integration — not in-process in Playwright tests.

---

## Table of contents

1. [Architecture at a glance](#architecture-at-a-glance)
2. [The core engine (`frap-core-java`)](#the-core-engine-frap-core-java)
3. [Native client (`frap-core-native`)](#native-client-frap-core-native)
4. [The MCP server (`frap-mcp`)](#the-mcp-server-frap-mcp)
5. [The 6 MCP tools](#the-6-mcp-tools)
6. [I/O modes: inline vs file](#io-modes-inline-vs-file)
7. [The pipeline (worked examples)](#the-pipeline-worked-examples)
8. [Configuration reference](#configuration-reference)
9. [Build & test](#build--test)
10. [Repository layout](#repository-layout)
11. [Platform support](#platform-support)
12. [Module docs & links](#module-docs--links)

---

## Architecture at a glance

```
                         YOUR SIDE (client / agent)                  │  FRAP SIDE
                                                                     │
  ┌────────────┐   runs snapshot JS   ┌────────────────┐             │
  │  Browser   │ ◀────────────────────│  Agent / LLM   │             │
  │ (Playwright│   { html, elements } │  or your code  │             │
  │  /CDP/...)  │ ───────────────────▶│                │             │
  └────────────┘                      └───────┬────────┘             │
                                              │ MCP (stdio / http)   │
                                              ▼                      │
                                   ┌───────────────────────┐         │
                                   │   frap-mcp server     │         │
                                   │  6 @McpTool methods   │         │
                                   │  FrapTools (inline)   │         │
                                   │  FrapFileTools (file) │         │
                                   │  FrapSnapshotTool     │         │
                                   │  FrapHelpTool         │         │
                                   └──────────┬────────────┘         │
                                              ▼                      │
                                   ┌───────────────────────┐         │
                                   │   FrapToolService     │  single source of logic
                                   └──────────┬────────────┘         │
                                              ▼                      │
                                   ┌───────────────────────┐         │
                                   │  FrapCoreClient       │  Java API
                                   │  └ FrapRpcClient      │  spawns subprocess
                                   └──────────┬────────────┘         │
                                              ▼ JSON-RPC over stdio  │
                                   ┌───────────────────────┐         │
                                   │  frap-core-rpc (Rust) │  native binary, bundled
                                   └───────────────────────┘         │
```

Key idea: **frap has no browser.** The browser-side DOM snapshot is captured by
*you* (Playwright / CDP / any MCP browser tool) by running a script frap hands you;
frap only analyses the captured `{ html, elements }` snapshot and emits maps,
locators, Page Objects and healing decisions via the native engine.

---

## The core engine (`frap-core-java`)

The Java client for the frap engine. It talks to the native `frap-core-rpc`
binary over a subprocess JSON-RPC channel — **no Rust/Cargo needed** on supported
platforms (the binary is bundled in the JAR and auto-extracted at runtime).

### What it does

1. **Self-healing selectors** — when a selector no longer matches, frap finds the
   element by structure and returns the new selector + confidence + ranked
   candidates (instead of guessing wrong).
2. **Page discovery & clustering** — turns a DOM snapshot into a structured
   `ElementMap` where repeating UI (rows, cards, nav links, tiles) collapse into
   `LIST` / `SINGLE` clusters, each element scored 0..1.
3. **Page Object generation** — emits compilable Page Object source (e.g. Java
   Playwright) from an `ElementMap`.
4. **RCA & context** — root-cause analysis over a runtime timeline (UI + network +
   logs). Explainable, deterministic output.

### Client API — `FrapCoreClient`

```java
public interface FrapCoreClient extends AutoCloseable {
    HealResult       heal(HealRequest request);
    RcaReport        analyzeRca(ContextTimeline timeline, long failureAtMs);
    ElementMap       buildElementMap(DOMSnapshot snapshot, MapOptions options);
    ElementMap       filterElementMap(ElementMap map, FilterSpec spec);
    GeneratedArtifact generatePageObject(ElementMap map, GenerateOptions options);
    boolean          isAlive();
}
```

- **`FrapRpcClient`** — the production implementation; `FrapRpcClient.create()`
  extracts and spawns the bundled native binary and keeps it alive for reuse.
- DTOs live in `io.github.kotlerdev.frap.core.dto` (records, Jackson `snake_case`):
  `DOMSnapshot`, `ElementMap`, `ElementNode`, `Cluster`/`ClusterType`,
  `LocatorRecommendation`, `Signature`, `MapOptions`, `FilterSpec`,
  `GenerateOptions`, `GeneratedArtifact`/`GeneratedFile`, `HealRequest`/`HealResult`,
  `Candidate`, `MapMetadata`, …
- Other packages: `context` (timeline), `events`, `logs`, `rca`, `semantics`,
  `snapshot`, `config`.

### Maven coordinate

```xml
<dependency>
    <groupId>io.github.kotler-dev</groupId>
    <artifactId>frap-core-java</artifactId>
    <version>1.1.0</version>
</dependency>
```

See [`frap-core-java/README.md`](./frap-core-java/README.md).

---

## Native client (`frap-core-native`)

Experimental **JNI** client — an alternative to the subprocess RPC transport that
loads the engine in-process. Not published to Maven Central; build locally. Same
`FrapCoreClient` contract. See [`frap-core-native/README.md`](./frap-core-native/README.md).

---

## The MCP server (`frap-mcp`)

A Spring Boot / Spring AI ([`spring-ai` 1.1.7](https://docs.spring.io/spring-ai/reference/))
server that exposes the frap engine as **MCP tools** over two transports. It is a
multi-module Maven project (`frap-mcp-parent`, Java 21, Spring Boot 3.5.13):

| Module | Role |
|--------|------|
| `frap-mcp-tools` | Shared `@McpTool` beans + `FrapToolService` + `ArtifactStore`; depends on `frap-core-java` |
| `frap-mcp-stdio` | Executable runner — **stdio** transport, **file** mode |
| `frap-mcp-http`  | Executable runner — **streamable-http** transport, **inline** mode |
| `frap-mcp-http-local` | Executable runner — **streamable-http** (port 8765), **file** mode + `POST /frap/ingest` |

Both runners `@ComponentScan` the tools package; the active tool set is selected by
the `frap.io.mode` property via `@ConditionalOnProperty` (see below). Each runner
therefore exposes **6 tools**: 4 mode-specific + 2 always-on (`frap_snapshot_script`,
`frap_help`).

```
frap-mcp-tools
├── FrapToolService        single source of frap-core-client call logic (IOException→IllegalState)
├── FrapSnapshotTool       always-on  → frap_snapshot_script   (STEP 1)
├── FrapHelpTool           always-on  → frap_help              (mode-aware guide)
├── FrapTools              @COP frap.io.mode=inline (default)  → 4 inline tools (objects)
├── FrapFileTools          @COP frap.io.mode=file              → 4 file tools (paths + digest)
└── io/ArtifactStore       work-dir resolve, readJson/writeJson/writeText, summarize()
    io/ElementMapSummary, ElementMapFileResult, GeneratedArtifactFileResult
```

See [`frap-mcp/README.md`](./frap-mcp/README.md).

---

## The 6 MCP tools

| Tool | Step | Purpose | Inline (http) | File (stdio) |
|------|------|---------|---------------|--------------|
| **`frap_help`** | — | Beginner guide: what frap is, the order of calls, what to pass; mode-aware | returns text | returns text |
| **`frap_snapshot_script`** | 1 (mandatory, runs **client-side**) | Returns the JS you run in *your* browser to capture the DOM snapshot | returns JS string | returns JS string |
| **`frap_build_element_map`** | 2 | Snapshot → clustered `ElementMap` + locators + confidence | `domSnapshot` obj → `ElementMap` obj | `domSnapshotPath` → `{ element_map_path, summary }` |
| **`frap_filter_element_map`** | 2b (optional) | Shrink the map (interactive-only / cluster size / tags) | `elementMap` obj → `ElementMap` obj | `elementMapPath` → `{ element_map_path, summary }` |
| **`frap_generate_page_object`** | 3 | `ElementMap` → Page Object source | `elementMap` obj → `{ files:[{path,content}] }` | `elementMapPath` → `{ file_paths, file_count, work_dir }` (files written to disk) |
| **`frap_heal`** | standalone | Repair one broken selector against a fresh snapshot | `request` obj → `HealResult` | `domSnapshotPath` + selector → `HealResult` (inline) |

> Tool **names are identical** across modes — only one mode-specific set is active
> per runner, so the agent never sees “two builds”. The descriptions are written to
> be understood by very small/free models: each one states `INPUT`, `OUTPUT`,
> `NEXT`, where each input comes from, and a concrete `EXAMPLE`.

`frap_help` is the fastest way for any model to learn the flow — call it first.

---

## I/O modes: inline vs file

The same logic, two ways of moving large artifacts (DOM snapshot, ElementMap,
generated source):

| `frap.io.mode` | Runner | Beans active | Artifacts pass as | Why |
|----------------|--------|--------------|-------------------|-----|
| `inline` *(default)* | http | `FrapTools` (+ snapshot, help) | **inline JSON** in the request/response | HTTP clients may be remote — no shared filesystem |
| `file` | stdio | `FrapFileTools` (+ snapshot, help) | **absolute file paths + a compact digest** | stdio = local: server and agent share one FS, so don't waste context streaming tens of KB |

**File-mode** is pass-by-reference: a tool reads its big input from a file, writes
its big output to a file under `frap.io.work-dir`, and returns the **path plus a
summary digest** so the agent gets a signal without opening the file. The digest
(`summary`) includes: `element_count`, `cluster_count`, `single_clusters`,
`list_clusters`, `largest_list_size`, `conf_avg`, `locators_ge_080`,
`strategy_counts`.

Gating is on an explicit per-runner property:
`@ConditionalOnProperty(name="frap.io.mode", havingValue="inline", matchIfMissing=true)`
for `FrapTools`, `havingValue="file"` for `FrapFileTools`. `frap-mcp-stdio` sets
`frap.io.mode=file`; `frap-mcp-http` sets `frap.io.mode=inline`.

---

## The pipeline (worked examples)

```
frap_help  →  frap_snapshot_script  →  (run JS in your browser)  →
   frap_build_element_map  →  (optional frap_filter_element_map)  →  frap_generate_page_object
frap_heal is separate — used later to repair a broken selector.
```

**STEP 1 is mandatory and runs on your side.** `frap_snapshot_script` returns a
JavaScript string; run it with **any** browser tool you have — `playwright-cli`,
the Playwright MCP server, the chrome-devtools MCP server, or a direct CDP
debugging-port connection — to obtain `{ html, elements: [...] }`.

### File mode (stdio)

```jsonc
// 1. frap_snapshot_script()  -> JS string; run it in the browser -> { html, elements }
//    save that object to /tmp/frap/snapshot-main.json
// 2. frap_build_element_map
{ "domSnapshotPath": "/tmp/frap/snapshot-main.json",
  "options": { "url": "https://example.com/app/main" } }
//    -> { "element_map_path": "/tmp/frap/element-map-3f2a.json",
//         "summary": { "element_count": 57, "cluster_count": 7, "conf_avg": 0.59, ... } }
// 3. frap_generate_page_object
{ "elementMapPath": "/tmp/frap/element-map-3f2a.json",
  "language": "java_playwright", "className": "MainPage", "packageName": "com.example.pages" }
//    -> { "file_paths": ["/tmp/frap/com/example/pages/MainPage.java"], "file_count": 1, "work_dir": "/tmp/frap" }
```

### Inline mode (http)

```jsonc
// 2. frap_build_element_map
{ "domSnapshot": { "html": "<html>...</html>", "elements": [ /* ...from the snapshot JS... */ ] },
  "options": { "url": "https://example.com/app/main" } }
//    -> the full ElementMap object { elements, clusters, metadata }
// 3. frap_generate_page_object
{ "elementMap": { /* the exact object from step 2 */ },
  "language": "java_playwright", "className": "MainPage", "packageName": "com.example.pages" }
//    -> { "files": [ { "path": "com/example/pages/MainPage.java", "content": "package ..." } ] }
//       (you write each content to its path yourself)
```

---

## Configuration reference

| Property / env | Applies to | Default | Meaning |
|----------------|-----------|---------|---------|
| `frap.io.mode` | frap-mcp runners | `inline` (`matchIfMissing`) | `inline` → object tools; `file` → path+digest tools. Set per runner. |
| `frap.runtime.dir` / `FRAP_RUNTIME_DIR` | frap-core-java, frap-mcp | `<jar-dir>/.frap` | Runtime base: extracted native binary (`bin/`), default work dir (`work/`), logs (`logs/`). Avoids `/tmp` when policy blocks exec there. JVM: `-Dfrap.runtime.dir=/path`; Spring: `--frap.runtime.dir=/path`. |
| `frap.io.work-dir` / `FRAP_IO_WORK_DIR` | frap-mcp (file mode) | `${frap.runtime.dir}/work` | Base dir for written artifacts. Not auto-cleaned. Returned paths are absolute. |
| `FRAP_CORE_BIN` (env) | frap-core-java | bundled binary | Override the path to the native `frap-core-rpc` binary (skip JAR extract). |
| `logging.file.name` | frap-mcp runners | `${frap.runtime.dir}/logs/frap-mcp.log` | Log file path (stdio keeps console OFF). |
| `spring.ai.mcp.server.*` | frap-mcp runners | see `application.properties` | MCP server name/version/transport; stdio reserves stdout for JSON-RPC. |

---

## Build & test

### Core SDK (`frap-sdk-parent`, Java 17)

```bash
cd sdk/java
mvn -P java-unit verify          # Java unit tests
cd smoke-consumer && mvn compile exec:java   # minimal end-to-end usage
```

### MCP server (`frap-mcp-parent`, Java 21)

```bash
# (optional) build the native binary if not bundled for your platform:
cargo build -p frap-core --bin frap-core-rpc --release

# Unit layer (shared tools): @TempDir + Mockito, no native binary
mvn -f sdk/java/frap-mcp/pom.xml -pl frap-mcp-tools test

# Integration layer (stdio, file mode, REAL native binary). -am rebuilds frap-mcp-tools.
mvn -f sdk/java/frap-mcp/pom.xml -pl frap-mcp-stdio -am verify

# Inline regression (http)
mvn -f sdk/java/frap-mcp/pom.xml -pl frap-mcp-http -am verify

# Full reactor (both runners)
mvn -f sdk/java/frap-mcp/pom.xml verify
```

Run a server:

```bash
# stdio (file mode) — for local MCP clients
java -jar sdk/java/frap-mcp/frap-mcp-stdio/target/frap-mcp-stdio.jar

# streamable-http (inline mode) — serves MCP at http://localhost:8080/mcp
java -jar sdk/java/frap-mcp/frap-mcp-http/target/frap-mcp-http.jar

# streamable-http (file mode + ingest) — http://localhost:8765/mcp
java -jar sdk/java/frap-mcp/frap-mcp-http-local/target/frap-mcp-http-local.jar
```

---

## Repository layout

```
sdk/java/
├── pom.xml                       # frap-sdk-parent (Java 17)
├── README.md                     # ← this file
├── frap-core-java/               # Core RPC client + DTOs + bundled native binary
│   └── src/main/resources/META-INF/native/   # per-platform frap-core-rpc binaries
├── frap-core-native/             # Experimental JNI client (not on Central)
├── smoke-consumer/               # Minimal usage example
└── frap-mcp/                     # frap-mcp-parent (Java 21, Spring Boot 3.5.13)
    ├── frap-mcp-tools/           # @McpTool beans, FrapToolService, ArtifactStore
    ├── frap-mcp-stdio/           # stdio runner  (frap.io.mode=file)
    ├── frap-mcp-http/            # streamable-http runner (frap.io.mode=inline)
    └── frap-mcp-http-local/      # streamable-http + file mode + /frap/ingest

adapters/playwright-java/         # Playwright adapter (separate module)
examples/java/playwright/         # Runnable demo
crates/core/                      # Rust source of the frap-core-rpc binary
```

---

## Platform support (native binary)

| OS | Arch | Status |
|----|------|--------|
| macOS | aarch64 (Apple Silicon) | ✅ Bundled |
| macOS | x86_64 | ✅ Bundled |
| Linux | x86_64 (glibc / musl) | ✅ Bundled |
| Linux | aarch64 | ✅ Bundled |
| Windows | x86_64 | ✅ Bundled (or `FRAP_CORE_BIN` override) |

The native binary is extracted from the JAR at runtime; no Rust toolchain required
on supported platforms.

---

## Module docs & links

- **[Discover & Page Object](../../docs/en/discover-page-object.md)** — page map, clusters, generated locators, confidence (Java)
- **[Quickstart: discover](../../docs/quickstart-discover.md)** — dom-benchmark, semantic coverage (F019)
- [`frap-core-java/README.md`](./frap-core-java/README.md) — core RPC client API
- [`frap-core-native/README.md`](./frap-core-native/README.md) — experimental JNI client
- [`frap-mcp/README.md`](./frap-mcp/README.md) — MCP server, tools, modes (deep dive)
- [Playwright adapter](../../adapters/playwright-java/README.md) — Playwright integration
- [Demo](../../examples/java/playwright/) — runnable example
- [`DEVELOPMENT.md`](./DEVELOPMENT.md) — **`develop/java-v1.1.1`**, SNAPSHOT workflow for contributors
- [`VERIFICATION.md`](./VERIFICATION.md) — test levels & acceptance matrix
- [`MAVEN_RELEASE_CHECKLIST.md`](./MAVEN_RELEASE_CHECKLIST.md) — Maven Central publication
- [Rust core](../../crates/core/README.md) — native RPC binary

---

## Versions

- **Released (Maven Central):** `frap-core-java`, `frap-playwright`, `frap-mcp-*` @ **1.1.0**
- **Development (git):** **1.1.1-SNAPSHOT** on branch **`develop/java-v1.1.1`**

**Core SDK (1.1.x line)**

- ✅ Playwright Java adapter
- ✅ Self-healing selectors
- ✅ Page discovery and clustering (F019: dom-benchmark, semantic coverage)
- ✅ Page Object generation — semantic Playwright APIs (`getByTestId`, `getByRole`, …) — [discover guide](../../docs/en/discover-page-object.md)
- ✅ Six-platform bundled `frap-core-rpc` (Linux glibc/musl/aarch64, macOS, Windows x64)
- ⚠️ WebDriver/Selenium — roadmap v1.4
