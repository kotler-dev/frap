# Интеграции

## Playwright

### Установка

```bash
npm install @frap/sdk @frap/playwright
```

### Кастомный движок селекторов

Рекомендуется для новых проектов:

```typescript
import { frapPlaywright, registerFrapSelector } from '@frap/playwright';

export default defineConfig({
  ...frapPlaywright({
    minConfidence: 0.85,    // Порог уверенности для healing
    reportDir: './frap-reports',
    enableHealing: true,
    enableReporting: true,
  }),
  use: {
    async setup({ selectors }) {
      await registerFrapSelector(selectors);
    },
  },
});
```

Используйте селекторы с префиксом `frap:`:

```typescript
await page.locator('frap:[data-testid="submit"]').click();
```

### Wrapper API

Для существующих тестов — обёртка локаторов:

```typescript
import { withFrap } from '@frap/playwright';

const button = await withFrap(page.getByTestId('submit'), page);
await button.click();
```

## Параметры конфигурации

| Параметр | Тип | По умолчанию | Описание |
|----------|-----|--------------|----------|
| `minConfidence` | number | 0.85 | Минимальный score для healing |
| `reportDir` | string | './frap-reports' | Директория для отчётов |
| `enableHealing` | boolean | true | Включить самовосстановление |
| `enableReporting` | boolean | true | Генерация отчётов |

## Другие платформы

- **Java / Playwright** — PoC готов (`frap-playwright`, demo E2E: `./scripts/run-java-e2e.sh`)
- **Java / Selenium** — в планах (WebDriver, Selenide) — [F014](../project/feature/F014-java-sdk-ui-adapters.md)
- **Python** — в планах

Полная схема имён артефактов — в [Frap.md](../../Frap.md).
