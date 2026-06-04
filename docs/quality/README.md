# Release Quality Digest

Measured release analysis for the Java SDK and Core locator pipeline. Each published Maven line (`java-v*`) ships with a paired digest.

## Languages

| Language | Index |
|----------|--------|
| English | [en/releases/README.md](en/releases/README.md) |
| Русский | [ru/releases/README.md](ru/releases/README.md) |

Digests in one language do not cross-link to the other; use this page to switch.

## Generate a report

```bash
cd frap
./scripts/quality-report.sh --release 1.1.1 --baseline-tag java-v1.1.0
```

Output: `target/quality/quality-report-<version>.json` and `.md` (metrics scaffold).

## Maven Central gate

See [`sdk/java/MAVEN_RELEASE_CHECKLIST.md`](../sdk/java/MAVEN_RELEASE_CHECKLIST.md) § Pre-publish: Quality Digest.
