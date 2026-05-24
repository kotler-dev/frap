#!/bin/bash
# Pre-push hook: Run formatting and basic checks before pushing

set -e

echo "Running pre-push checks..."

# Check Rust formatting
if [ -d "crates" ]; then
    echo "Checking Rust formatting..."
    cd crates
    if ! cargo fmt -- --check; then
        echo ""
        echo "ERROR: Rust code is not formatted."
        echo "Run 'cd crates && cargo fmt' to fix formatting."
        exit 1
    fi
    cd ..
fi

echo "Pre-push checks passed!"
