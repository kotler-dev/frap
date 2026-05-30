# Frap + Playwright (TypeScript example)

Minimal consumer project: wrap a Playwright locator so the test survives a renamed `data-testid`.

Uses the demo page `schedule-heal.html` from [`fixtures/fixtureconf/conference/`](../../../fixtures/fixtureconf/conference/) (test looks for `talk-open-healing`, page has `talk-card-open-healing`).

## Prerequisites

- Node.js 20+
- Demo site on port 3000 (from repo root):

```bash
./scripts/start.sh
```

## From monorepo (local packages)

```bash
cd examples/typescript/playwright
npm install
npx playwright install chromium
npm test
```

## From npm (published packages)

Replace `file:` dependencies in `package.json` with:

```bash
npm install @frap/sdk @frap/playwright @playwright/test
```

Then run `npm test` with the demo site available at `TEST_SERVER_URL` (default `http://localhost:3000`).

## Reports

After `npm test`:

```bash
open frap-reports/frap-debug.html
```

## See also

- [Java example](../../java/playwright/)
