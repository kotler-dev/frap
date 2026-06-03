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

| Page | Pattern |
|------|---------|
| 01-catalog-list | LIST clusters, data-testid cards |
| 02-no-testid-buttons | text / role locators |
| 03-duplicate-labels | duplicate visible text (DP005 manual review) |
| 04-aria-only | aria-label primary |
| 05-shadow-open | button in open shadow root |
| 06-contenteditable | contenteditable region |
| 07-role-button | role=button on div |
| 08-generated-id | ember-style id demoted |
| 09-sibling-label | sibling `<label>` → `#calc-btn` (C013 Sber tile) |

## Policy

Update `ground-truth/` only when locator **policy** changes; otherwise fix Core or `SnapshotBuilder`.
