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
- Bundled native binaries (Linux x86_64 glibc/musl, macOS aarch64)

### frap-playwright
- `Frap.discover(page)`, `Frap.generatePageObject(page, dir, options)`
- `withFrap`, `FrapExtension`, reports

### Maven
- Version `1.0.0`, coordinates `io.github.kotler-dev`
- `smoke-consumer` module for local Maven-only verification
- Release workflow: `.github/workflows/publish-maven.yml`

## On hold

- `frap-core-native` on Maven Central (JNI experimental)
- Selenium / WebDriver (F014 track B)
- Windows bundled RPC binary
- TypeScript SDK release (frozen; contracts remain in repo)

## Verify

```bash
cd crates && cargo test -p frap-core
cd sdk/java && mvn -P java-unit verify
./scripts/run-java-e2e.sh   # from repo root
```
