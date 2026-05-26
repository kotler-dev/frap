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

- [ ] `OSSRH_USERNAME` — Sonatype JIRA username
- [ ] `OSSRH_TOKEN` — Sonatype JIRA password or token
- [ ] `GPG_PRIVATE_KEY` — Output of `gpg --armor --export-secret-keys KEY_ID`
- [ ] `GPG_PASSPHRASE` — Your GPG key passphrase

## Release Steps

### 1. Prepare Release

```bash
# Update version (remove -SNAPSHOT)
cd sdk/java
mvn versions:set -DnewVersion=1.1.0
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
git commit -m "Release 1.1.0"

# Create tag
git tag -a v1.1.0 -m "Release 1.1.0"
git push origin v1.1.0
```

### 4. CI Release

GitHub Actions automatically:
- Builds native binaries for Linux/macOS
- Extracts them to resources
- Publishes to Maven Central staging

Monitor at: https://github.com/kotler-dev/frap/actions

### 5. Release from Staging

1. Go to https://s01.oss.sonatype.org/
2. Log in with Sonatype credentials
3. Navigate to "Staging Repositories"
4. Find your staging repo (e.g., `iogithubkotlerdevfrap-1000`)
5. Click "Close" (waits for checks)
6. Click "Release" (publishes to Maven Central)

### 6. Verify

Wait 10-30 minutes, then verify:

```bash
# Check Maven Central
curl "https://repo1.maven.org/maven2/io/github/kotlerdev/frap/frap-core-java/1.1.0/frap-core-java-1.1.0.pom"

# Or use in test project
mvn dependency:resolve -DincludeArtifactIds=frap-core-java
```

### 7. Post-Release

```bash
# Update to next SNAPSHOT
cd sdk/java
mvn versions:set -DnewVersion=1.2.0-SNAPSHOT
mvn versions:commit

git add sdk/java/
git commit -m "Prepare for next development iteration"
git push
```

## Troubleshooting

### "401 Unauthorized" in CI

**Cause**: Wrong OSSRH credentials.

**Solution**: Verify secrets in GitHub settings.

### "No public key" in staging

**Cause**: GPG key not uploaded to keyserver.

**Solution**:
```bash
gpg --keyserver keyserver.ubuntu.com --send-keys KEY_ID
```

### Missing artifacts in staging

**Cause**: Native binaries not bundled.

**Solution**: Ensure workflow downloads artifacts:
```yaml
- uses: actions/download-artifact@v4
  with:
    name: native-binaries
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
