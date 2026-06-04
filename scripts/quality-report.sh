#!/usr/bin/env bash
# Generate release quality metrics (JSON + Markdown scaffold).
# Usage: ./scripts/quality-report.sh --release 1.1.1 --baseline-tag java-v1.1.0
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

RELEASE=""
BASELINE_TAG=""
SKIP_TESTS=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --release) RELEASE="$2"; shift 2 ;;
    --baseline-tag) BASELINE_TAG="$2"; shift 2 ;;
    --skip-tests) SKIP_TESTS=1; shift ;;
    -h|--help)
      echo "Usage: $0 --release VERSION --baseline-tag TAG [--skip-tests]"
      exit 0
      ;;
    *) echo "Unknown arg: $1" >&2; exit 1 ;;
  esac
done

if [[ -z "${RELEASE}" || -z "${BASELINE_TAG}" ]]; then
  echo "ERROR: --release and --baseline-tag are required" >&2
  exit 1
fi

OUT_DIR="${ROOT_DIR}/target/quality"
mkdir -p "${OUT_DIR}"
JSON_OUT="${OUT_DIR}/quality-report-${RELEASE}.json"
MD_OUT="${OUT_DIR}/quality-report-${RELEASE}.md"
TS="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
GIT_SHA="$(git rev-parse --short HEAD 2>/dev/null || echo unknown)"

run_contracts() {
  local test_file="$1"
  if [[ "${SKIP_TESTS}" -eq 1 ]]; then
    echo "skipped"
    return 0
  fi
  if (cd crates && cargo test -p frap-core --test "${test_file}" -q >/dev/null 2>&1); then
    echo "pass"
  else
    echo "fail"
  fi
}

LOCATOR_STATUS="$(run_contracts contract_locator_quality)"
DOM_STATUS="$(run_contracts contract_dom_benchmark)"

DOM_PAGES="$(find fixtures/dom-benchmark/pages -name '*.html' 2>/dev/null | wc -l | tr -d ' ')"
GT_FILES="$(find fixtures/dom-benchmark/ground-truth -name '*.expected.json' 2>/dev/null | wc -l | tr -d ' ')"

JAVA_UNIT_TESTS=0
JAVA_PW_TESTS=0
if [[ -d adapters/playwright-java/src/test ]]; then
  JAVA_PW_TESTS="$(grep -r '@Test' adapters/playwright-java/src/test/java 2>/dev/null | wc -l | tr -d ' ')"
fi
if [[ -d sdk/java/frap-core-java/src/test ]]; then
  JAVA_UNIT_TESTS="$(grep -r '@Test' sdk/java/frap-core-java/src/test/java 2>/dev/null | wc -l | tr -d ' ')"
fi

E2E_TESTS=0
if [[ -d examples/java/playwright/src/test ]]; then
  E2E_TESTS="$(grep -r '@Test' examples/java/playwright/src/test/java 2>/dev/null | wc -l | tr -d ' ')"
fi

DIFF_STAT=""
DIFF_FILES=0
RELEASE_TAG="java-v${RELEASE}"
if git rev-parse "${RELEASE_TAG}" >/dev/null 2>&1; then
  DIFF_RANGE="${BASELINE_TAG}..${RELEASE_TAG}"
elif git rev-parse "${BASELINE_TAG}" >/dev/null 2>&1; then
  DIFF_RANGE="${BASELINE_TAG}..HEAD"
else
  DIFF_RANGE=""
fi
if [[ -n "${DIFF_RANGE}" ]]; then
  DIFF_STAT="$(git diff --stat "${DIFF_RANGE}" -- \
    sdk/java adapters/playwright-java crates/core 2>/dev/null | tail -1 || true)"
  DIFF_FILES="$(git diff --name-only "${DIFF_RANGE}" -- \
    sdk/java adapters/playwright-java crates/core 2>/dev/null | wc -l | tr -d ' ')"
else
  DIFF_STAT="baseline tag ${BASELINE_TAG} not found"
  DIFF_FILES=0
fi

read -r -d '' EXPECTED_JSON <<'EOF' || true
EOF
EXPECTED_PATH="crates/core/tests/fixtures/locator-quality/expected.json"
MAX_POS=""
MIN_HC=""
MIN_SEM=""
if [[ -f "${EXPECTED_PATH}" ]]; then
  MAX_POS="$(python3 -c "import json; d=json.load(open('${EXPECTED_PATH}')); print(d.get('max_positional',''))" 2>/dev/null || echo "")"
  MIN_HC="$(python3 -c "import json; d=json.load(open('${EXPECTED_PATH}')); print(d.get('min_high_confidence',''))" 2>/dev/null || echo "")"
  MIN_SEM="$(python3 -c "import json; d=json.load(open('${EXPECTED_PATH}')); print(d.get('min_semantic_strategy',''))" 2>/dev/null || echo "")"
  TOTAL_EL="$(python3 -c "import json; d=json.load(open('${EXPECTED_PATH}')); print(d.get('total',''))" 2>/dev/null || echo "")"
fi

cat > "${JSON_OUT}" <<EOF
{
  "release": "${RELEASE}",
  "baseline_tag": "${BASELINE_TAG}",
  "generated_at": "${TS}",
  "git_sha": "${GIT_SHA}",
  "contracts": {
    "contract_locator_quality": "${LOCATOR_STATUS}",
    "contract_dom_benchmark": "${DOM_STATUS}"
  },
  "dom_benchmark": {
    "html_pages": ${DOM_PAGES:-0},
    "ground_truth_files": ${GT_FILES:-0}
  },
  "locator_quality_fixture": {
    "total_elements": ${TOTAL_EL:-null},
    "max_positional": ${MAX_POS:-null},
    "min_high_confidence": ${MIN_HC:-null},
    "min_semantic_strategy": ${MIN_SEM:-null}
  },
  "test_counts": {
    "java_core_unit": ${JAVA_UNIT_TESTS},
    "playwright_java_unit": ${JAVA_PW_TESTS},
    "java_playwright_e2e": ${E2E_TESTS}
  },
  "baseline_delta": {
    "files_changed": ${DIFF_FILES},
    "diff_stat_summary": "$(echo "${DIFF_STAT}" | sed 's/"/\\"/g')"
  }
}
EOF

cat > "${MD_OUT}" <<EOF
# Quality report scaffold — v${RELEASE}

- Generated: ${TS}
- Git: ${GIT_SHA}
- Baseline: ${BASELINE_TAG}

## Contracts

| Test | Status |
|------|--------|
| contract_locator_quality | ${LOCATOR_STATUS} |
| contract_dom_benchmark | ${DOM_STATUS} |

## dom-benchmark

- HTML pages: ${DOM_PAGES}
- Ground-truth files: ${GT_FILES}

## Locator-quality fixture thresholds

- total: ${TOTAL_EL:-n/a}
- max_positional: ${MAX_POS:-n/a}
- min_high_confidence: ${MIN_HC:-n/a}
- min_semantic_strategy: ${MIN_SEM:-n/a}

## Test pyramid (static @Test count)

| Layer | Count |
|-------|-------|
| frap-core-java unit | ${JAVA_UNIT_TESTS} |
| playwright-java unit | ${JAVA_PW_TESTS} |
| examples/java/playwright e2e | ${E2E_TESTS} |

## Baseline delta (${BASELINE_TAG}..HEAD)

- Files changed (scoped paths): ${DIFF_FILES}
- ${DIFF_STAT}

Copy sections into \`docs/quality/en/releases/v${RELEASE}-digest.md\`.
EOF

echo "[quality-report] JSON: ${JSON_OUT}"
echo "[quality-report] MD:   ${MD_OUT}"
