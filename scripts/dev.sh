#!/bin/bash

# Development mode - watches for changes and rebuilds
# Usage: ./scripts/dev.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Frap Dev Mode ===${NC}"
echo ""
echo "Starting development mode with file watching..."
echo ""

# Initial build
"$SCRIPT_DIR/build.sh"

# Start server
"$SCRIPT_DIR/start.sh" &

echo ""
echo -e "${GREEN}Dev mode active!${NC}"
echo ""
echo "Watching for changes..."
echo "Press Ctrl+C to stop"
echo ""

# Watch for changes (requires fswatch or inotifywait)
if command -v fswatch &> /dev/null; then
    fswatch -o "$PROJECT_ROOT/sdk/typescript/src" "$PROJECT_ROOT/adapters/playwright/src" | while read f; do
        echo -e "${YELLOW}Changes detected, rebuilding...${NC}"
        "$SCRIPT_DIR/build.sh"
    done
elif command -v inotifywait &> /dev/null; then
    inotifywait -m -r -e modify,create,delete "$PROJECT_ROOT/sdk/typescript/src" "$PROJECT_ROOT/adapters/playwright/src" | while read path action file; do
        echo -e "${YELLOW}Changes detected in $file, rebuilding...${NC}"
        "$SCRIPT_DIR/build.sh"
    done
else
    echo -e "${YELLOW}Install fswatch (macOS: brew install fswatch) for auto-rebuild${NC}"
    echo "Or run ./scripts/build.sh manually after changes"
fi

# Cleanup on exit
trap "$SCRIPT_DIR/stop.sh" EXIT
