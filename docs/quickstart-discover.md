# Quickstart: discover locators on a page

See all interactive and semantic elements Frap finds on a page — with clusters and confidence scores.

## Prerequisites

- Java 17+
- Playwright browsers: `mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args=install` (from a module with Playwright)

## 1. Open a page

```java
page.navigate("https://your-app.example/catalog");
// or: page.setContent(htmlString);
```

## Which mode?

You do not need level names L1–L4 in daily work:

| Need | Use |
|------|-----|
| Full catalog for the page (LLM, exploration) | `MapOptions.semanticCatalog()` — default |
| Only buttons/inputs/links | `MapOptions.actionable()` |
| Repeating cards or rows | After discover, print `LIST` clusters (see step 3) |
| Stable locators for tests | Prefer `fragile: false` and `strategy` `data-testid` or `role` |

Details: [discover-scope.md](../../project/architecture/discover-scope.md).

## 2. Discover

```java
import io.github.kotlerdev.frap.core.dto.MapOptions;
import io.github.kotlerdev.frap.playwright.wrapper.Frap;

ElementMap map = Frap.discover(page, MapOptions.semanticCatalog());
```

## 3. Inspect output

```java
for (ElementNode el : map.elements()) {
    System.out.printf(
        "%s → %s (strategy=%s, conf=%.2f, fragile=%s, cluster=%s)%n",
        el.tag(),
        el.recommendedSelector(),
        el.locator().strategy(),
        el.locator().confidence(),
        el.fragile(),
        el.clusterId()
    );
}
map.clusters().stream()
    .filter(c -> c.clusterType() == ClusterType.LIST)
    .forEach(c -> System.out.println("LIST cluster: " + c.id() + " size=" + c.elementIds().size()));
```

## 4. Save JSON (for LLM or diff)

```java
Path out = Path.of("target/element-map.json");
Frap.discoverToFile(page, out, MapOptions.semanticCatalog());
```

## 5. Optional: Page Object

```java
Frap.generatePageObject(page, Path.of("target/pages"), GenerateOptions.javaPlaywright("CatalogPage", "com.example"));
```

## Benchmark fixtures (C012 / F019)

Reference pages and CI gates: [`fixtures/dom-benchmark/README.md`](../fixtures/dom-benchmark/README.md) (pages 01–09).

```bash
cd frap/crates && cargo test -p frap-core contract_dom_benchmark
./scripts/run-java-e2e.sh   # DomBenchmarkE2eTest + DiscoveryPageObjectE2eTest
```

Gates DP001–DP006: [docs/benchmark.md](../../docs/benchmark.md).

## Optional: live site (C013)

Prod URL + НУЦ + headed browser — not a CI gate:

```bash
./scripts/run-sber-explore.sh
```

Case: [C013-sber-person-giga-explore.md](../../project/cases/C013-sber-person-giga-explore.md).

More: [java-getting-started.md](../../docs/en/java-getting-started.md) (Discover section).
