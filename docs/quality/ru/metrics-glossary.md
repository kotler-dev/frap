# Глоссарий метрик

| Метрика | Значение |
|---------|----------|
| **доля позиционных** | Локаторы с fallback `css` + `nth-of-type` |
| **высокий confidence** | `locator.confidence >= 0.8` |
| **семантическая стратегия** | `testid`, `role`, `placeholder`, `href`, `text`, `id` (не голый `css`) |
| **scoped locator** | Заполнено `LocatorRecommendation.scope` |
| **relative list** | LIST-кластер с относительным локатором по контейнеру |
| **dom-benchmark** | HTML 01–09 + `ground-truth/*.expected.json` |
| **DP001–DP004** | Пороги discover ([benchmark.md](../../docs/benchmark.md)) |

Пороги fixture locator-quality: `crates/core/tests/fixtures/locator-quality/expected.json`.
