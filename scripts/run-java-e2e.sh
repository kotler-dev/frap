#!/usr/bin/env bash
# Java Playwright E2E: frap-core-rpc + test-app + browser + examples/java-playwright-demo
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
exec "${ROOT_DIR}/scripts/run-l4-java-demo.sh"
