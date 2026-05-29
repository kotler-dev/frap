# Maven Central Release Checklist

## Prerequisites

### 1. Sonatype Account

- [ ] Create account at https://central.sonatype.org/publish/publish-guide/
- [ ] Verify `io.github.kotlerdev.frap` namespace (GitHub-based, auto-verified)

### 2. GPG Key

```bash
# Generate key
gpg --gen-key

# Get key ID
gpg --list-keys

# Upload to keyserver
gpg --keyserver keyserver.ubuntu.com --send-keys KEY_ID
```

### 3. GitHub Secrets

Add to https://github.com/kotler-dev/frap/settings/secrets/actions:

| Secret | Purpose | Status |
|--------|---------|--------|
| `CENTRAL_USERNAME` | Sonatype Central Portal token username | ✅ Configured |
| `CENTRAL_PASSWORD` | Sonatype Central Portal token password | ✅ Configured |
| `GPG_PRIVATE_KEY` | ASCII-armored GPG private key | ✅ Configured |
| `GPG_PASSPHRASE` | GPG key passphrase | ✅ Configured |

> **Note:** GPG key was generated for `io.github.kotler-dev.frap` namespace.
> Key ID: `D8B424D9603C9A7F`
>
> **Action required:** Publish the public key to a keyserver before first release:
> ```bash
> # Or use web interface: https://keyserver.ubuntu.com/
> gpg --keyserver keyserver.ubuntu.com --send-keys D8B424D9603C9A7F
> ```

### 4. Release Scope for 1.0.0

API surface in `frap-core-rpc`: `heal`, `analyze_rca`, `build_element_map`, `filter_element_map`, `generate_page_object`.

Publish to Maven Central:

- [ ] `io.github.kotlerdev.frap:frap-core-java:1.0.0`
- [ ] `io.github.kotlerdev.frap:frap-playwright:1.0.0`

Do not publish in 1.0.0:

- [ ] `frap-core-native` (kept in repository for local/experimental JNI flow)
- [ ] `internal/demo/showcase/java-playwright` (demo-only module)

## Release Steps

### 1. Prepare Release

```bash
# Update version (remove -SNAPSHOT)
cd sdk/java
mvn versions:set -DnewVersion=1.0.0
mvn versions:commit

# Verify
git diff sdk/java/pom.xml
```

### 2. Run Full Build

```bash
# Build native binaries locally (or use CI)
cd crates
cargo build --release -p frap-core --bin frap-core-rpc

# Copy to resources
mkdir -p ../sdk/java/frap-core-java/src/main/resources/META-INF/native
cp target/release/frap-core-rpc \
   ../sdk/java/frap-core-java/src/main/resources/META-INF/native/frap-core-rpc-$(uname -s | tr '[:upper:]' '[:lower:]')-$(uname -m)

# Test build
cd ../sdk/java
mvn clean verify -P release
```

### 3. Create Tag

```bash
# Commit version change
git add sdk/java/
git commit -m "Release 1.0.0"

# Create tag
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0
```

### 4. CI Release

GitHub Actions automatically:
- Builds native binaries for Linux/macOS
- Extracts them to resources
- Publishes `frap-core-java` and `frap-playwright` to Maven Central

Monitor at: https://github.com/kotler-dev/frap/actions

### 5. Verify on Central

Wait 10-30 minutes, then verify:

```bash
# Check Maven Central
curl "https://repo1.maven.org/maven2/io/github/kotlerdev/frap/frap-core-java/1.0.0/frap-core-java-1.0.0.pom"
curl "https://repo1.maven.org/maven2/io/github/kotlerdev/frap/frap-playwright/1.0.0/frap-playwright-1.0.0.pom"

# Or use in test project
mvn dependency:resolve -DincludeArtifactIds=frap-core-java
```

### 5b. Smoke (local, no Central)

```bash
cd sdk/java && mvn install -pl frap-core-java -DskipTests
cd smoke-consumer && mvn compile exec:java
# or: FRAP_CORE_BIN=../../crates/target/release/frap-core-rpc mvn compile exec:java
```

### 6. Post-Release

```bash
# Update to next SNAPSHOT
cd sdk/java
mvn versions:set -DnewVersion=1.0.1-SNAPSHOT
mvn versions:commit

git add sdk/java/
git commit -m "Prepare for next development iteration"
git push
```

## Troubleshooting

### "401 Unauthorized" in CI

**Cause**: Wrong Sonatype Central token credentials.

**Solution**: Verify `CENTRAL_USERNAME` and `CENTRAL_PASSWORD` in GitHub settings.

### "No public key" in staging

**Cause**: GPG key not uploaded to keyserver.

**Solution**: Upload the project's signing key (Key ID: `D8B424D9603C9A7F`):
```bash
gpg --keyserver keyserver.ubuntu.com --send-keys D8B424D9603C9A7F
```

Or submit manually at https://keyserver.ubuntu.com/ using this public key block:

```
-----BEGIN PGP PUBLIC KEY BLOCK-----

xsFNBGoZQLoBEADO/b21Fey8PDfiwhorOTgns6bUQ2Q+N2tuujNJFTo+eyY5F4uD
lxvtvOzWyUvAObcdyjOEMZDFZPRTgSZ0M1R5MaiKMdM2ygGve6/NaM26JJy5pmUM
ugx6Qq5KgrgUIlhmjW6kfKf2ZiHzxt9/gpIpNx1rKBWbFHGrvr3czY0mG/j4UOyC
XDkhyrhZCa3jYSx0quf9rumh438S63eP8awalw4s8GIGua2mx5BFm9w0YrfXhzjU
NfLuM8uto5K3FsIaHkSsKYP9nqoByO84ylqZZpQJbi6oBvRAGggaGKCmcbSSqmuy
FC/cRBoSxzTK+eZFGUhJSES8307UHqJZZ0JPUFFTmBqUgtvu2Qae5KetUCe4XFAf
ue29ue4kvyBK/kSCxQp1Q0nGrhZp8xhjFF7QHk0FH/0qL6QHYXuHTGj9Y+Jhnooy
0admtO6ukIIm56M7ofC4oUyK7VCsb+zOZuPrUKSjMW80a/qmSgs/9NdJ1+BnlM5k
AmtCldHje+ZEY2sfNpx3qRTTspEiYXx0GW9qIam0GnTumA7ebC2pD5j6M1Q2OF+A
rCxCfHJyMXqpii4yC5N14PX48AZ5LW156DOjFO9CwLWyJ9QaZ5FBmcmpPxrPLsS/
/Vz1e+QMAeTxe31aRpT718jVuubEzsvoVEYJ6PyXHyvavKkqb0nL/DxX4QARAQAB
zTRGcmFwIENJIChpby5naXRodWIua290bGVyLWRldi5mcmFwKSA8ZGV2QGZyYXAu
bG9jYWw+wsF2BBMBCAAgBQJqGUC7Ah4BFiEEcvQ7kvGFqmVlK1hj2LQk2WA8mn8A
CgkQ2LQk2WA8mn9Lxg//QPpiVfckHuyPYDyvYSc32Zfpb2HMiHYCGqPfGNEshkuy
OvEbo5qr5veoUVQITE9670bf8WYwyqnAqtWSdXnPRBJ7IoaRJXKe4Ch0tAqYtTyB
/SCBlK2JDtirKNcWVF+wG8aeX+PkcgQGuewPIY5PO7gvDaY7N7HxhQ3bIaiD0yFb
bBtXe/lIYJrm2X8iFMjl2sX3GRaZPBPtRF15LWdnpbgNwBFiYkHYkhQ86BdGyGX3
iEsIsaHoNGXuaoEnjb2d01ULmtRYQ0Iw+jDBArLrfjq08/1QEm4NmYW3bZm0g5bD
StesKm4SpP5vQZwR1D6/6fe7lwNQsyQj3gbIO+1xe1L06OIRkpt7sLxjO7lQNY3g
jBpAKUW8hLCjiPncqsSXNzJmh1sMPMi3Hhcv605+lYS2hL4sVqiGr/Ix6FLeSsIl
aP10TAWUy+PpCkl2QJQ/0+G+X1EOeaiPU6Y0Pj+Yq787NA/2hE1viQTIVvgu401L
ZnKfaVN39rAF3GYr8FabO1v4ISRVZdGsviwcS/EWigOixlIb9YulBSXxaivma954
RbU3LbkTdzHy5IJ4knEXdL/DseCbCkY3+s5/MpprK6yEAAlJfHSt/anh0gKUmvij
itLynnH5BF1P0z52mtziUfq8rjE7if9vpRVTMRkFn+ZDQErsJAuqTE7DF+g/2RQ=
=T01x
-----END PGP PUBLIC KEY BLOCK-----
```

### Missing artifacts in publish bundle

**Cause**: Native binaries not bundled.

**Solution**: Ensure workflow downloads artifacts:
```yaml
- uses: actions/download-artifact@v4
  with:
    pattern: native-binaries-*
    merge-multiple: true
    path: sdk/java/frap-core-java/src/main/resources/META-INF/native/
```

## Version Numbers

Follow [Semantic Versioning](https://semver.org/):

- `MAJOR` — Breaking API changes
- `MINOR` — New features, backwards compatible
- `PATCH` — Bug fixes

Examples:
- `1.0.0` — Initial release
- `1.1.0` — New features
- `1.1.1` — Bug fix
- `2.0.0` — Breaking changes

## Final Go/No-Go (before pushing `v1.0.0`)

- [ ] No `io.frap` remains in Maven coordinates (`pom.xml`, README snippets, demo POM)
- [ ] No `SNAPSHOT` remains in release-facing docs/snippets
- [ ] `mvn -P release -pl frap-core-java,../../adapters/playwright-java -am verify` passes
- [ ] `target` outputs include `.jar`, `-sources.jar`, `-javadoc.jar`, `.pom`, `.asc`
- [x] CI secrets (`CENTRAL_*`, `GPG_*`) are present and valid

---

## Appendix: Current Signing Key

### Active Release Signing Key (as of 2025-05-29)

| Property | Value |
|----------|-------|
| **Key ID** | `D8B424D9603C9A7F` |
| **UID** | Frap CI (io.github.kotler-dev.frap) <dev@frap.local> |
| **Algorithm** | RSA 4096-bit |
| **Keyserver** | https://keyserver.ubuntu.com/ |
| **Status** | ✅ Published and active |

**Search on keyserver:**
```
https://keyserver.ubuntu.com/pks/lookup?search=D8B424D9603C9A7F&fingerprint=on&op=index
```

**Location of private key:** GitHub Secret `GPG_PRIVATE_KEY` (not stored in repo)

### Key Rotation Procedure

When rotating the signing key:

1. Generate new key pair locally
2. Add to GitHub Secrets (`GPG_PRIVATE_KEY`, `GPG_PASSPHRASE`)
3. Publish new public key to keyserver
4. Update this documentation with new Key ID
5. Old key remains valid for artifacts already published
