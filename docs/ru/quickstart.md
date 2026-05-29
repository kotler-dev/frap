# Быстрый старт

Установка Frap для Playwright:

```bash
npm install @frap/sdk @frap/playwright
```

## Настройка Playwright

```typescript
// playwright.config.ts
import { defineConfig } from '@playwright/test';
import { frapPlaywright, registerFrapSelector } from '@frap/playwright';

export default defineConfig({
  ...frapPlaywright({
    minConfidence: 0.85,
    reportDir: './frap-reports',
  }),
  use: {
    async setup({ selectors }) {
      await registerFrapSelector(selectors);
    },
  },
});
```

## Использование в тестах

```typescript
// test.spec.ts
import { test, expect } from '@playwright/test';

test('payment flow', async ({ page }) => {
  // Селектор с самовосстановлением через префикс frap:
  await page.locator('frap:[data-testid="pay-btn"]').click();
});
```

При изменении DOM Frap находит элемент по его структурной сигнатуре.

## Отчёты

После прогона тестов в `frap-reports/`:

- `frap-report.json` — события healing и результаты
- `junit.xml` — формат JUnit с аннотациями Frap

## Дополнительно

- [Интеграции](integrations.md) — полная справка по настройке
- [README адаптера](../../adapters/playwright/README.md) — детальное API
