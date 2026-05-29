#!/usr/bin/env bash
# Java Playwright E2E: frap-core-rpc + fixtureconf + browser + examples/java/playwright
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

echo "[java-e2e] repo: ${ROOT_DIR}"

echo "[java-e2e] building frap-core-rpc..."
cd "${ROOT_DIR}/crates"
cargo build --release -p frap-core --bin frap-core-rpc
export FRAP_CORE_BIN="${ROOT_DIR}/crates/target/release/frap-core-rpc"
echo "[java-e2e] FRAP_CORE_BIN=${FRAP_CORE_BIN}"

echo "[java-e2e] starting fixtureconf server (:3000)..."
cd "${ROOT_DIR}/fixtures/fixtureconf"
node server.js > /tmp/frap-test-app.log 2>&1 &
TEST_APP_PID=$!
trap 'kill "${TEST_APP_PID}" >/dev/null 2>&1 || true' EXIT

echo "[java-e2e] waiting for http://localhost:3000 ..."
for _ in {1..40}; do
  if curl -sf http://localhost:3000 >/dev/null; then
    echo "[java-e2e] fixtureconf is up"
    break
  fi
  sleep 0.2
done
curl -sf http://localhost:3000 >/dev/null

echo "[java-e2e] installing sdk/java artifacts (skip tests)..."
cd "${ROOT_DIR}/sdk/java"
mvn -q install -DskipTests

echo "[java-e2e] installing Playwright Chromium (Java)..."
cd "${ROOT_DIR}/examples/java/playwright"
mvn -q org.codehaus.mojo:exec-maven-plugin:3.1.0:java \
  -Dexec.mainClass=com.microsoft.playwright.CLI \
  -Dexec.args="install chromium"

echo "[java-e2e] running Java E2E suite (@Tag e2e) in examples/java/playwright..."
mvn test -Dfrap.core.bin="${FRAP_CORE_BIN}"

echo "[java-e2e] report artifacts:"
ls -la "${ROOT_DIR}/examples/java/playwright/target/frap-reports/conference"
