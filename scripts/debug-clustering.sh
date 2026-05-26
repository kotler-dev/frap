#!/bin/bash

# Debug clustering - runs tests in headed mode with visible browser
# Usage: ./scripts/debug-clustering.sh [cp001|cp002|cp003]

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

TEST_TYPE=${1:-cp002}
PORT=${2:-3000}

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== frap Debug Mode (headed) ===${NC}"
echo ""

# Ensure server is running
if ! curl -s http://localhost:$PORT > /dev/null 2>&1; then
    echo -e "${YELLOW}Starting test server...${NC}"
    "$SCRIPT_DIR/start.sh" "$PORT" &
    sleep 3
fi

# Build packages
echo -e "${YELLOW}Building packages...${NC}"
cd "$PROJECT_ROOT/sdk/typescript" && npm run build
cd "$PROJECT_ROOT/adapters/playwright" && npm run build

echo ""
echo -e "${BLUE}Running $TEST_TYPE in headed debug mode${NC}"
echo -e "${YELLOW}Browser will open - watch the console output${NC}"
echo ""

cd "$PROJECT_ROOT/e2e"

case $TEST_TYPE in
    cp001)
        npx playwright test cp001-happy-path.spec.ts --headed --reporter=list
        ;;
    cp002)
        npx playwright test cp002-refactor-heal.spec.ts:6 --headed --reporter=list --debug
        ;;
    cp003)
        npx playwright test cp003-safe-fail.spec.ts --headed --reporter=list
        ;;
    *)
        echo -e "${RED}Unknown test type: $TEST_TYPE${NC}"
        echo "Usage: ./scripts/debug-clustering.sh [cp001|cp002|cp003]"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}=== Debug session complete ===${NC}"
