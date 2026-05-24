# Publishing `@fletta/*` to npm

Packages publish to **https://registry.npmjs.org** (see `.github/workflows/publish.yml`), not GitHub Packages.

For GitHub Packages (`npm.pkg.github.com`), see [Working with the npm registry (GitHub)](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-npm-registry) — that requires different `publishConfig.registry` and `.npmrc` scope mapping.

## Prerequisites

### 1. Create the `@fletta` scope on npm

Scoped names like `@fletta/sdk` require an **npm organization** (or a user account literally named `fletta`).

1. Log in at [npmjs.com](https://www.npmjs.com/) as the account that will own releases.
2. Create an org: [Create an organization](https://www.npmjs.com/org/create) → name **`fletta`** (lowercase).
3. If the name is taken, either request access from the owner or choose another scope and rename packages in `package.json` (e.g. `@kotler-dev/sdk`).

### 2. `NPM_TOKEN` in GitHub

Repository secret **`NPM_TOKEN`** must be a token that can **publish** to the `@fletta` scope:

| Token type | Notes |
|------------|--------|
| **Automation** (recommended for CI) | Created under the **fletta** org → Access Tokens. Not 2FA-expiring on publish. |
| Personal access token (classic) | User must be **owner** of the `fletta` org; enable **Bypass 2FA for automation** if 2FA blocks CI publishes. |

Required capability: publish packages under `@fletta/*`.

Public scoped packages already set `"publishConfig": { "access": "public" }` in each `package.json`.

### 3. Release tag

Publishing runs on **git tag** `v*` or manual **Publish to npm** workflow.

The tagged commit must include `.github/workflows/publish.yml` (tags on old commits without that file do not publish).

## Release checklist

```bash
# On main after merge, versions aligned in package.json:
git pull origin main
git tag -a v1.1.2 -m "Release 1.1.2"
git push origin v1.1.2
```

Or: **Actions → Publish to npm → Run workflow**.

Order in CI: `@fletta/sdk` first, then `@fletta/playwright` (depends on published SDK version).

## Verify

```bash
npm view @fletta/sdk version
npm view @fletta/playwright version
```

## Common errors

| Error | Cause | Fix |
|-------|--------|-----|
| `Scope not found` | No org/user `fletta` on npm | Create org `fletta` or change package scope |
| `402 Payment Required` | Private scoped package without paid plan | Use `"access": "public"` in `publishConfig` |
| `403 Forbidden` | Token cannot publish to scope | Org automation token or org owner + 2FA bypass |
| `409 Conflict` | Version already published | Bump `version` in `package.json`, new tag |
