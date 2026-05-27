# Java SDK Implementation Status

## ✅ Completed Iterations

### Iter 0: frap-core-rpc Binary
- **Location**: `crates/core/src/bin/rpc.rs`
- **Feature**: NDJSON-RPC over stdin/stdout
- **Methods**: `heal`, `analyze_rca`
- **CI**: smoke tests in `.github/workflows/ci.yml`
- **Docs**: `docs/en/java-sdk-rpc.md`

### Iter 1: Maven Core SDK
- **Module**: `sdk/java/frap-core-java/`
- **DTOs**: All TypeScript types ported (Signature, DOMSnapshot, HealRequest, HealResult, etc.)
- **Client**: `FrapRpcClient` - subprocess RPC implementation
- **Config**: `FrapConfig`, semantics, context, RCA types

### Iter 2: Playwright Java Adapter
- **Module**: `adapters/playwright-java/`
- **API**: `Frap.withFrap(locator, page, options)`
- **Features**: 
  - Automatic healing on click/fill/check/...
  - Context capture (network, console, WebSocket)
  - JUnit5 Extension (`@ExtendWith(FrapExtension.class)`)
- **Snapshot**: `SnapshotBuilder` - DOM capture via Playwright JS evaluation

### Iter 3: Reports + Demo
- **Example**: `examples/java-playwright-demo/`
- **Tests**: Conference healing tests, RCA tests, reporting verification (`@Tag("e2e")`)
- **Artifacts**: frap-report.json, junit.xml, frap-context-events.jsonl, frap-events.jsonl
- **E2E gate**: `./scripts/run-java-e2e.sh` (13 tests, `BUILD SUCCESS`)
- **CI**: job `java-playwright-e2e` in `.github/workflows/ci.yml`

### Iter 3.1: Selector resolution & test split (2026-05-27)
- **`extractSelector`**: parses `locator('...')` and `Locator@` + selector (Playwright Java `toString()`)
- **`SnapshotBuilder`**: pre-record via `exists(Locator)` / `extractForLocator` (Playwright API, not `querySelector` on invalid strings)
- **`WithFrapOptions.selector()`**, `Frap.withFrap(Page, String, …)` for explicit selector override
- **Test suites**: unit in `adapters/playwright-java` (`excludedGroups=e2e`); E2E in demo module

### Iter 4: JNI/Native (Production)
- **Rust FFI**: `crates/core/src/ffi.rs` - C API
- **Module**: `sdk/java/frap-core-native/`
- **Client**: `FrapNativeClient` - JNI bindings
- **Interface**: `FrapCoreClient` - common interface for RPC and Native
- **Transport selection**: Auto-fallback or explicit via `frap.transport` property

## ⏸️ On Hold

### Iter 5: Selenium + JUnit5 (pending Maven setup)
**Goal**: WebDriver integration for bank/enterprise legacy tests

**Components needed**:
```
adapters/junit5/
  ├── FrapWebDriver.java       - Decorator/wrapper for WebDriver
  ├── FrapElementLocator.java  - FindElement hook
  └── FrapTestExecutionListener.java - JUnit5 listener

examples/java-selenium-demo/
  ├── SeleniumConferenceTest.java
  └── WebDriverFactory.java
```

**Key differences from Playwright**:
- WebDriver has different DOM access (no `page.evaluate`)
- Need `JavascriptExecutor` for snapshot capture
- Slower element detection (no quick `document.querySelector` check)
- JUnit5 Extension vs TestExecutionListener

### Iter 6: Selenide (pending after Selenium)
**Goal**: Fluent API integration for Selenide users

**Components needed**:
```
adapters/selenide/
  ├── FrapSelenide.java        - Extension or custom ElementFinder
  └── FrapCondition.java       - Selenide conditions with healing
```

## 🔧 Maven Setup Required

Current structure:
```
sdk/java/
  ├── pom.xml (parent)
  ├── frap-core-java/
  ├── frap-core-native/
  └── ... (future modules)
```

To build:
```bash
cd sdk/java
mvn install -DskipTests
```

For examples with all modules:
```bash
cd sdk/java
mvn install -DincludeExamples
```

Full Playwright Java E2E (from repo root):
```bash
./scripts/run-java-e2e.sh
```

## 📋 Next Steps (when ready)

1. **Test current build**: Verify `mvn compile` works on the created modules
2. **Resolve any Maven issues**: Dependencies, module structure
3. **Iter 5**: Implement `adapters/junit5/` with WebDriver hook
4. **Iter 6**: Implement `adapters/selenide/` for Selenide integration

## 📊 Summary Statistics

| Component | Files | Lines (approx) |
|-----------|-------|----------------|
| frap-core-java | 25+ Java | ~3000 |
| frap-core-native | 5+ Java | ~500 |
| playwright-java | 15+ Java | ~2000 |
| java-playwright-demo | 5+ Java | ~600 |
| Rust FFI | 1 Rust + config | ~300 |
| **Total** | **50+** | **~6400** |

## 🔗 Key References

- **TypeScript SDK**: `sdk/typescript/` (reference implementation)
- **Rust Core**: `crates/core/` (algorithm source)
- **Playwright Adapter TS**: `adapters/playwright/` (behavior reference)
- **F000 P1 FFI Spec**: `project/feature/F000-core-platform-api.md`
- **F014 Java SDK Spec**: `project/feature/F014-java-sdk-ui-adapters.md`
