# Audit Report — Java SDK 1.0.0 (Maven Central)

## Summary

| Field | Value |
|-------|-------|
| **Date** | 2026-05-30 |
| **Baseline commit** | `1efdc9becf7f81342b00eeb77e0e2f7844b2777e` (WIP working tree with audit fixes applied) |
| **Verdict** | **Go with fixes applied** — ready to republish after dropping failed Central Portal deployment |

Primary blocker identified and fixed: Maven `groupId` was `io.github.kotlerdev` while Central Portal namespace is **`io.github.kotler-dev`**. This caused 48 deployment validation failures (`Failed to get coordinates from pom file`).

---

## Findings

| ID | P | Area | Finding | Fix | Status |
|----|---|------|---------|-----|--------|
| F01 | P0 | Maven | `groupId` mismatch vs verified namespace `io.github.kotler-dev` | Updated all POMs + docs | **fixed** |
| F02 | P0 | Docs | `project/release/README.md` marked Java as `published` before Central verify | Status → `ready (awaiting publish)` | **fixed** |
| F03 | P0 | Native | Wrong binary names in repo (`frap-core-rpc-darwin-*`, `macos-arm64`) | Renamed to `frap-core-rpc-macos-aarch64`; removed duplicates | **fixed** |
| F04 | P0 | CI | Full CI only on `v*` tags | Added `java-v*` trigger to `ci.yml`, `lint.yml`, `publish-maven.yml` | **fixed** |
| F15 | P0 | Maven | Staged POMs referenced unpublished parent `frap-sdk-parent` | `flatten-maven-plugin` (oss) + explicit groupId; `skipPublishing` on core | **fixed** |
| F05 | P1 | CI | `publish-maven.yml` shared `v*` with npm | Maven workflow now `java-v*` only | **fixed** |
| F06 | P1 | Docs | `Frap.md` had `io.frap` coords and `-SNAPSHOT` | Updated to `io.github.kotler-dev:1.0.0` | **fixed** |
| F07 | P1 | Docs | Platform table claimed macOS x86_64 bundled | Updated `java-maven-central.md`, `java-api-reference.md` | **fixed** |
| F08 | P1 | CHANGELOG | Java 1.0.0 only in Unreleased | Added `[Java SDK 1.0.0]` section | **fixed** |
| F09 | P1 | Stats | FEATURES Java SDK 91% vs matrix 100% | Aligned to 12/12 shipped | **fixed** |
| F10 | P1 | Workflow | `chmod` step had literal `\n` in publish-maven.yml | Fixed | **fixed** |
| F11 | P2 | Repo | Stray `crates/sdk/java/...` duplicate native path | Removed | **fixed** |
| F12 | P2 | E2E | `debug-mode.spec.ts` fails to load (No tests found) | Pre-existing; not release blocker for Java | **open** |
| F13 | P2 | Lint | TS lint `continue-on-error: true` in lint.yml | Informational | **open** |
| F14 | P2 | Central | Artifacts not yet on Maven Central (404) | Republish with `java-v1.0.0` after Portal drop | **pending user action** |

---

## Verification log

### L1 — Rust Core

```bash
cd crates && cargo test -p frap-core
cd crates && cargo fmt -- --check
cd crates && cargo clippy -p frap-core -- -D warnings
```

**Result:** PASS

### L2 — Java unit

```bash
export FRAP_CORE_BIN=crates/target/release/frap-core-rpc
cd sdk/java && mvn -P java-unit verify
```

**Result:** PASS (frap-core-java + frap-core-native + frap-playwright unit tests)

### L3 — Java E2E

```bash
./scripts/run-java-e2e.sh
```

**Result:** PASS (16 tests, BUILD SUCCESS)

### L4 — Release package

```bash
cd sdk/java && mvn -P release -pl frap-core-java,../../adapters/playwright-java -am package -DskipTests
```

**Result:** PASS  
Embedded POM path: `META-INF/maven/io.github.kotler-dev/frap-core-java/pom.xml` ✅

### L5 — Smoke consumer

```bash
cd sdk/java && mvn install -pl frap-core-java -DskipTests
cd smoke-consumer && mvn compile exec:java
```

**Result:** PASS (`frap smoke OK: 1 elements`)

### E2E gates (TypeScript)

| Gate | Command | Result |
|------|---------|--------|
| Context C002–C004 | `npx playwright test --config=playwright.context.config.ts` + verify scripts | PASS |
| Conference CP001–CP005 | `npx playwright test --config=playwright.conference.config.ts` + `verify-reports.mjs` | PASS |
| Debug mode F012 | `npx playwright test debug-mode.spec.ts` | FAIL (import/load issue) |

### Security

| Check | Result |
|-------|--------|
| `frap-signing-key*.txt` in `.gitignore` | PASS |
| Git history scan for private keys | No leaked keys in tracked history |
| GPG key `D8B424D9603C9A7F` on keyserver | HTTP 200 |
| `io.frap` in Maven POMs | None |

### Central consumer (pre-publish)

```bash
curl repo1.maven.org/.../io/github/kotler-dev/frap-core-java/1.0.0/...
mvn dependency:get -Dartifact=io.github.kotler-dev:frap-core-java:1.0.0
```

**Result:** 404 (expected until successful republish)

---

## Release checklist (from MAVEN_RELEASE_CHECKLIST)

- [x] Namespace `io.github.kotler-dev` verified in Central Portal
- [x] `groupId` = `io.github.kotler-dev` in all deployable POMs
- [x] Version `1.0.0` in `sdk/java/pom.xml`
- [x] No `io.frap` in Maven coordinates (release-facing)
- [x] `mvn -P release -pl frap-core-java,../../adapters/playwright-java -am package` passes
- [x] L1–L3 + smoke consumer pass locally
- [x] CI secrets documented (`CENTRAL_*`, `GPG_*`)
- [x] GPG key on keyserver
- [x] `publish-maven.yml` triggers on `java-v*`
- [ ] Drop failed deployment in Central Portal UI
- [ ] Push tag `java-v1.0.0` and monitor Actions
- [ ] Post-publish curl verify on `repo1.maven.org`
- [ ] Update `project/release/README.md` status to `published`

---

## Republish procedure

1. In [Central Portal](https://central.sonatype.com/), **drop** the failed `1.0.0` deployment (48 validation errors).
2. Commit audit fixes (this changeset).
3. Optional: `workflow_dispatch` → **frap CI** for full gate before tag.
4. Tag and push:
   ```bash
   git tag -a java-v1.0.0 -m "Java SDK 1.0.0 — Maven Central"
   git push origin java-v1.0.0
   ```
5. Monitor **Publish to Maven Central** workflow.
6. After 10–30 min:
   ```bash
   curl -sf "https://repo1.maven.org/maven2/io/github/kotler-dev/frap-core-java/1.0.0/frap-core-java-1.0.0.pom"
   curl -sf "https://repo1.maven.org/maven2/io/github/kotler-dev/frap-playwright/1.0.0/frap-playwright-1.0.0.pom"
   ```
7. Bump to `1.0.1-SNAPSHOT` for next dev iteration.

---

## Target coordinates

```xml
<dependency>
    <groupId>io.github.kotler-dev</groupId>
    <artifactId>frap-core-java</artifactId>
    <version>1.0.0</version>
</dependency>
```

Java packages remain `io.github.kotlerdev.frap.*` (no hyphen — valid Java identifiers).

---

*Generated as part of pre-release audit for Maven Central 1.0.0.*
