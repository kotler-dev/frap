# Release audit playbook

## Pre-tag (blocker)

1. `cd crates && cargo test -p frap-core --test contract_locator_quality --test contract_dom_benchmark`
2. `cd sdk/java && mvn -P java-unit verify`
3. `./scripts/run-java-e2e.sh`
4. `./scripts/run-frap-mcp-verify.sh` (JDK 21)
5. `./scripts/quality-report.sh --release <ver> --baseline-tag java-v<prev>`
6. Fill EN + RU digests from report JSON; review sections 3–4–7–9.

## Post-tag (maintainer)

1. Tag `java-v*`, workflow `publish-maven.yml`.
2. Sonatype Portal: Publish validated deployments.
3. `curl` repo1 POMs; `smoke-consumer` @ release version.
4. GitHub Release: attach EN digest markdown.

## Do not claim in public digests

- Live production site pass rates (use C013 as optional manual, not a gate).
- Selenium/WebDriver support (v1.4.0 roadmap).
- npm discover parity (separate release line).
