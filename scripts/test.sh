#!/bin/bash

# Test script - runs all tests
# Usage: ./scripts/test.sh [cp001|cp002|cp003|all]

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

echo -e "${BLUE}=== fletta Test Runner ===${NC}"
echo ""

# Check if server is running
if ! curl -s http://localhost:$PORT > /dev/null 2>&1; then
    echo -e "${YELLOW}Test server not running. Starting...${NC}"
    "$SCRIPT_DIR/start.sh" "$PORT"
    echo ""
fi

# Ensure packages are built
echo -e "${YELLOW}Ensuring packages are built...${NC}"
if [ ! -d "$PROJECT_ROOT/sdk/typescript/dist" ] || [ ! -d "$PROJECT_ROOT/adapters/playwright/dist" ]; then
    "$SCRIPT_DIR/build.sh"
fi

# Check if Playwright browsers are installed
echo -e "${YELLOW}Checking Playwright browsers...${NC}"
if [ ! -d "$HOME/Library/Caches/ms-playwright/chromium-"* ] 2>/dev/null; then
    echo -e "${YELLOW}Playwright browsers not found. Installing chromium...${NC}"
    cd "$PROJECT_ROOT/e2e"
    npx playwright install chromium
fi

cd "$PROJECT_ROOT/e2e"

case $TEST_TYPE in
    cp001)
        echo -e "${BLUE}Running CP001: Happy Path${NC}"
        npx playwright test cp001-happy-path.spec.ts
        ;;
    cp002)
        echo -e "${BLUE}Running CP002: Refactor Heal${NC}"
        npx playwright test cp002-refactor-heal.spec.ts
        ;;
    cp003)
        echo -e "${BLUE}Running CP003: Safe Fail${NC}"
        npx playwright test cp003-safe-fail.spec.ts || true
        echo -e "${YELLOW}Note: CP003 is expected to have some failures${NC}"
        ;;
    all|*)
        echo -e "${BLUE}Running all tests${NC}"
        npx playwright test
        ;;
esac

echo ""
echo -e "${GREEN}=== Tests complete! ===${NC}"
echo ""
echo "Reports available at:"
echo "  - e2e/fletta-reports/"
echo "  - e2e/test-results/"
