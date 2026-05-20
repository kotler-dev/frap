#!/bin/bash

# Start script - builds packages and starts test server
# Usage: ./scripts/start.sh [port]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

PORT=${1:-3000}
PID_FILE="$PROJECT_ROOT/.server.pid"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== fletta Start ===${NC}"
echo ""

# Check if port is already in use
if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo -e "${YELLOW}Port $PORT is already in use. Stopping existing server...${NC}"
    "$SCRIPT_DIR/stop.sh" "$PORT"
    sleep 1
fi

# Build first
echo -e "${YELLOW}Building packages...${NC}"
"$SCRIPT_DIR/build.sh"
echo ""

# Start test server
echo -e "${YELLOW}Starting test server on port $PORT...${NC}"
cd "$PROJECT_ROOT/test-app"

# Start server in background
node server.js &
SERVER_PID=$!

# Save PID
echo $SERVER_PID > "$PID_FILE"

# Wait for server to start
echo -e "${YELLOW}Waiting for server to start...${NC}"
for i in {1..10}; do
    if curl -s http://localhost:$PORT > /dev/null 2>&1; then
        echo ""
        echo -e "${GREEN}=== Server started! ===${NC}"
        echo ""
        echo -e "Test pages available at:"
        echo -e "  ${BLUE}http://localhost:$PORT${NC} - Index"
        echo -e "  ${BLUE}http://localhost:$PORT/cp001-stable.html${NC} - CP001"
        echo -e "  ${BLUE}http://localhost:$PORT/cp002-refactored.html${NC} - CP002"
        echo -e "  ${BLUE}http://localhost:$PORT/cp003-ambiguous.html${NC} - CP003"
        echo ""
        echo -e "To stop the server: ${YELLOW}./scripts/stop.sh${NC}"
        exit 0
    fi
    sleep 1
done

echo ""
echo -e "${RED}Server failed to start within 10 seconds${NC}"
exit 1
