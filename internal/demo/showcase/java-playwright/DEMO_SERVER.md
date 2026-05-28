# Demo server setup

The Java demo tests open pages in a browser. They expect HTML at **`http://localhost:3000`** by default (conference scenarios under `/conference/`).

## Recommended: one command from repo root

Builds the healing engine, starts the demo server, installs Chromium, runs all E2E tests:

```bash
./scripts/run-java-e2e.sh
```

You do not need to configure paths manually when using this script.

## Server only (for manual `mvn test`)

From the **repository root**:

```bash
node internal/demo/site/server.js
```

Check that it responds:

```bash
curl -sf http://localhost:3000/conference/index.html
```

Then in another terminal:

```bash
cd internal/demo/showcase/java-playwright
mvn test
```

## Static pages without Node

If you only need one page (for example `schedule-heal.html`):

```bash
cd internal/demo/site
python3 -m http.server 3000
```

Open `http://localhost:3000/conference/schedule-heal.html`.

## Your own server

Point tests at another base URL:

```bash
mvn test -Dtest.server.url=https://your-host.example
```

## Healing engine (`FRAP_CORE_BIN`)

E2E tests call the Frap core over RPC. The path is set automatically by:

- `./scripts/run-java-e2e.sh`
- `pom.xml` default: `../../../../crates/target/release/frap-core-rpc` (from module dir)

If you see `frap-core-rpc binary not found`:

```bash
cd crates
cargo build --release -p frap-core --bin frap-core-rpc
export FRAP_CORE_BIN="$(pwd)/target/release/frap-core-rpc"
cd ../internal/demo/showcase/java-playwright
mvn test -Dfrap.core.bin="${FRAP_CORE_BIN}"
```

## Skip E2E

When the server, core binary, or browser is unavailable:

```bash
mvn test -DskipIT
```

## Install Chromium (once)

```bash
cd internal/demo/showcase/java-playwright
mvn -q exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
```
