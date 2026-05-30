# Examples

Runnable consumer projects by language — mirror of [`sdk/`](../sdk/) layout, but only copy-paste demos (not libraries).

| Language | Path | Stack | Run |
|----------|------|-------|-----|
| Java | [java/playwright/](java/playwright/) | `frap-playwright` + Playwright Java | `./scripts/run-java-e2e.sh` |
| TypeScript | [typescript/playwright/](typescript/playwright/) | `@frap/sdk` + `@frap/playwright` | See [typescript/playwright/README.md](typescript/playwright/README.md) |

## Demo site (FixtureConf)

Examples and E2E use [`fixtures/fixtureconf/`](../fixtures/fixtureconf/) at `http://localhost:3000`:

```bash
./scripts/start.sh
```

## Layout

```
examples/
├── java/playwright/
└── typescript/playwright/

fixtures/fixtureconf/     # shared AUT (conference + context pages)
```

For SDK source and adapters, see [`sdk/`](../sdk/) and [`adapters/`](../adapters/).
