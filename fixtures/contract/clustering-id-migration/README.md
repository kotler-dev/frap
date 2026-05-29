# Contract: clustering id → data-id migration

Language-agnostic fixture for the scenario:

- **Before:** `ul > li#1`, `ul > li#2` (represented as `li[id="1"]`, `li[id="2"]`)
- **After:** `ul > li#1`, `ul > li[data-id="2"]`

## Files

| File | Role |
|------|------|
| `page-after.html` | DOM after migration (for Playwright `setContent`) |
| `request.json` | `HealRequest` input for core RPC / WASM |
| `expected.json` | Normalized contract assertions (not exact selector strings) |

## Runners

- **Rust:** `crates/core/tests/contract_clustering_id_migration.rs`
- **Java:** `sdk/java/frap-core-java/src/test/java/.../ClusteringIdMigrationContractTest.java`
- **TypeScript:** `sdk/typescript/src/contract/clustering-id-migration.test.ts`
- **Playwright e2e:** `internal/testing/conference/clustering-id-migration.spec.ts`
