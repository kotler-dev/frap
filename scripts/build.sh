#!/bin/bash

# Build script - builds all packages in dependency order
# Usage: ./scripts/build.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Frap Build ===${NC}"
echo ""

# Build WASM + SDK
echo -e "${YELLOW}Building WASM + SDK...${NC}"
cd "$PROJECT_ROOT/sdk/typescript"
npm run build
echo -e "${GREEN}✓ SDK built${NC}"
echo ""

# Build Playwright adapter
echo -e "${YELLOW}Building Playwright adapter...${NC}"
cd "$PROJECT_ROOT/adapters/playwright"
npm run build
echo -e "${GREEN}✓ Playwright adapter built${NC}"
echo ""

# Build Rust core (if Rust is installed)
if command -v cargo &> /dev/null; then
    cd "$PROJECT_ROOT/crates"
    
    # Check formatting first
    echo -e "${YELLOW}Checking Rust formatting...${NC}"
    if ! cargo fmt -- --check; then
        echo -e "${RED}✗ Formatting check failed${NC}"
        echo -e "${YELLOW}Run 'cd crates && cargo fmt' to fix${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Formatting OK${NC}"
    
    # Run clippy with warnings as errors (same as CI)
    echo -e "${YELLOW}Running clippy checks...${NC}"
    if ! cargo clippy -- -D warnings; then
        echo -e "${RED}✗ Clippy found issues${NC}"
        echo -e "${YELLOW}Run 'cd crates && cargo clippy' to see details${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Clippy passed${NC}"
    
    echo -e "${YELLOW}Building Rust core...${NC}"
    cargo build --release
    echo -e "${GREEN}✓ Rust core built${NC}"
    
    # Run Rust tests
    echo -e "${YELLOW}Running Rust tests...${NC}"
    cargo test
    echo -e "${GREEN}✓ Rust tests passed${NC}"
else
    echo -e "${YELLOW}Rust not installed, skipping Rust build${NC}"
fi

echo ""
echo -e "${GREEN}=== Build complete! ===${NC}"
