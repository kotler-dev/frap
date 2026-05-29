#!/usr/bin/env bash
# Smoke test for frap-core-rpc binary
# Usage: ./scripts/smoke-frap-core-rpc.sh [path_to_binary]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(dirname "$SCRIPT_DIR")"

# Default binary path
BINARY="${1:-$REPO_ROOT/crates/target/release/frap-core-rpc}"

# If not found, try dev build
if [[ ! -x "$BINARY" ]]; then
    BINARY="$REPO_ROOT/crates/target/debug/frap-core-rpc"
fi

# If still not found, build it
if [[ ! -x "$BINARY" ]]; then
    echo "[smoke] Binary not found, building..."
    cd "$REPO_ROOT/crates"
    cargo build --release -p frap-core --bin frap-core-rpc
    BINARY="$REPO_ROOT/crates/target/release/frap-core-rpc"
fi

echo "[smoke] Testing frap-core-rpc: $BINARY"

# Test 1: heal request
FIXTURE="$REPO_ROOT/crates/core/tests/fixtures/heal_request.json"
echo "[smoke] Test 1: heal request"
# Use jq to compact JSON to single line for NDJSON protocol
if command -v jq &> /dev/null; then
    RESPONSE=$(jq -c . "$FIXTURE" | "$BINARY")
else
    # Fallback: use node or python if available
    if command -v node &> /dev/null; then
        RESPONSE=$(node -e "console.log(JSON.stringify(require('$FIXTURE')))" | "$BINARY")
    elif command -v python3 &> /dev/null; then
        RESPONSE=$(python3 -c "import json; print(json.dumps(json.load(open('$FIXTURE'))))" | "$BINARY")
    else
        echo "[smoke] ERROR: jq, node, or python3 required to compact JSON"
        exit 1
    fi
fi
echo "[smoke] Response: $RESPONSE"

# Check that response has result and no error
if ! echo "$RESPONSE" | grep -q '"result":'; then
    echo "[smoke] FAIL: No result in response"
    exit 1
fi

if echo "$RESPONSE" | grep -q '"error":'; then
    echo "[smoke] FAIL: Error in response"
    exit 1
fi

echo "[smoke] Test 1: PASS"

# Test 2: analyze_rca with minimal timeline
echo "[smoke] Test 2: analyze_rca request"
RCA_REQUEST='{"id":2,"method":"analyze_rca","params":{"timeline":{"events":[{"kind":"ui","timestamp_ms":1000,"trace_id":"test-1","element":"[data-testid=pay-btn]","action":"click"},{"kind":"ui","timestamp_ms":2000,"trace_id":"test-1","element":"[data-testid=pay-btn]","action":"failure","detail":"Element not found"}]},"failure_at_ms":0,"window_ms":30000}}'

RCA_RESPONSE=$(echo "$RCA_REQUEST" | "$BINARY")
echo "[smoke] Response: $RCA_RESPONSE"

if ! echo "$RCA_RESPONSE" | grep -q '"result":'; then
    echo "[smoke] FAIL: No result in RCA response"
    exit 1
fi

echo "[smoke] Test 2: PASS"

# Test 3: invalid JSON
echo "[smoke] Test 3: invalid JSON"
INVALID_RESPONSE=$(echo 'not valid json' | "$BINARY")
echo "[smoke] Response: $INVALID_RESPONSE"

if ! echo "$INVALID_RESPONSE" | grep -q '"error":'; then
    echo "[smoke] FAIL: Expected error for invalid JSON"
    exit 1
fi

echo "[smoke] Test 3: PASS"

# Test 4: unknown method
echo "[smoke] Test 4: unknown method"
UNKNOWN_RESPONSE=$(echo '{"id":4,"method":"unknown_method","params":{}}' | "$BINARY")
echo "[smoke] Response: $UNKNOWN_RESPONSE"

if ! echo "$UNKNOWN_RESPONSE" | grep -q '"error":'; then
    echo "[smoke] FAIL: Expected error for unknown method"
    exit 1
fi

if ! echo "$UNKNOWN_RESPONSE" | grep -q 'METHOD_NOT_FOUND'; then
    echo "[smoke] FAIL: Expected METHOD_NOT_FOUND error"
    exit 1
fi

echo "[smoke] Test 4: PASS"

echo "[smoke] All tests passed!"
