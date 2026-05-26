# Integrations

## Playwright

### Installation

```bash
npm install @frap/frap @frap/frap-playwright
```

### Custom Selector Engine

Recommended for new projects:

```typescript
import { frapPlaywright, registerFrapSelector } from '@frap/frap-playwright';

export default defineConfig({
  ...frapPlaywright({
    minConfidence: 0.85,    // Confidence threshold for healing
    reportDir: './frap-reports',
    enableHealing: true,
    enableReporting: true,
  }),
  use: {
    async setup({ selectors }) {
      await registerFrapSelector(selectors);
    },
  },
});
```

Use selectors with `frap:` prefix:

```typescript
await page.locator('frap:[data-testid="submit"]').click();
```

### Wrapper API

For existing tests, wrap locators:

```typescript
import { withFrap } from '@frap/frap-playwright';

const button = await withFrap(page.getByTestId('submit'), page);
await button.click();
```

## Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `minConfidence` | number | 0.85 | Minimum score for healing |
| `reportDir` | string | './frap-reports' | Output directory |
| `enableHealing` | boolean | true | Enable self-healing |
| `enableReporting` | boolean | true | Generate reports |

## Other Platforms

- **Java** — roadmap (Selenium, Selenide)
- **Python** — roadmap

See [Frap.md](../../Frap.md) for the full artifact naming scheme.
