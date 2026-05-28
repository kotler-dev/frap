# Java SDK 1.0.0 — verification matrix

## Level 1 — Rust / contracts

```bash
cd crates && cargo test -p frap-core
```

## Level 2 — Java SDK

```bash
cd sdk/java && mvn -P java-unit verify
```

## Level 3 — Playwright E2E

```bash
./scripts/run-java-e2e.sh
```

## Level 4 — Pre-release

```bash
cd sdk/java
mvn -P release -pl frap-core-java,../../adapters/playwright-java -am package -DskipTests
```

## Level 5 — Smoke consumer (Maven only)

```bash
cd sdk/java && mvn install -pl frap-core-java -DskipTests
cd smoke-consumer && mvn compile exec:java
```

## Manual acceptance

1. `Frap.discover(page)` → clusters with size ≥ 2 on list pages
2. `Frap.generatePageObject` → compilable Java under `target/generated`
3. `withFrap` after testid change → heal in report
