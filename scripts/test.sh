#!/bin/bash

# Test script - runs fletta E2E tests
# Usage: ./scripts/test.sh [conference|conference-fail|conference-dbg|conference-single|context|debug|all]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

TEST_TYPE=${1:-all}
PORT=${2:-3000}

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

CONF_CONFIG="playwright.conference.config.ts"
CTX_CONFIG="playwright.context.config.ts"

echo -e "${BLUE}=== fletta Test Runner ===${NC}"
echo ""

if ! curl -s http://localhost:$PORT > /dev/null 2>&1; then
    echo -e "${YELLOW}Test server not running. Starting...${NC}"
    "$SCRIPT_DIR/start.sh" "$PORT"
    echo ""
fi

echo -e "${YELLOW}Ensuring packages are built...${NC}"
if [ ! -d "$PROJECT_ROOT/sdk/typescript/dist" ] || [ ! -d "$PROJECT_ROOT/adapters/playwright/dist" ]; then
    "$SCRIPT_DIR/build.sh"
fi

echo -e "${YELLOW}Checking Playwright browsers...${NC}"
if [ ! -d "$HOME/Library/Caches/ms-playwright/chromium-"* ] 2>/dev/null; then
    echo -e "${YELLOW}Playwright browsers not found. Installing chromium...${NC}"
    cd "$PROJECT_ROOT/e2e"
    npx playwright install chromium
fi

cd "$PROJECT_ROOT/e2e"

case $TEST_TYPE in
    conference)
        echo -e "${BLUE}Running Conference demo (FixtureConf)${NC}"
        npx playwright test --config="$CONF_CONFIG" || true
        node conference/verify-reports.mjs
        echo -e "${YELLOW}Note: CONF-*-FAIL cases are expected to fail${NC}"
        ;;
    conference-fail)
        echo -e "${BLUE}Running Conference FAIL cases only${NC}"
        CONF_FAIL_ONLY=1 npx playwright test --config="$CONF_CONFIG" --grep 'FAIL' || true
        CONF_FAIL_ONLY=1 npx playwright test --config="$CONF_CONFIG" zzz-reporting.spec.ts -g 'CONF-RPT-RUN-FAIL' || true
        ;;
    conference-dbg)
        echo -e "${BLUE}Running Conference debug cases${NC}"
        npx playwright test --config="$CONF_CONFIG" --grep 'CONF-.*DBG|CONF-SH|CONF-POL' || true
        ;;
    conference-single)
        echo -e "${BLUE}Running single debug case (explorer stub)${NC}"
        npx playwright test --config="$CONF_CONFIG" dbg-single.spec.ts
        ;;
    context)
        echo -e "${BLUE}Running Context Layer E2E (C002/C003/C004)${NC}"
        cd "$PROJECT_ROOT/crates"
        cargo test -p fletta-context
        cd "$PROJECT_ROOT/e2e"
        npx playwright test --config="$CTX_CONFIG"
        node context/verify-context.mjs
        node context/verify-reports.mjs
        node context/verify-rca.mjs
        echo -e "${YELLOW}Note: C002 and slow C003 use test.fail (expected failures)${NC}"
        ;;
    debug)
        echo -e "${BLUE}Running F012 debug-mode specs${NC}"
        npx playwright test debug-mode.spec.ts
        ;;
    all|*)
        echo -e "${BLUE}Running debug-mode + Conference demo${NC}"
        npx playwright test debug-mode.spec.ts
        npx playwright test --config="$CONF_CONFIG" || true
        ;;
esac

echo ""
echo -e "${GREEN}=== Tests complete! ===${NC}"
echo ""
echo "Reports:"
echo "  - e2e/fletta-reports/conference/  (Conference project)"
echo "  - e2e/fletta-reports/context/     (Context Layer C002–C004)"
echo "  - e2e/test-results/"
