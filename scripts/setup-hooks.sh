#!/bin/bash

# Setup git hooks for fletta development
# Usage: ./scripts/setup-hooks.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "Setting up git hooks for fletta development..."

# Create hooks directory if not exists
mkdir -p "$PROJECT_ROOT/.git/hooks"

# Install pre-push hook
if [ -f "$PROJECT_ROOT/.cursor/hooks/pre-push.sh" ]; then
    cp "$PROJECT_ROOT/.cursor/hooks/pre-push.sh" "$PROJECT_ROOT/.git/hooks/pre-push"
    chmod +x "$PROJECT_ROOT/.git/hooks/pre-push"
    echo "✓ Installed pre-push hook"
    echo "  Runs: cargo fmt --check, cargo clippy -D warnings"
else
    echo "⚠ pre-push.sh not found in .cursor/hooks/"
fi

echo ""
echo "Git hooks installed successfully!"
echo "Hooks will run automatically on 'git push'"
