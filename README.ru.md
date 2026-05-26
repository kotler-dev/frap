# Frap

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

> Детерминированная привязка селекторов к структуре DOM. Самовосстановление при изменениях, готовность к AI.

**Frap** парсит деревья элементов (DOM, accessibility), кластеризует компоненты детерминированными алгоритмами и генерирует стабильные локаторы для тестов. При изменении UI селектор «лечится» по структурной сигнатуре — с confidence score и отчётами.

## Без ML в ядре, готов к AI

Три слоя:

| Слой | Суть | ML в Frap? |
|------|------|------------|
| **Core** (OSS) | Сигнатуры, кластеризация Drain3, healing | **Нет** — одинаковый input → одинаковый output |
| **Integration** | MCP tools: `discover`, `resolve` | **Нет** — JSON-RPC для агентов |
| **Enhancements** (опционально) | Семантические имена, генерация шагов | **Опционально** — отдельный пакет |

**Frap — инфраструктурный слой** для AI-агентов и тестов.

---

## Быстрый старт (Playwright)

```bash
npm install @frap/frap @frap/frap-playwright
```

```typescript
// playwright.config.ts
import { frapPlaywright, registerFrapSelector } from '@frap/frap-playwright';

export default defineConfig({
  ...frapPlaywright({ minConfidence: 0.85 }),
  use: {
    async setup({ selectors }) {
      await registerFrapSelector(selectors);
    },
  },
});
```

```typescript
// test.spec.ts
await page.locator('frap:[data-testid="submit"]').click();
```

Подробнее: [docs/ru/quickstart.md](docs/ru/quickstart.md)

---

## Как это работает

1. Пробуем основной селектор
2. Если не найден — извлекаем структурную сигнатуру
3. Ищем похожие элементы кластеризацией (Drain3)
4. Считаем confidence score для кандидатов
5. Если лучший >= порога — элемент «лечится»
6. В отчёте: исходный селектор, новый, confidence

---

## Документация

| Язык | Быстрый старт | Интеграции | Дизайн |
|------|---------------|------------|--------|
| **English** | [docs/en/quickstart.md](docs/en/quickstart.md) | [docs/en/integrations.md](docs/en/integrations.md) | [Frap.en.md](Frap.en.md) |
| **Русский** | [docs/ru/quickstart.md](docs/ru/quickstart.md) | [docs/ru/integrations.md](docs/ru/integrations.md) | [Frap.md](Frap.md) |

API адаптера: [adapters/playwright/README.md](adapters/playwright/README.md)

---

## Структура проекта

```
frap/
├── crates/              # Rust ядро (сигнатуры, кластеризация, healing)
├── sdk/typescript/      # TypeScript SDK + WASM bindings
├── adapters/playwright/ # Интеграция с Playwright
├── test-app/            # Демо-страницы
├── e2e/                 # End-to-end тесты
└── docs/                # Документация (en/ru)
```

---

## Roadmap

- **v0.1.0** — TypeScript SDK + Playwright адаптер
- **v0.2.0** — MCP интеграция + генератор Page Object
- **v0.4.0** — Java SDK (Selenium/Selenide)
- **v1.0.0** — Мультиплатформа (Android/iOS)

См. [CHANGELOG.md](CHANGELOG.md).

---

## Лицензия

Apache-2.0 — см. [LICENSE](LICENSE) и [NOTICE](NOTICE).
