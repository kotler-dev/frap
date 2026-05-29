# Release Policy

Versioning rules and CI cadence for frap distributions.

## Per-Registry Semver

**Rule:** npm, Maven, and Rust Core versions are independent. They advance according to their own semver, not a unified product version.

| Registry | Current | Semver Scope | Notes |
|----------|---------|--------------|-------|
| npm | 1.1.x | Product-aligned | Follows TypeScript SDK milestones |
| Maven | 1.0.x | Self-contained | Java 1.0.0 bundles features from product v1.0.0–v1.1.0+ |
| Rust Core | 0.1.x | Internal | Not published to crates.io; embedded in SDKs |

## Product Release vs Distribution Version

- **Product release** (roadmap): v1.0.0, v1.1.0, v1.2.0… — tracked in [FEATURES.md](../FEATURES.md)
- **Distribution version** (registry): `@frap/sdk` 1.1.1, `frap-core-java` 1.0.0 — tracked in [README.md](./README.md)

A product release may ship across multiple distribution versions. Example:
- Product v1.1.0 (Unified Context + RCA) → npm 1.1.0, later 1.1.1
- Product v1.2.0 (Page Object Gen) → Java 1.0.0 (already included), npm TBD

## Capability = Shipped Definition

A capability is **shipped** for a surface only when:

1. **Code** — implementation in Core + SDK
2. **Tests** — E2E gates passing in CI
3. **User docs** — installation, API reference, examples
4. **Demo** — runnable example or presentation slide

Partial coverage = `doc-gap` or `code-only`. See matrix files per surface.

## Git Tags and CI

Current trigger (both npm and Maven):

```yaml
# .github/workflows/publish-maven.yml (Maven) and publish.yml (npm)
on:
  push:
    tags:
      - 'java-v*'   # Maven only
      - 'v*'        # npm only (avoid shared tag unless skip-logic added)
```

**Known issue:** Single tag `v1.1.2` triggers both workflows, but manifests may have different versions (npm 1.1.2 vs Maven 1.0.0). Options:

1. **Separate tags** (recommended for future):
   - `v1.1.2` → npm only
   - `java-v1.0.1` → Maven only

2. **Workflow inputs** (manual dispatch):
   - `workflow_dispatch` with `package: [sdk, playwright, both]` (already in place)

3. **Skip logic** — CI checks version in manifest vs registry before publish.

## Versioning Rules

### npm
- Follows semver strictly
- `MAJOR` — breaking API changes
- `MINOR` — new capabilities
- `PATCH` — bug fixes

### Maven
- Same semver semantics
- `1.0.0` — initial stable release
- `1.0.1` — patch (binaries, docs)
- `1.1.0` — new capabilities

### Rust Core
- `0.x.y` — pre-1.0, internal workspace
- Bump minor when RPC contract changes
- Bump patch for fixes

## Naming Conventions

| Surface | Old (deprecated) | Canonical |
|---------|------------------|-----------|
| npm SDK | `@frap/frap`, `@frap/frap-sdk` | `@frap/sdk` |
| npm Playwright | `@frap/frap-playwright` | `@frap/playwright` |
| Maven groupId | `io.github.kotler-dev` (deprecated: `io.github.kotlerdev`) | `io.github.kotler-dev` |
| Java package | — (unchanged) | `io.github.kotlerdev.frap.*` |

## Verification Checklist (per release)

- [ ] Version bumped in manifest (`package.json`, `pom.xml`, `Cargo.toml`)
- [ ] CHANGELOG.md updated (for product-level changes)
- [ ] Matrix file updated (capability coverage)
- [ ] CI secrets valid (`NPM_TOKEN`, `CENTRAL_*`, `GPG_*`)
- [ ] Tag pushed: `git tag -a vX.Y.Z -m "Release X.Y.Z"`
- [ ] Post-publish: verify `npm view` / Maven Central index

## References

- [README.md](./README.md) — current versions and coordinates
- [FEATURES.md](../FEATURES.md) — product roadmap
- [docs/publishing-npm.md](../../docs/publishing-npm.md) — npm-specific guide
- [sdk/java/MAVEN_RELEASE_CHECKLIST.md](../../sdk/java/MAVEN_RELEASE_CHECKLIST.md) — Maven-specific checklist
