# Java SDK — development branch

## Active integration branch

| | |
|---|---|
| **Branch** | `develop/java-v1.1.1` |
| **Maven (workspace)** | `1.1.1-SNAPSHOT` |
| **Last release** | **1.1.0** — tag `java-v1.1.0`, Maven Central |

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

Use **1.1.0** from Maven Central — not SNAPSHOT from this branch:

```xml
<dependency>
  <groupId>io.github.kotler-dev</groupId>
  <artifactId>frap-core-java</artifactId>
  <version>1.1.0</version>
</dependency>
```

## Release line (maintainers)

Next Maven release target: **1.1.1** → branch `release/java-v1.1.1`, tag `java-v1.1.1`.  
See [`MAVEN_RELEASE_CHECKLIST.md`](./MAVEN_RELEASE_CHECKLIST.md).

Frozen backup: `develop/java-v1.0.1` (pre–1.1.0 line).
