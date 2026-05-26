#!/bin/bash
# Pre-push hook: Run formatting and clippy checks before pushing

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
    
    # Run clippy with warnings as errors (same as CI)
    echo "Running Rust clippy..."
    if ! cargo clippy -- -D warnings 2>&1; then
        echo ""
        echo "ERROR: Clippy found issues."
        echo "Run 'cd crates && cargo clippy' to see details."
        exit 1
    fi
    cd ..
fi

echo "Pre-push checks passed!"
