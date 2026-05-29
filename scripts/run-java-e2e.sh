#!/usr/bin/env bash
# Java Playwright E2E: frap-core-rpc + demo site + browser + internal/demo/showcase/java-playwright
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
exec "${ROOT_DIR}/scripts/run-l4-java-demo.sh"
