# fletta

Self-healing test selectors for Playwright and beyond.

## Quick Start (using scripts)

```bash
# Setup everything (install dependencies)
./scripts/setup.sh

# Build all packages
./scripts/build.sh

# Start test server and build packages
./scripts/start.sh

# Run tests (in another terminal)
./scripts/test.sh

# Stop server
./scripts/stop.sh
```

## Quick Start (manual)

### 1. Start the test server

```bash
cd test-app
node server.js
```

The server will start on http://localhost:3000

### 2. Build packages

```bash
# Build SDK
cd sdk/typescript && npm install && npm run build

# Build Playwright adapter
cd ../../adapters/playwright && npm install && npm run build
```

### 3. Run the tests

```bash
cd e2e
npm install
npx playwright install chromium
npm run test
```

## Project Structure

```
fletta/
├── crates/                      # Rust Core
│   ├── signature/              # DOM signature extraction
│   ├── clustering/             # Drain3 clustering algorithm
│   └── healing/                # Self-healing fallback chain
├── sdk/typescript/             # TypeScript SDK
├── adapters/playwright/      # Playwright integration
├── test-app/                 # PoC test pages
├── e2e/                      # E2E tests
├── scripts/                  # Build/Run scripts
│   ├── setup.sh              # Install dependencies
│   ├── build.sh              # Build all packages
│   ├── clean-build.sh        # Clean and rebuild
│   ├── start.sh              # Start test server
│   ├── stop.sh               # Stop server
│   ├── test.sh               # Run tests
│   └── dev.sh                # Dev mode with watching
└── project/                  # Documentation
    ├── FEATURES.md           # Feature status
    ├── feature/              # Feature cards
    └── architecture/         # Architecture docs
```

## Core Concept

When a test selector fails, fletta:

1. Extracts the original element's signature (path, attributes, text)
2. Clusters similar elements using Drain3 algorithm
3. Calculates confidence score for each candidate
4. Heals the selector if confidence ≥ minConfidence (default: 0.85)
5. Reports the healing attempt with diff and top candidates

## Usage

```typescript
import { withFletta } from '@fletta/playwright';

test('payment flow', async ({ page }) => {
  // Wrap your locator with fletta
  const payButton = await withFletta(
    page.getByTestId('pay-btn'), 
    page,
    { minConfidence: 0.85 }
  );
  
  // If data-testid changes, fletta will find by signature
  await payButton.click();
});
```

## PoC Cases

| Case | Description | Status |
|------|-------------|--------|
| CP001 | Happy path — stable selector | Ready |
| CP002 | Refactor heal — changed testid | Ready |
| CP003 | Safe fail — ambiguous elements | Ready |

## Features

- ✅ Self-healing selectors via signature matching
- ✅ Playwright integration (wrapper API)
- ✅ JUnit and JSON reporting
- ✅ Configurable confidence threshold
- ✅ TypeScript SDK
- ✅ Rust core (ready for WASM)

## Available Scripts

| Script | Description |
|--------|-------------|
| `./scripts/setup.sh` | Install all dependencies |
| `./scripts/build.sh` | Build SDK, adapter, and Rust core |
| `./scripts/clean-build.sh` | Clean everything and rebuild |
| `./scripts/start.sh [port]` | Start test server (default: 3000) |
| `./scripts/stop.sh [port]` | Stop server and free port |
| `./scripts/test.sh [cp001\|cp002\|cp003\|all]` | Run specific or all tests |
| `./scripts/dev.sh` | Dev mode with auto-rebuild |

## Roadmap

- **v1.0.0** (MVP): Core + Playwright adapter — 90% complete
- **v1.1.0**: Unified Context + RCA
- **v1.2.0**: MCP Integration + Page Object Generator
- **v2.0.0**: Multi-platform (Android/iOS)

See [project/FEATURES.md](project/FEATURES.md) for details.

## License

MIT
