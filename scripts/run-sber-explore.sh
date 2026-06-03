#!/usr/bin/env bash
# C013: live discover Sber person_giga (Playwright + Frap).
# НУЦ-сертификаты в macOS; для explore лучше Яндекс/Chrome, не bundled Chromium.
set -euo pipefail

# =============================================================================
# НАСТРОЙКИ — меняйте только этот блок (без export и без чтения доки)
# =============================================================================

# Страница
URL="https://www.sberbank.ru/ru/person_giga"

# Браузер: true = окно на экране (нужно для Яндекса и паузы на модале)
HEADED=true

# true = падать, если якорь не найден; false = probe (только WARN в логе)
STRICT=true
# true = то же, что STRICT=false (удобный переключатель)
PROBE=false

# Браузер: auto | chromium | bundled
#   auto — на macOS ищет Яндекс, иначе Playwright Chromium
#   chromium / bundled — только встроенный Chromium Playwright (часто без НУЦ)
BROWSER="auto"
# Путь к бинарнику (пусто = авто). Пример:
# BROWSER_EXECUTABLE="/Applications/Yandex.app/Contents/MacOS/Yandex"
BROWSER_EXECUTABLE=""

# Пауза на открытом модале, мс (0 = выкл). Работает только при HEADED=true
PAUSE_AFTER_MODAL_MS=8000

# Замедление действий Playwright, мс (0 = выкл)
SLOW_MO=0

# Лимит элементов в Frap.discover
MAX_ELEMENTS=12000

# Генерировать skeleton Page Object в артефактах
GENERATE_PO=true

# Имя папки run-<RUN_ID> в project/artifacts/sber-person-giga/ (пусто = timestamp)
RUN_ID=""

# Полный путь к артефактам (пусто = workspace/.../run-<RUN_ID>)
OUTPUT_DIR=""

# true = не пересобирать cargo/mvn и не качать Chromium (быстрый повтор)
SKIP_BUILD=false

# =============================================================================
# Дальше — логика скрипта (обычно не трогать)
# =============================================================================

usage() {
  cat <<'EOF'
Настройки — блок «НАСТРОЙКИ» в начале scripts/run-sber-explore.sh:

  URL                      страница (person_giga)
  HEADED                   true = окно браузера (нужно для Яндекса)
  STRICT / PROBE           false / PROBE=true = не падать на якорях
  BROWSER                  auto | chromium | bundled
  BROWSER_EXECUTABLE       путь к Yandex/Chrome; "" = авто на macOS
  PAUSE_AFTER_MODAL_MS     пауза на модале, мс (0 = выкл)
  SLOW_MO                  замедление Playwright
  MAX_ELEMENTS             лимит discover
  GENERATE_PO              skeleton Page Object
  RUN_ID / OUTPUT_DIR      каталог артефактов
  SKIP_BUILD               true = без cargo/mvn

Запуск: ./scripts/run-sber-explore.sh
Разово из терминала: FRAP_EXPLORE_HEADED=false ./scripts/run-sber-explore.sh
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

# Опционально: env перекрывает блок выше (для CI/одноразовых экспериментов)
_apply_env() {
  local name="$1" var="$2"
  local env_name="FRAP_EXPLORE_${name}"
  if [[ -n "${!env_name:-}" ]]; then
    printf -v "$var" '%s' "${!env_name}"
  fi
}
_apply_env URL URL
_apply_env HEADED HEADED
_apply_env STRICT STRICT
_apply_env PROBE PROBE
_apply_env BROWSER BROWSER
_apply_env BROWSER_EXECUTABLE BROWSER_EXECUTABLE
_apply_env PAUSE_AFTER_MODAL_MS PAUSE_AFTER_MODAL_MS
_apply_env SLOW_MO SLOW_MO
_apply_env MAX_ELEMENTS MAX_ELEMENTS
_apply_env GENERATE_PO GENERATE_PO
_apply_env RUN_ID RUN_ID
_apply_env OUTPUT_DIR OUTPUT_DIR
_apply_env SKIP_BUILD SKIP_BUILD

if [[ "${PROBE}" == "true" ]]; then
  STRICT=false
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORKSPACE_DIR="$(cd "${ROOT_DIR}/.." && pwd)"
RUN_ID="${RUN_ID:-$(date +%Y%m%d-%H%M%S)}"
OUTPUT_DIR="${OUTPUT_DIR:-${WORKSPACE_DIR}/project/artifacts/sber-person-giga/run-${RUN_ID}}"

if [[ -s "${HOME}/.sdkman/bin/sdkman-init.sh" ]]; then
  set +u
  ZSH_VERSION="${ZSH_VERSION:-}"
  # shellcheck disable=SC1090
  source "${HOME}/.sdkman/bin/sdkman-init.sh"
  set -u
fi

echo "[sber-explore] --- config ---"
echo "[sber-explore] url=${URL}"
echo "[sber-explore] headed=${HEADED} strict=${STRICT} probe=${PROBE}"
echo "[sber-explore] browser=${BROWSER} executable=${BROWSER_EXECUTABLE:-<auto>}"
echo "[sber-explore] pauseAfterModalMs=${PAUSE_AFTER_MODAL_MS} slowMo=${SLOW_MO}"
echo "[sber-explore] maxElements=${MAX_ELEMENTS} generatePo=${GENERATE_PO} skipBuild=${SKIP_BUILD}"
echo "[sber-explore] output=${OUTPUT_DIR}"
echo "[sber-explore] --------------"

if [[ "${SKIP_BUILD}" == "true" ]]; then
  export FRAP_CORE_BIN="${FRAP_CORE_BIN:-${ROOT_DIR}/crates/target/release/frap-core-rpc}"
  echo "[sber-explore] skip build, core=${FRAP_CORE_BIN}"
  cd "${ROOT_DIR}/examples/java/playwright"
else
  echo "[sber-explore] building frap-core-rpc..."
  cd "${ROOT_DIR}/crates"
  cargo build --release -p frap-core --bin frap-core-rpc
  export FRAP_CORE_BIN="${ROOT_DIR}/crates/target/release/frap-core-rpc"

  echo "[sber-explore] installing Java SDK..."
  cd "${ROOT_DIR}/sdk/java"
  mvn -q install -DskipTests

  echo "[sber-explore] ensuring Playwright Chromium..."
  cd "${ROOT_DIR}/examples/java/playwright"
  mvn -q org.codehaus.mojo:exec-maven-plugin:3.1.0:java \
    -Dexec.mainClass=com.microsoft.playwright.CLI \
    -Dexec.args="install chromium"
fi

mkdir -p "${OUTPUT_DIR}"

resolved_executable="${BROWSER_EXECUTABLE}"
if [[ -z "${resolved_executable}" && "${BROWSER}" != "chromium" && "${BROWSER}" != "bundled" ]]; then
  for candidate in \
    "/Applications/Yandex.app/Contents/MacOS/Yandex" \
    "/Applications/Yandex Browser.app/Contents/MacOS/Yandex"; do
    if [[ -x "${candidate}" ]]; then
      resolved_executable="${candidate}"
      break
    fi
  done
fi

MVN_BROWSER_ARGS=()
if [[ -n "${resolved_executable}" ]]; then
  echo "[sber-explore] using browser: ${resolved_executable}"
  MVN_BROWSER_ARGS=(-Dfrap.explore.browserExecutable="${resolved_executable}")
else
  echo "[sber-explore] using bundled Playwright Chromium (задайте BROWSER_EXECUTABLE для НУЦ)"
fi

echo "[sber-explore] running test..."
mvn test \
  -Dtest=SberPersonGigaExploreTest \
  -Djunit.groups=manual-live \
  -Dfrap.core.bin="${FRAP_CORE_BIN}" \
  -Dfrap.explore.outputDir="${OUTPUT_DIR}" \
  -Dfrap.explore.headed="${HEADED}" \
  -Dfrap.explore.url="${URL}" \
  -Dfrap.explore.generatePo="${GENERATE_PO}" \
  -Dfrap.explore.strictAssertions="${STRICT}" \
  -Dfrap.explore.browser="${BROWSER}" \
  -Dfrap.explore.maxElements="${MAX_ELEMENTS}" \
  -Dfrap.explore.slowMo="${SLOW_MO}" \
  -Dfrap.explore.pauseAfterModalMs="${PAUSE_AFTER_MODAL_MS}" \
  "${MVN_BROWSER_ARGS[@]}"

echo "[sber-explore] done — artifacts:"
ls -la "${OUTPUT_DIR}"
