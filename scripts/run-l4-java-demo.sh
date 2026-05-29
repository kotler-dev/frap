#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Initialize SDKMAN (for mvn) without sourcing zshrc.
# In bash with `set -u`, SDKMAN's init may reference ZSH_VERSION, so guard it.
if [[ -s "${HOME}/.sdkman/bin/sdkman-init.sh" ]]; then
  set +u
  ZSH_VERSION="${ZSH_VERSION:-}"
  # shellcheck disable=SC1090
  source "${HOME}/.sdkman/bin/sdkman-init.sh"
  set -u
fi

echo "[l4] repo: ${ROOT_DIR}"

echo "[l4] building frap-core-rpc..."
cd "${ROOT_DIR}/crates"
cargo build --release -p frap-core --bin frap-core-rpc
export FRAP_CORE_BIN="${ROOT_DIR}/crates/target/release/frap-core-rpc"
echo "[l4] FRAP_CORE_BIN=${FRAP_CORE_BIN}"

echo "[l4] starting demo site server (:3000)..."
cd "${ROOT_DIR}/internal/demo/site"
node server.js > /tmp/frap-test-app.log 2>&1 &
TEST_APP_PID=$!
trap 'kill "${TEST_APP_PID}" >/dev/null 2>&1 || true' EXIT

echo "[l4] waiting for http://localhost:3000 ..."
for _ in {1..40}; do
  if curl -sf http://localhost:3000 >/dev/null; then
    echo "[l4] test-app is up"
    break
  fi
  sleep 0.2
done
curl -sf http://localhost:3000 >/dev/null

echo "[l4] installing sdk/java artifacts (skip tests)..."
cd "${ROOT_DIR}/sdk/java"
mvn -q install -DskipTests

echo "[l4] installing Playwright Chromium (Java)..."
cd "${ROOT_DIR}/internal/demo/showcase/java-playwright"
mvn -q org.codehaus.mojo:exec-maven-plugin:3.1.0:java \
  -Dexec.mainClass=com.microsoft.playwright.CLI \
  -Dexec.args="install chromium"

echo "[l4] running Java E2E suite (@Tag e2e) in internal/demo/showcase/java-playwright..."
mvn test -Dfrap.core.bin="${FRAP_CORE_BIN}"

echo "[l4] report artifacts:"
ls -la "${ROOT_DIR}/internal/demo/showcase/java-playwright/target/frap-reports/conference"

