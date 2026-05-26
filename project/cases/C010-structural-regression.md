# Case: Structural Regression — Checkout Block (C010)

## Meta

- **ID**: C010
- **Название**: Structural Regression — Checkout Block
- **Фичи**: F017 (Structural Contract)
- **Статус**: concept → F017.0 (документация), validated → F017.1/2 (implementation)
- **Created**: 2026-05-27

## Сценарий

Проверка структуры checkout-формы без screenshot тестирования. Идеальный сценарий для демонстрации structural contract: форма оплаты — критичный бизнес-путь, где «пропал блок CVV» или «кнопка Submit сдвинулась в другой контейнер» — это структурный баг, который надо ловить до прогона functional suite.

## Phase 1: Element-level Contract (F017.1, доступно v1.2)

### Setup

```typescript
// checkout.spec.ts — structural gate (отдельный job от functional tests)
import { test, expect } from '@playwright/test';
import { Frap } from '@frap/frap';

test.describe('Checkout structural contract', () => {
  // Baseline signatures — сгенерированы на stable версии
  const signatures = [
    './signatures/checkout-form.json',
    './signatures/payment-methods.json',
    './signatures/submit-button.json',
  ];

  test('critical zones are structurally stable', async ({ page }) => {
    await page.goto('/checkout');
    
    for (const sigPath of signatures) {
      const sig = await Frap.deserialize(sigPath);
      const resolved = await sig.locate(page, {
        healStrategy: 'fail',  // ❌ не heal — fail с diff
        confidence: 0.95
      });
      
      // Assertion: элемент найден ТОЧНО, без drift
      expect(resolved.found).toBe(true);
      expect(resolved.healed).toBe(false);
      
      // Если drift — отчёт содержит кандидатов и diff
      if (resolved.drift) {
        console.log('Drift detected:', JSON.stringify(resolved.drift, null, 2));
      }
    }
  });
});
```

### Intentional Drift (для demo)

```html
<!-- checkout-v1.html (baseline) -->
<form data-testid="checkout-form">
  <div data-testid="payment-methods">
    <input type="radio" name="method" value="card" />
    <input type="radio" name="method" value="paypal" />
  </div>
  <button data-testid="submit" type="submit">Pay</button>
</form>

<!-- checkout-v2.html (drift) — intentional breaking change для demo -->
<form data-testid="checkout-form">
  <!-- payment-methods renamed → breaking signature -->
  <div data-testid="payment-options">
    <input type="radio" name="method" value="card" />
  </div>
  <!-- submit moved inside accordion — structural shift -->
  <details>
    <summary>Confirm</summary>
    <button data-testid="submit" type="submit">Pay</button>
  </details>
</form>
```

### Ожидаемый результат

```json
// Console / frap-events.jsonl
{
  "event": "structural_drift_detected",
  "element": "payment-methods",
  "originalSignature": { "anchor": "[data-testid='payment-methods']" },
  "drift": {
    "type": "element_drift",
    "anchor_changed": true,
    "candidates": [
      { "selector": "[data-testid='payment-options']", "confidence": 0.72 }
    ]
  },
  "resolution": "failed_heal_strategy",
  "severity": "critical"
}
```

## Phase 2: Page-level + Drift Report (F017.2, v2)

### Full Page Contract

```bash
# Baseline на production
frap discover --url https://shop.example.com/checkout \
  --scope interactive \
  --output checkout-baseline.json

# CI на staging: detect drift
frap analyze --url https://staging.example.com/checkout \
  --against checkout-baseline.json \
  --output drift-report.json

# Gate: critical drift = exit 1
frap gate --report drift-report.json \
  --max-severity warning \
  --whitelist ./expected-changes.yaml
```

### Drift Report Schema

```json
{
  "version": "1.0",
  "url": "https://staging.example.com/checkout",
  "baseline_url": "https://shop.example.com/checkout",
  "timestamp": "2026-05-27T12:00:00Z",
  "summary": {
    "total_elements": 47,
    "added": 1,
    "removed": 1,
    "moved": 1,
    "changed": 2
  },
  "drifts": [
    {
      "id": "payment-methods",
      "type": "element_drift",
      "severity": "critical",
      "original": { "testid": "payment-methods" },
      "current": { "testid": "payment-options" },
      "confidence": 0.72,
      "message": "payment-methods not found; closest candidate: payment-options (confidence 0.72)"
    },
    {
      "id": "submit-button",
      "type": "structural_drift",
      "severity": "warning",
      "original": { "parent": "checkout-form", "depth": 1 },
      "current": { "parent": "details", "depth": 2 },
      "message": "submit-button moved deeper in hierarchy"
    }
  ]
}
```

### Verification Script (verify-drift.mjs)

```javascript
// e2e/structural/verify-drift.mjs — gate script по аналогии с verify-rca.mjs
import { readFileSync } from 'fs';

const report = JSON.parse(readFileSync(process.argv[2], 'utf8'));
const maxSeverity = process.argv[3] || 'warning';

const severityOrder = { info: 0, warning: 1, critical: 2 };
const maxLevel = severityOrder[maxSeverity];

let hasCritical = false;
for (const drift of report.drifts) {
  if (severityOrder[drift.severity] > maxLevel) {
    console.error(`❌ ${drift.severity}: ${drift.message}`);
    hasCritical = true;
  }
}

if (hasCritical) {
  console.error('\nStructural contract VIOLATED');
  process.exit(1);
} else {
  console.log('✅ Structural contract OK');
  process.exit(0);
}
```

## Phase 3: Policy DSL (F017.3, v2+)

```yaml
# checkout-contract.yaml
scopes:
  checkout:
    selector: '[data-testid="checkout-form"]'
    invariants:
      # Кластер payment-methods должен существовать
      - type: cluster_exists
        id: payment-methods
        severity: critical
        
      # Кнопка submit должна быть единственная в форме
      - type: element_unique
        role: submit
        scope: checkout
        severity: warning
        
      # Все интерактивные элементы должны быть привязаны
      - type: no_orphan_elements
        selector: '[data-testid]'
        severity: info

# CI: только ожидаемые изменения по тикету
whitelist:
  - ticket: PROJ-1234
    change: payment-methods renamed to payment-options
    severity_override: info
```

## Acceptance Criteria (для C006)

### Phase 1 (F017.1) — ready for implementation

- [ ] Тестовая страница `test-app/structural/checkout-v1.html` (baseline)
- [ ] Тестовая страница `test-app/structural/checkout-v2.html` (drift)
- [ ] Playwright spec `e2e/structural/checkout-element-gate.spec.ts`
- [ ] При drift с `healStrategy: 'fail'` — тест падает с explainable diff
- [ ] `frap-events.jsonl` содержит drift classification
- [ ] README в `e2e/structural/` с инструкцией запуска

### Phase 2 (F017.2) — roadmap v2

- [ ] `discover` API возвращает full element map для checkout
- [ ] `analyze --against` генерирует `drift-report.json`
- [ ] Gate script `verify-drift.mjs` проходит на v1, падает на v2
- [ ] Отчёт содержит: added/removed/moved, cluster changes

### Phase 3 (F017.3) — roadmap v2+

- [ ] Парсинг `checkout-contract.yaml`
- [ ] Policy checker валидирует invariants
- [ ] Whitelist по тикету работает в CI

## Связанные документы

- [F017: Structural Contract](../feature/F017-structural-contract.md) — полная карточка фичи
- [docs/structural-contract.md](../../docs/structural-contract.md) — матрица доступности
- [project/cases/README.md](./README.md) — каталог кейсов
- [F001: Self-Healing](../feature/F001-self-healing.md) — основа для element-level contract
- [F004: Page Object Gen](../feature/F004-page-object-gen.md) — shared discover pipeline
