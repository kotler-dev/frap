# Интеграции

## Playwright

### Установка

```bash
npm install @frap/frap @frap/frap-playwright
```

### Кастомный движок селекторов

Рекомендуется для новых проектов:

```typescript
import { frapPlaywright, registerFrapSelector } from '@frap/frap-playwright';

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
import { withFrap } from '@frap/frap-playwright';

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

- **Java** — в планах (Selenium, Selenide)
- **Python** — в планах

Полная схема имён артефактов — в [Frap.md](../../Frap.md).
