#!/usr/bin/env bash
# Verify frap-mcp reactor (Java 21). Requires FRAP_CORE_BIN or bundled macOS binary in JAR.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

if [[ -z "${JAVA_HOME:-}" ]] || ! "${JAVA_HOME}/bin/java" -version 2>&1 | grep -q 'version "21'; then
  for candidate in \
    "${HOME}/.sdkman/candidates/java/21.0.11-tem" \
    "${HOME}/.sdkman/candidates/java/21.0.10-tem" \
    "${HOME}/.local/jdk-21/Contents/Home" \
    "${HOME}/.local/jdk-21.0.5+11/Contents/Home"; do
    if [[ -x "${candidate}/bin/java" ]] && "${candidate}/bin/java" -version 2>&1 | grep -q 'version "21'; then
      export JAVA_HOME="${candidate}"
      break
    fi
  done
fi

if [[ -z "${JAVA_HOME:-}" ]] || ! "${JAVA_HOME}/bin/java" -version 2>&1 | grep -q 'version "21'; then
  echo "[frap-mcp] ERROR: JDK 21 required. Install e.g. sdk install java 21.0.11-tem" >&2
  exit 1
fi

export PATH="${JAVA_HOME}/bin:${PATH}"
echo "[frap-mcp] JAVA_HOME=${JAVA_HOME}"
"${JAVA_HOME}/bin/java" -version

if [[ -z "${FRAP_CORE_BIN:-}" ]]; then
  export FRAP_CORE_BIN="${ROOT_DIR}/crates/target/release/frap-core-rpc"
fi
if [[ ! -x "${FRAP_CORE_BIN}" ]]; then
  echo "[frap-mcp] building frap-core-rpc..."
  (cd crates && cargo build --release -p frap-core --bin frap-core-rpc)
fi
echo "[frap-mcp] FRAP_CORE_BIN=${FRAP_CORE_BIN}"

echo "[frap-mcp] installing local frap-core-java (required for extended LocatorRecommendation DTO)..."
mvn -f "${ROOT_DIR}/sdk/java/pom.xml" -pl frap-core-java -am install -DskipTests -q

mvn -f sdk/java/frap-mcp/pom.xml verify "$@"
