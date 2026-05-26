#!/bin/bash

# Setup script - installs all dependencies in correct order
# Usage: ./scripts/setup.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== fletta Setup ===${NC}"
echo ""

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo -e "${RED}Error: Node.js is not installed${NC}"
    exit 1
fi

# Check if Rust is installed
if ! command -v cargo &> /dev/null; then
    echo -e "${YELLOW}Warning: Rust is not installed. Rust core tests will be skipped.${NC}"
fi

echo -e "${YELLOW}Installing SDK dependencies...${NC}"
cd "$PROJECT_ROOT/sdk/typescript"
npm install

echo -e "${YELLOW}Installing Playwright adapter dependencies...${NC}"
cd "$PROJECT_ROOT/adapters/playwright"
npm install

echo -e "${YELLOW}Installing E2E test dependencies...${NC}"
cd "$PROJECT_ROOT/e2e"
npm install

echo -e "${YELLOW}Installing Playwright browsers (chromium)...${NC}"
cd "$PROJECT_ROOT/e2e"
npx playwright install chromium

echo ""
echo -e "${GREEN}=== Setup complete! ===${NC}"
echo ""
echo "Next steps:"
echo "  ./scripts/build.sh    - Build all packages"
echo "  ./scripts/start.sh    - Start test server"
echo "  ./scripts/test.sh     - Run tests"
