# Demo

Everything needed to show Frap: slides, static demo site, and runnable showcases.

## Structure

| Path | Contents |
|------|----------|
| [presentation/](presentation/) | Slide deck (`index.html`) |
| [site/](site/) | Static server + FixtureConf pages + context fixtures |
| [showcase/java-playwright/](showcase/java-playwright/) | Java Playwright E2E demo project |

## Demo server

```bash
./scripts/start.sh    # http://localhost:3000
```

Serves from [site/](site/):

- `/conference/*` — FixtureConf 2026 Spring
- `/context/*` — Context layer (C002–C004)

## Presentation

Open locally (file:// or via any static server):

```bash
open internal/demo/presentation/index.html
```

Uses CSS from [../palettte/](../palettte/).
