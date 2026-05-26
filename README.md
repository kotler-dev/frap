# Frap

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

> Deterministic DOM binding for stable selectors. Frap binds your selectors to structure — automatic healing, LLM-ready grounding.

**Frap** parses element trees (DOM, ViewTree, accessibility), clusters components with deterministic algorithms, and generates stable locators for tests. When a selector breaks, it heals by signature matching with confidence scores and diff reports.

## No ML in core, AI-ready

Three layers:

| Layer | What | ML in Frap? |
|-------|------|-------------|
| **Core** (OSS) | Signatures, Drain3 clustering, self-healing | **No** — same input → same output |
| **Integration** | MCP tools: `discover`, `resolve`, `analyze` | **No** — JSON-RPC wire to agents |
| **Enhancements** (optional) | Semantic naming, step generation | **Optional** — separate package |

**Frap is a grounding layer** — deterministic infrastructure for AI agents and tests.

---

## Quick Start (Playwright)

```bash
npm install @frap/frap @frap/frap-playwright
```

```typescript
// playwright.config.ts
import { frapPlaywright, registerFrapSelector } from '@frap/frap-playwright';

export default defineConfig({
  ...frapPlaywright({ minConfidence: 0.85 }),
  use: {
    async setup({ selectors }) {
      await registerFrapSelector(selectors);
    },
  },
});
```

```typescript
// test.spec.ts
await page.locator('frap:[data-testid="submit"]').click();
```

See [docs/en/quickstart.md](docs/en/quickstart.md) for details.

---

## How It Works

1. Primary selector is attempted first
2. If not found, Frap extracts DOM signature
3. Similar elements are found using clustering (Drain3)
4. Confidence score is calculated for each candidate
5. If best candidate >= threshold, element is "healed"
6. Report includes original selector, new selector, and confidence

---

## Documentation

| Language | Quick Start | Integrations |
|----------|-------------|--------------|
| **English** | [docs/en/quickstart.md](docs/en/quickstart.md) | [docs/en/integrations.md](docs/en/integrations.md) |
| **Русский** | [docs/ru/quickstart.md](docs/ru/quickstart.md) | [docs/ru/integrations.md](docs/ru/integrations.md) |

Full design: [Frap.md](Frap.md)  
Adapter API: [adapters/playwright/README.md](adapters/playwright/README.md)

---

## Project Structure

```
frap/
├── crates/              # Rust core (signature, clustering, healing)
├── sdk/typescript/      # TypeScript SDK + WASM bindings
├── adapters/playwright/ # Playwright integration
├── test-app/            # Demo pages
├── e2e/                 # End-to-end tests
└── docs/                # Documentation (en/ru)
```

---

## Roadmap

- **v0.1.0** — TypeScript SDK + Playwright adapter
- **v0.2.0** — MCP integration + Page Object generator
- **v0.4.0** — Java SDK (Selenium/Selenide)
- **v1.0.0** — Multi-platform (Android/iOS)

See [CHANGELOG.md](CHANGELOG.md) for release notes.

---

## License

Apache-2.0 — see [LICENSE](LICENSE) and [NOTICE](NOTICE).
