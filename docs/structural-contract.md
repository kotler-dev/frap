# Structural Contract: структурный контракт вместо pixel diff

Страница позиционирования: матрица доступности, anti-pitch, связь с visual regression.

> **One-liner**: Проверка «страница устроена как в требованиях» через структурные инварианты — DOM, кластеры, сигнатуры; не pixel diff, а explainable drift report.

---

## Матрица доступности

| Возможность | Статус | Где в продукте | Пример использования |
|-------------|--------|----------------|----------------------|
| **Explainable diff** при смене элемента | ✅ Ready | F001 healing, `frap-events.jsonl`, F012 debug report | В отчёте видно: «элемент смещён, top-3 кандидаты, confidence 0.87» |
| **Привязка критичных зон** (signature baseline) | ✅ Ready | `bind` / `serialize` / `locate` API | `Frap.bind('[data-testid="submit"]')` → JSON baseline |
| **CI: падать при неожиданном heal** | ✅ Ready | `healStrategy: 'fail'` в конфигурации | Структурный gate: любое отклонение = fail |
| **Полная element map + discover CLI** | ❌ Roadmap | F004 + F017.2 (v2) | `frap discover --url …` → `element-map.json` |
| **Drift report (page-level)** | ❌ v2 | F017.2 + P07 | `analyze --against` → `drift-report.json` с added/removed/moved |
| **Декларативные инварианты (YAML)** | ❌ v2+ | F017.3 | `structural-contract.yaml`: cluster_exists, element_unique |
| **Относительная геометрия** | ❌ v2 | F007 Visual Fingerprint | «Кнопка съехала на 20px» по relative bounds |
| **Screenshot / pixel regression** | **Out of scope** | Percy, Playwright `toMatchSnapshot` | Frap не сравнивает пиксели |

**Легенда**: ✅ — доступно сейчас (v1.x); ❌ — roadmap; **Out** — сознательно вне зоны Frap.

---

## Сравнение слоёв: Visual regression vs Frap

| Аспект | Visual regression (Percy, etc.) | Frap Structural Contract |
|--------|--------------------------------|--------------------------|
| **Что сравниваем** | Пиксели, скриншоты | DOM-структура, кластеры, сигнатуры |
| **Что ловим** | Цвет, шрифт, anti-aliasing, layout pixel-perfect | Пропал блок, сдвинулась форма, сломалась вложенность |
| **Типичный fail** | «Красный пиксель в углу» | «Кластер checkout-form: удалён элемент payment-method» |
| **Объяснимость** | PNG diff, визуальный осмотр | `drift-report.json` с severity, candidates, confidence |
| **CI gate** | `toMatchSnapshot()` | `assertStructuralContract(baseline, current)` |
| **Flaky при** | Анимации, шрифты ОС, скролл | Редко (структура стабильнее пикселей) |
| **On-prem** | Облачный сервис (обычно) | ✅ Локальный deterministic engine |

**Когда использовать оба**: скриншоты — «как нарисовано в Figma»; Frap — «как собрано, не сломалась ли композиция».

---

## Anti-pitch: чего Frap не обещает

Мы сознательно не закрываем эти ожидания, чтобы не разочаровать команды:

| Ожидание | Реальность |
|----------|------------|
| «Pixel-perfect как в Figma» | ❌ Нет. Структура и геометрия (bounds), не цвет и шрифт. |
| «Автотесты без кода из Confluence» | ❌ Нет. Frap даёт grounding, не генерирует сценарии из требований. |
| «Заменит Playwright / Selenium» | ❌ Нет. Integration, not replacement — слой поверх. |
| «Увидит «кривую» вёрстку на глаз» | ❌ Нет. ML-оценка «красивости» вне scope; deterministic core. |
| «Поймает баг в CSS-градиенте» | ❌ Нет. Computed styles — только стабильные (цвет фона кнопки), не визуальные эффекты. |

**Что обещаем**: при изменении DOM структуры вы получите **explainable diff** с confidence scores и severity; CI gate на структурных инвариантах; drift detection **до** массового падения тестов.

---

## Workflow: structural contract в CI

### Phase 1: Element-level (v1.2, доступно сейчас)

```typescript
// 1. Bind на стабильной версии (development)
const sig = await Frap.bind(page.locator('[data-testid="submit"]'));
await sig.serializeTo('./signatures/submit.json');

// 2. CI gate: строгий режим — любой drift = fail
test('checkout structure', async ({ page }) => {
  const sig = await Frap.deserialize('./signatures/submit.json');
  const resolved = await sig.locate(page, { 
    healStrategy: 'fail',  // ❌ не heal, а fail с diff
    confidence: 0.95       // высокий порог
  });
  expect(resolved.found).toBe(true);
  expect(resolved.healed).toBe(false); // baseline точный
});
```

### Phase 2: Page-level + Drift report (v2)

```bash
# Baseline на production
frap discover --url https://prod.example.com/checkout \
  --output baseline.json

# CI: сравнение со staging
frap analyze --url https://staging.example.com/checkout \
  --against baseline.json \
  --output drift-report.json

# Gate: критичный drift = exit 1
frap gate --report drift-report.json --max-severity warning
```

### Phase 3: Policy DSL (v2+)

```yaml
# structural-contract.yaml
scopes:
  checkout:
    selector: '[data-testid="checkout-form"]'
    invariants:
      - type: cluster_exists
        id: payment-methods
        severity: critical
      - type: element_unique
        role: submit
        severity: warning
      - type: no_orphan_elements
        selector: '[data-testid]'
        severity: info
```

---

## Связь с другими документами

- [project/feature/F017-structural-contract.md](../project/feature/F017-structural-contract.md) — полная карточка фичи с фазами F017.0–F017.4;
- [docs/pains.md](./pains.md) — P07: drift UI незаметен до массового падения;
- [docs/glossary.md](./glossary.md) — drift, element map, signature;
- [Frap.md](../Frap.md) — концепция структурной регрессии UI (terminology);
- [docs/positioning.md](./positioning.md) — общее позиционирование продукта;
- [project/cases/C010-structural-regression.md](../project/cases/C010-structural-regression.md) — сценарий checkout.

---

## Для сайта / презентаций

**Короткая формулировка (30 сек)**:

> Frap Structural Contract — CI gate для вёрстки без скриншотов. Проверяет, что страница собрана правильно: DOM-структура, кластеры компонентов, критичные зоны на месте. При изменении — explainable diff: что именно сдвинулось или пропало, с severity. Deterministic, on-prem, NO ML.

**Буллиты для слайда**:

- ✅ Структурная регрессия: element / structural / cluster drift
- ✅ Explainable: candidates, confidence scores, audit trail
- ✅ CI gate: fail on unexpected heal, policy-based thresholds
- ❌ Не pixel diff — структура, не цвет
- ❌ Не замена Playwright — слой поверх
- 🔮 Page-level drift report (v2), Policy DSL (v2+)
