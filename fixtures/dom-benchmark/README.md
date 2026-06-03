# DOM benchmark (F019 / C012)

Frozen snapshots + ground-truth thresholds for discover quality gates (DP001–DP004).

## Layout

- `pages/` — HTML for Playwright E2E (`page.setContent` or file URL)
- `snapshots/` — `{id}.snapshot.json` with `dom_snapshot` + `options`
- `ground-truth/` — `{id}.expected.json` thresholds

## Run

```bash
cd frap/crates && cargo test -p frap-core contract_dom_benchmark
```

## Pages

Coverage levels: [discover-scope.md](../../../project/architecture/discover-scope.md) (L1–L4). Proposal: [discover-coverage-proposal.md](../../../project/architecture/discover-coverage-proposal.md).

| Page | Pattern | Levels tested |
|------|---------|---------------|
| 01-catalog-list | LIST clusters, data-testid cards | L2 + **L3** (DP004) |
| 02-no-testid-buttons | text / role locators | L2 |
| 03-duplicate-labels | duplicate visible text (DP005 manual review) | L2 + ambiguity |
| 04-aria-only | aria-label primary | L2 |
| 05-shadow-open | button in open shadow root | L2 + shadow |
| 06-contenteditable | contenteditable region | L2 |
| 07-role-button | role=button on div | L2 |
| 08-generated-id | ember-style id demoted | L2 |
| 09-sibling-label | sibling `<label>` → `#calc-btn` (C013 Sber tile) | L2 (accessible name) |

## Policy

Update `ground-truth/` only when locator **policy** changes; otherwise fix Core or `SnapshotBuilder`.
