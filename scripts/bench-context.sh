#!/bin/bash
# Measure unified context capture overhead (F002 AC: < 20%).
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

if ! curl -s http://localhost:3000 > /dev/null 2>&1; then
  "$SCRIPT_DIR/start.sh"
fi

if [ ! -d "$PROJECT_ROOT/adapters/playwright/dist" ]; then
  "$SCRIPT_DIR/build.sh"
fi

cd "$PROJECT_ROOT/e2e"
npx playwright test --config=playwright.context.config.ts context/overhead.spec.ts
