# Metrics glossary

| Metric | Meaning |
|--------|---------|
| **positional share** | Locators using `css` + `nth-of-type` fallback |
| **high confidence** | `locator.confidence >= 0.8` |
| **semantic strategy** | `testid`, `role`, `placeholder`, `href`, `text`, `id` (not bare `css`) |
| **scoped locator** | `LocatorRecommendation.scope` set (SCOPE-uniqueness) |
| **relative list** | LIST cluster with `getByRole(container).filter(...).getByRole(child)` |
| **dom-benchmark** | Frozen HTML 01–09 + `ground-truth/*.expected.json` |
| **DP001–DP004** | Discover policy gates ([benchmark.md](../../docs/benchmark.md)) |

Thresholds for the locator-quality fixture are in `crates/core/tests/fixtures/locator-quality/expected.json`.
