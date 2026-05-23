# Feature: Playwright Adapter (F008)

## Meta

- **Epic**: Integration → Playwright
- **Roll-up target**: ## MVP v1.0.0
- **Status**: in-progress
- **Target release**: v1.0.0
- **Created**: 2026-05-20
- **Related cases**: C001, C004, CP001–CP005

## Goal

Интеграция fletta с Playwright через custom selectors API. Не заменяем Playwright — дополняем при failure.

## User workflow

1. Пользователь устанавливает `@fletta/playwright`
2. Добавляет fletta в playwright.config.ts
3. В тесте использует `fletta:` prefix или `withFletta()` обёртку
4. При изменении UI fletta активирует self-healing
5. Результат: отчёт в JUnit/JSON формате

## Scope

### In
- Playwright custom selector engine `fletta:`
- Обертка `withFletta()` для существующих локаторов
- Конфигурация: minConfidence, reportDir, policy
- Экспорт отчётов: JUnit XML, JSON
- Интеграция с Playwright trace viewer

### Out
- Замена Playwright runner
- Собственный браузерный драйвер
- Playwright MCP (это отдельный проект)

## Acceptance criteria

- [x] Установка: `npm install @fletta/playwright` работает (local)
- [x] Конфигурация через playwright.config.ts
- [x] Custom selector: `page.locator('fletta:pay-btn')` работает
- [x] Wrapper: `withFletta(page.getByTestId('pay-btn'))` работает
- [x] CP001 проходит: stable тест без healing
- [x] CP002 проходит: heal при смене testid
- [ ] CP005 проходит: JUnit XML артефакт в CI
- [x] Документация: quick start < 15 минут

### Implementation Status
| Component | Status | Location |
|-----------|--------|----------|
| Package structure | ✅ | `adapters/playwright/` |
| Wrapper API | ✅ | `adapters/playwright/src/wrapper.ts` |
| Selector engine | ✅ | `adapters/playwright/src/selector-engine.ts` |
| Reporter (JUnit/JSON) | ✅ | `adapters/playwright/src/reporter.ts` |
| Config integration | ✅ | `adapters/playwright/src/index.ts` |
| README | ✅ | `adapters/playwright/README.md` |

### Usage Example
```typescript
import { withFletta } from '@fletta/playwright';

test('payment', async ({ page }) => {
  const button = await withFletta(page.getByTestId('pay-btn'), page);
  await button.click();
});
```

## Implementation notes (sketch)

### Структура пакета
```
adapters/playwright/
├── src/
│   ├── index.ts           # основной экспорт
│   ├── selector-engine.ts # регистрация custom selector
│   ├── wrapper.ts         # withFletta() обёртка
│   ├── config.ts          # типы конфигурации
│   └── reporter.ts        # JUnit/JSON export
├── package.json
└── README.md
```

### Custom selector engine
```typescript
// playwright.config.ts
import { flettaPlaywright } from '@fletta/playwright';

export default defineConfig({
  ...flettaPlaywright({
    minConfidence: 0.85,
    reportDir: './fletta-reports',
  }),
});
```

### Использование в тесте
```typescript
// Вариант A: custom selector
test('payment', async ({ page }) => {
  await page.locator('fletta:pay-btn').click();
});

// Вариант B: обёртка существующего
test('payment', async ({ page }) => {
  await withFletta(page.getByTestId('pay-btn')).click();
});
```

### Взаимодействие с Rust core
- TypeScript SDK вызывает WASM-модуль для сравнения сигнатур
- Асинхронный API: `heal(primarySelector, domSnapshot) -> HealingResult`

### Зависимость от Core
Адаптер строится поверх [F013: TypeScript SDK](./F013-typescript-sdk.md) и F000 Core Platform API (WASM).
Playwright adapter — тонкая обёртка: DOM snapshot → SDK/Core.heal() → result.

См. [F000: Core Platform API](./F000-core-platform-api.md), [F013](./F013-typescript-sdk.md)

### Риски и зависимости
- Зависит от F001 (Self-Healing core)
- Playwright API stability (custom selectors — stable)
- WASM производительность в Node.js

## Verification / Test plan

### Manual smoke
```bash
# Установка и настройка
cd demo-app
npm install @fletta/playwright

# Конфигурация
# playwright.config.ts — добавить flettaPlaywright()

# Запуск тестов
npx playwright test

# Проверка отчёта
ls fletta-reports/
# Expected: junit.xml, healing-report.json
```

### Automation
- Интеграционные тесты: CP001–CP005
- E2E тесты: реальный Playwright + fletta
- CI pipeline: GitHub Actions

## Related docs

- [integrations.md](../../docs/integrations.md) — полная картина интеграций
- [positioning.md](../../docs/positioning.md) — vs Playwright MCP
- [benchmark.md](../../docs/benchmark.md) — CP001–CP005 gates
