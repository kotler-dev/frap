# Testing

Playwright conference suite and generated report artifacts.

## Conference (CONF-*)

- Specs: [conference/](conference/)
- Config: [playwright.conference.config.ts](playwright.conference.config.ts) (`npm install` in this directory)
- Reports: `internal/testing/frap-reports/conference/` (gitignored)
- Guide: [conference/README.md](conference/README.md)

```bash
./scripts/test.sh conference
```

## Context layer

Specs remain under `e2e/context/` (C002–C004). Fixtures: [../demo/site/context/](../demo/site/context/).

```bash
./scripts/test.sh context
```
