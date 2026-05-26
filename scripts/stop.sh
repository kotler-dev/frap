#!/bin/bash

# Stop script - stops test server and frees ports
# Usage: ./scripts/stop.sh [port]

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

PORT=${1:-3000}
PID_FILE="$PROJECT_ROOT/.server.pid"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Stopping test server...${NC}"

# Try to stop by PID file
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if kill -0 "$PID" 2>/dev/null; then
        echo "Killing server process (PID: $PID)"
        kill "$PID" 2>/dev/null || kill -9 "$PID" 2>/dev/null || true
        rm -f "$PID_FILE"
        echo -e "${GREEN}✓ Server stopped${NC}"
    else
        echo "PID file exists but process not running"
        rm -f "$PID_FILE"
    fi
fi

# Kill any process using the port
PIDS=$(lsof -Pi :$PORT -sTCP:LISTEN -t 2>/dev/null || true)
if [ -n "$PIDS" ]; then
    echo "Killing processes on port $PORT: $PIDS"
    echo "$PIDS" | xargs kill -9 2>/dev/null || true
    echo -e "${GREEN}✓ Port $PORT freed${NC}"
else
    echo "No processes found on port $PORT"
fi

# Also check for common test server patterns
NODE_PIDS=$(ps aux | grep "node server.js" | grep -v grep | awk '{print $2}' || true)
if [ -n "$NODE_PIDS" ]; then
    echo "Killing node server processes"
    echo "$NODE_PIDS" | xargs kill -9 2>/dev/null || true
fi

echo -e "${GREEN}=== Server stopped ===${NC}"
