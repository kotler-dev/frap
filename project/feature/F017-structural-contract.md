# Feature: Structural Contract (F017)

## Meta

- **Epic (в терминах FEATURES.md)**: Feature → Observability / Quality
- **Roll-up target**: ## v2.0.0 (Scale)
- **Status**: draft
- **Target release**: v2.0.0 (полный контур); частичная ценность — с v1.2.0 (документация + element-level contract на базе F001)
- **Created**: 2026-05-27
- **Related cases**: C010 (structural-regression)

## Goal

Проверка «страница/блок устроены как в требованиях» через структурные инварианты и drift-отчёт. Не замена Playwright и не screenshot testing — а explicit CI gate с explainable diff: что именно изменилось в DOM-структуре, кластерах или геометрии, с severity и рекомендациями.

Концепция объединяет:
- **Baseline**: element map или signature-набор критичных зон;
- **Policy**: декларативные инварианты (кластер "checkout-form" должен существовать, кнопка "submit" — единственная в поддереве);
- **Drift engine**: сравнение текущего UI с baseline;
- **Report**: `drift-report.json` + human-readable diff для CI.

## User workflow

1. **Зафиксировать baseline** — `bind` на критичные зоны (сейчас) или `discover` полной страницы (v2);
2. **Описать инварианты** — YAML policy или чеклист в тикете (v2+), пока — кодовые assertions на `confidence` и `healStrategy: fail`;
3. **В CI: capture → compare** — structural job отдельно от functional suite;
4. **Assert по severity / whitelist** — ожидаемые изменения помечены в тикете;
5. **При fail — explainable diff** — element drift, structural drift, cluster drift из glossary.

## Scope

### In

- Element map baseline и drift types (element / structural / cluster) — см. [glossary](../../docs/glossary.md);
- Policy: scopes (зоны страницы), thresholds confidence, expected-change whitelist;
- CI gate + JUnit/JSON reporter hook;
- Интеграция Playwright: structural job отдельно от functional suite;
- Опционально: относительная геометрия через F007 (roadmap).

### Out

- Pixel / visual regression (Percy, `toMatchSnapshot`) — **вне scope Frap**;
- Полная генерация e2e «из Confluence» без участия команды — Frap даёт grounding, не orchestrator;
- Figma 1:1 pixel-perfect matching — без bounds/geometry;
- Замена Playwright/Selenium — integration, not replacement;
- ML-оценка «красивости» — deterministic core, NO ML.

## Subtasks

| ID | Содержание | Release | Зависимости | Статус |
|----|------------|---------|-------------|--------|
| F017.0 | Документация + матрица доступности + кейс C006 concept | v1.2.0 docs | — | 🔄 |
| F017.1 | **Element-level contract**: bind baseline signatures, CI `locate` + `confidence`, fail on heal (`healStrategy: fail`), diff в отчётах | v1.2.0 / v1.4.0 | F001, F008, F012 | ❌ |
| F017.2 | **Page-level contract**: `discover` → element map; `analyze --against`; `drift-report.json` | v2.0.0 | F004 pipeline (shared), core | ❌ |
| F017.3 | **Policy DSL**: `structural-contract.yaml`, scopes, invariants (cluster count, must-exist) | v2.0.0+ | F017.2 | ❌ |
| F017.4 | Geometry invariants (relative bounds) | v2.0.0 | F007 | ❌ |

## Acceptance criteria

### F017.0 (docs)
- [x] Карточка фичи заполнена (этот файл);
- [x] Матрица доступности в `docs/structural-contract.md`;
- [x] Кейс C006 описан в `project/cases/`;
- [x] Cross-links: pains.md P07, glossary, positioning.md, Frap.md.

### F017.1 (element-level)
- [ ] Playwright adapter поддерживает `structuralContract: { signaturesDir, failOnHeal: true }`;
- [ ] При intentional drift селектор не находится (heal не срабатывает или rejected по policy) — CI падает с explainable diff;
- [ ] `frap-events.jsonl` содержит drift classification;
- [ ] E2E сценарий на test-app без screenshot assertions.

### F017.2 (page-level)
- [ ] `discover` API возвращает element map с drift score;
- [ ] `analyze --against` генерирует `drift-report.json`;
- [ ] Report содержит: added/removed/moved elements, cluster changes, confidence drift;
- [ ] Gate script `verify-drift.mjs` как аналог `verify-rca.mjs`.

### F017.3 (policy DSL)
- [ ] Schema `structural-contract.yaml`:
  ```yaml
  scopes:
    checkout:
      selector: '[data-testid="checkout-form"]'
      invariants:
        - type: cluster_exists
          id: payment-methods
        - type: element_unique
          role: submit
  ```
- [ ] Policy checker в core реализует `approve_resolution` / `reject`;
- [ ] Whitelist: expected changes по тикету.

### F017.4 (geometry)
- [ ] Visual signature с bounds (от F007) участвует в drift;
- [ ] Отчёт: "button moved 20px right" с relative coordinates.

## Implementation notes (sketch)

### Modules (target)

```
crates/contract/          # v2: drift, policy, report schema
  src/drift.rs            # Diff algorithm: element map vs baseline
  src/policy.rs           # PolicyChecker, whitelist, scopes
  src/report.rs           # DriftReport JSON schema

adapters/playwright/      # SDK enhancement
  src/structural-gate.ts  # Reporter flag, assertions

sdk/typescript/
  src/discover.ts         # discover(), analyzeAgainst()
  src/contract.ts         # assertStructuralContract()

e2e/structural/           # C006 demo
  structural-gate.spec.ts
  verify-drift.mjs        # Gate script по аналогии с context/
```

### Shared pipeline with F004

Один element map format, два потребителя:
- **F004** — генерация PageObject / тестов (generate);
- **F017** — валидация структуры (validate).

Не дублировать discover logic — обе фичи используют `crates/discover/` (shared module, v2).

### Hooks в существующей архитектуре

- `DriftEvent` и `PolicyChecker` trait — уже заготовки в [platform-agnostic-core.md](../../project/architecture/platform-agnostic-core.md);
- Расширить `AuditLogger` для structural drift events;
- `ResolutionAttempt` → добавить `drift_classification`.

### Risks & mitigation

| Risk | Mitigation |
|------|------------|
| Дублирование с F004 | Shared `discover` pipeline; F017 = validate downstream |
| CLI (`frap discover`, `frap analyze`) не реализован | В матрице пометить «documented API / not shipped»; подзадача F000 JSON-RPC |
| Ожидание pixel-perfect | Anti-pitch в `docs/structural-contract.md` |
| Два источника truth (Frap.md vs F017) | Canonical matrix в `docs/structural-contract.md` |

## Verification / Test plan

### Manual smoke (F017.1)

```bash
# 1. Bind signatures на стабильной версии
npx playwright test structural-bind.spec.ts
# → generates signatures/*.json

# 2. Introduce intentional DOM change (test-app)
# edit test-app/checkout.html: remove data-testid

# 3. Run structural gate
npx playwright test structural-gate.spec.ts --reporter=list
# Expected: FAIL with explainable diff in console + frap-events.jsonl

# 4. View diff
jq '.driftEvents[]' frap-events.jsonl | head -20
# Expected: element_drift, candidate list, confidence scores
```

### Manual smoke (F017.2, v2)

```bash
# Page-level drift
frap discover --url https://staging.example.com --baseline element-map-prod.json
# Output: drift-report.json

# Gate
node e2e/structural/verify-drift.mjs --report drift-report.json --max-severity warning
# Expected: exit 0 (warning) or 1 (critical drift)
```

### Automation

- Unit: `drift.rs` — diff algorithm tests;
- Integration: Playwright structural gate на test-app с intentional changes;
- E2E gate: `verify-drift.mjs` schema validation (CP006 equivalent).

## Related docs

- [docs/structural-contract.md](../../docs/structural-contract.md) — матрица доступности, anti-pitch;
- [docs/glossary.md](../../docs/glossary.md) — drift, element map, signature;
- [docs/pains.md](../../docs/pains.md) — P07 drift UI незаметен до массового падения;
- [project/cases/C006-structural-regression.md](../cases/C006-structural-regression.md) — сценарий checkout;
- [Frap.md](../../Frap.md) — структурная регрессия UI (базовая концепция);
- [F004-page-object-gen.md](./F004-page-object-gen.md) — shared discover pipeline;
- [F007-visual-fingerprint.md](./F007-visual-fingerprint.md) — geometry baseline;
- [project/architecture/platform-agnostic-core.md](../architecture/platform-agnostic-core.md) — PolicyChecker, DriftEvent hooks.
