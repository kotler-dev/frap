# Playbook аудита релиза

## До тега (блокер)

1. `cargo test -p frap-core --test contract_locator_quality --test contract_dom_benchmark`
2. `mvn -P java-unit verify` в `sdk/java`
3. `./scripts/run-java-e2e.sh`
4. `./scripts/run-frap-mcp-verify.sh` (JDK 21)
5. `./scripts/quality-report.sh --release <ver> --baseline-tag java-v<prev>`
6. Заполнить digest EN + RU; проверить секции 3–4–7–9.

## После тега

1. Тег `java-v*`, workflow `publish-maven.yml`.
2. Sonatype Portal: Publish.
3. Проверка POM на repo1; smoke-consumer.
4. GitHub Release: приложить EN digest.

## Не заявлять в публичных digest

- Процент успеха на live prod (C013 — опционально вручную).
- Selenium/WebDriver (roadmap v1.4.0).
- Паритет npm discover.
