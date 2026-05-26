#!/bin/bash

# Clean rebuild script - removes all build artifacts and rebuilds
# Usage: ./scripts/clean-build.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== frap Clean Build ===${NC}"
echo ""

# Function to clean directory
clean_dir() {
    local dir=$1
    local name=$2
    
    if [ -d "$dir" ]; then
        echo -e "${YELLOW}Cleaning $name...${NC}"
        cd "$dir"
        
        # Remove build artifacts
        rm -rf dist 2>/dev/null || true
        rm -rf node_modules 2>/dev/null || true
        rm -f package-lock.json 2>/dev/null || true
        rm -rf wasm 2>/dev/null || true
        
        echo -e "${GREEN}✓ $name cleaned${NC}"
    fi
}

# Clean SDK
clean_dir "$PROJECT_ROOT/sdk/typescript" "SDK"

# Clean Playwright adapter
clean_dir "$PROJECT_ROOT/adapters/playwright" "Playwright adapter"

# Clean E2E
clean_dir "$PROJECT_ROOT/e2e" "E2E tests"

# Clean Rust
if command -v cargo &> /dev/null; then
    echo -e "${YELLOW}Cleaning Rust core...${NC}"
    cd "$PROJECT_ROOT/crates"
    cargo clean
    echo -e "${GREEN}✓ Rust core cleaned${NC}"
fi

# Clean reports
rm -rf "$PROJECT_ROOT/e2e/frap-reports" 2>/dev/null || true
rm -rf "$PROJECT_ROOT/e2e/test-results" 2>/dev/null || true

echo ""
echo -e "${GREEN}=== Clean complete! ===${NC}"
echo ""

# Rebuild
echo -e "${BLUE}Starting rebuild...${NC}"
echo ""
"$SCRIPT_DIR/setup.sh"
echo ""
"$SCRIPT_DIR/build.sh"
