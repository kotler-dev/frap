# Java SDK Implementation Status

## Completed (1.0.0 epic)

### Core + RPC
- `frap-core-rpc`: `heal`, `analyze_rca`, `build_element_map`, `filter_element_map`, `generate_page_object`
- Clustering wired in healing + contract `clustering-id-migration`
- Element map contract `element-map-list`
- Page Object codegen (`crates/core/src/page_object.rs`)

### frap-core-java
- DTOs: `ElementMap`, `FilterSpec`, `GenerateOptions`, …
- `FrapCoreClient`: buildElementMap, filterElementMap, generatePageObject
- `SnapshotStore` for JSON persistence
- Contract tests (Java reference SDK)
- Bundled native binaries (6 platforms: Linux glibc/musl/aarch64, macOS x86_64/aarch64, Windows x64)

### frap-playwright
- `Frap.discover(page)`, `Frap.generatePageObject(page, dir, options)`
- `withFrap`, `FrapExtension`, reports
- Discover/PoG quality (`develop/java-v1.0.1`, issues #7–#15): `:has-text`, LIST accessor, dedup, semantic names, `innerText` snapshot — [docs/en/discover-page-object.md](../../docs/en/discover-page-object.md)

### Maven
- Version `1.0.0`, coordinates `io.github.kotler-dev`
- `smoke-consumer` module for local Maven-only verification
- Release workflow: `.github/workflows/publish-maven.yml`

## F019 / 1.0.1 (discover ground truth)

- Core: `validate_dom_benchmark`, `CoverageMode`, `fragile`, `alternatives`, role+name ranking
- Fixtures: `fixtures/dom-benchmark/` (9 pages, incl. `09-sibling-label` / C013 pattern)
- Java: `MapOptions.semanticCatalog()`, `discoverToFile`, `DomBenchmarkE2eTest`
- Live manual: C013 `run-sber-explore.sh` → `project/artifacts/sber-person-giga/`
- Docs: `frap/docs/quickstart-discover.md`, `java-sdk-1.0.1-matrix.md`

Verify:

```bash
cd frap/crates && cargo test -p frap-core contract_dom_benchmark
cd frap/sdk/java && mvn -P java-unit verify
```

## frap-mcp (1.0.1, Maven Central)

- Reactor: `sdk/java/frap-mcp/` (Java 21, separate parent POM)
- Artifacts: `frap-mcp-tools`, `frap-mcp-stdio`, `frap-mcp-http`, `frap-mcp-http-local`
- 6 MCP tools; verify: `./scripts/run-frap-mcp-verify.sh` (JDK 21)
- Cross-client E2E: `@Tag("mcp-e2e")` in `SnapshotScriptCrossClientE2eTest`

## On hold

- `frap-core-native` on Maven Central (JNI experimental)
- Selenium / WebDriver (F014 track B)
- TypeScript SDK release (frozen; contracts remain in repo)

## Verify

```bash
cd crates && cargo test -p frap-core
cd sdk/java && mvn -P java-unit verify
./scripts/run-java-e2e.sh   # from repo root
```
