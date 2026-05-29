# Запуск демо-сервера

Java-демо открывает страницы в браузере. По умолчанию ожидается **`http://localhost:3000`** (сценарии conference в `/conference/`).

## Рекомендуется: одна команда из корня репозитория

Собирает движок healing, поднимает сервер, ставит Chromium, гоняет E2E:

```bash
./scripts/run-java-e2e.sh
```

При этом скрипте пути настраивать вручную не нужно.

## Только сервер (для ручного `mvn test`)

Из **корня репозитория**:

```bash
node internal/demo/site/server.js
```

Проверка:

```bash
curl -sf http://localhost:3000/conference/index.html
```

В другом терминале:

```bash
cd internal/demo/showcase/java-playwright
mvn test
```

## Статика без Node

Если нужна одна страница (например `schedule-heal.html`):

```bash
cd internal/demo/site
python3 -m http.server 3000
```

Откройте `http://localhost:3000/conference/schedule-heal.html`.

## Свой сервер

Другой базовый URL:

```bash
mvn test -Dtest.server.url=https://your-host.example
```

## Движок healing (`FRAP_CORE_BIN`)

E2E вызывают ядро Frap по RPC. Путь задаётся автоматически:

- `./scripts/run-java-e2e.sh`
- значение по умолчанию в `pom.xml`: `../../../../crates/target/release/frap-core-rpc` (от каталога модуля)

Если видите `frap-core-rpc binary not found`:

```bash
cd crates
cargo build --release -p frap-core --bin frap-core-rpc
export FRAP_CORE_BIN="$(pwd)/target/release/frap-core-rpc"
cd ../internal/demo/showcase/java-playwright
mvn test -Dfrap.core.bin="${FRAP_CORE_BIN}"
```

## Пропустить E2E

Если нет сервера, бинарника или браузера:

```bash
mvn test -DskipIT
```

## Установить Chromium (один раз)

```bash
cd internal/demo/showcase/java-playwright
mvn -q exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
```
