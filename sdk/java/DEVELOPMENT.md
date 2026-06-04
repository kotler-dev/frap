# Java SDK — development branch

## Active integration branch

| | |
|---|---|
| **Branch** | `develop/java-v1.1.1` |
| **Maven (workspace)** | `1.2.0-SNAPSHOT` |
| **Last release** | **1.1.1** — tag `java-v1.1.1`, Maven Central |

Fork and open PRs **into `develop/java-v1.1.1`**, not directly into `main`.

```bash
git fetch origin
git checkout develop/java-v1.1.1
git pull origin develop/java-v1.1.1
git checkout -b feat/my-change
```

## Build & verify

```bash
# Rust RPC (required for Java integration tests)
cd crates && cargo build --release -p frap-core --bin frap-core-rpc
export FRAP_CORE_BIN="$PWD/target/release/frap-core-rpc"

cd ../sdk/java
mvn -P java-unit verify

cd ../..
./scripts/run-java-e2e.sh
./scripts/run-frap-mcp-verify.sh   # JDK 21
```

## Consumers (released artifacts)

Use **1.1.1** from Maven Central — not SNAPSHOT from this branch:

```xml
<dependency>
  <groupId>io.github.kotler-dev</groupId>
  <artifactId>frap-core-java</artifactId>
  <version>1.1.1</version>
</dependency>
```

## Hosts that block `/tmp` (noexec / security policy)

**1.1.1+** extracts the native binary under `<jar-dir>/.frap/bin/` (not `/tmp`). Override:

```bash
java -Dfrap.runtime.dir="$PWD/.frap" -jar frap-mcp-http-local.jar
```

MCP startup logs resolved paths (`frap runtime: dir=...`) — check `logs/frap-mcp.log` or console (http-local).

**Upgrade from 1.1.0:** bump coordinates to `1.1.1`; remove manual `FRAP_CORE_BIN` extract workaround if you added it for `/tmp` policy.

## Release line (maintainers)

Next Maven release target: **1.2.0** on `develop/java-v1.1.1` after **1.1.1** is on Central.  
See [`MAVEN_RELEASE_CHECKLIST.md`](./MAVEN_RELEASE_CHECKLIST.md).

Frozen backup: `develop/java-v1.0.1` (pre–1.1.0 line).
