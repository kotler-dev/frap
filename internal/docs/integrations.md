# Интеграция, а не замена

Frap — **слой** поверх существующих раннеров и отчётов. Не новый test framework, не замена Playwright/Selenium/JUnit.

---

## Принципы

1. **Раннер остаётся** — Playwright Test, JUnit, TestNG, Cypress (later).
2. **Primary-селектор сохраняется** — healing только при failure.
3. **Экспорт в привычные форматы** — JUnit XML, Allure, Playwright HTML report / trace.
4. **Opt-in** — одна зависимость + конфиг, без переписывания suite.

---

## Core Platform API

Адаптеры Playwright, Selenium и мобильные SDK строятся на едином Rust Core.
Core можно использовать standalone — без адаптеров — через WASM, FFI или JSON-RPC.

### Interfaces

| Интерфейс | Назначение | Пример использования |
|-----------|------------|---------------------|
| WASM | Node.js, Deno, Browser | `new HealingEngine().heal(...)` |
| FFI (C-API) | Нативные языки | Java JNI, Swift, Python ctypes |
| JSON-RPC | Внешние процессы | CLI, CI pipeline, другие языки |

### Custom adapter

Чтобы написать адаптер для нового фреймворка, нужно:
1. Получить DOM/ViewTree snapshot от фреймворка
2. Вызвать Core через один из интерфейсов
3. Обработать HealingResult, применить новый селектор

См. подробнее: [F000: Core Platform API](../project/feature/F000-core-platform-api.md)

---

## Playwright (MVP, P0)

### Adapter

```typescript
// playwright.config.ts
import { frapPlaywright } from 'frapcode-playwright';

export default defineConfig({
  ...frapPlaywright({
    minConfidence: 0.85,
    reportDir: './frap-reports',
  }),
});
```

### Использование в тесте

```typescript
// Вариант A: custom selector engine
await page.locator('frap:pay-btn').click();
// primary: data-testid=pay-btn, fallbacks: signature chain

// Вариант B: обёртка существующего локатора (миграция)
import { withFrap } from 'frapcode-playwright';
await withFrap(page.getByTestId('pay-btn')).click();
```

### Что не делаем

- Не заменяем `@playwright/test` runner.
- Не форкаем Playwright MCP — **дополняем** MCP tools `frap/heal`, `frap/analyze`.

---

## Selenium / Java (roadmap P4, зафиксировано для банка)

**Минимальная интеграция** (JUnit 5 + Selenium + Page Object, фазы 0–2, CI): **[integrations-selenium-java.md](./integrations-selenium-java.md)**.

**SDK Java / TypeScript / Python:** [sdk-strategy.md](../project/architecture/sdk-strategy.md). Фичи: [F013](../project/feature/F013-typescript-sdk.md), [F014](../project/feature/F014-java-sdk-ui-adapters.md), [F015](../project/feature/F015-python-sdk-adapters.md).

### JUnit 5

```java
@ExtendWith(FrapExtension.class)
class CheckoutTest {
  @Test
  void pay() {
    driver.findElement(By.cssSelector("[data-testid='pay-btn']")).click();
  }
}
```

### Listener / SelfHealingDriver (опции)

| Подход | Плюсы | Минусы |
|--------|-------|--------|
| JUnit Extension | Минимум изменений тестов | Только JUnit 5 |
| Wrapped WebDriver | Как Healenium-web | Больше магии в runtime |
| Selenium 4 listener | Прозрачность | Зависит от версии |

**Цель для S1 (audience):** подключение **без смены** Page Object структуры — healing на уровне findElement.

### Экспорт

- JUnit XML: `<system-out>` или custom properties с `Frap.healed`, `Frap.confidence`, `Frap.diff`
- Allure: attachment `Frap-healing.json`
- GitLab/Jenkins: junit artifact

---

## CI/CD

| Система | Интеграция |
|---------|------------|
| GitLab CI | Job template, junit artifacts, `frap-reports/` |
| Jenkins | JUnit publisher + archive artifacts |
| GitHub Actions | Action `frap/report-upload` (enterprise later) |

```yaml
# GitLab excerpt
test:e2e:
  script:
    - npx playwright test
  artifacts:
    reports:
      junit: frap-reports/junit.xml
    paths:
      - frap-reports/
```

---

## Отчёты и observability

| Система | Интеграция |
|---------|------------|
| Allure | Step «Frap-heal» + attachment |
| ReportPortal | Log healing event (API v2) |
| Playwright Trace | Link healed step to trace viewer |

---

## MCP (v2+, дополнение Playwright MCP)

| Tool | Назначение |
|------|------------|
| `frap/replay` | Прогон сценария с healing в CI-режиме |
| `frap/analyze` | RCA JSON для агента |
| `frap/export` | Обновить селектор в файле теста (PR suggestion) |
| `frap/benchmark` | Прогнать CP001–CP003 на demo app |

**Разделение с Playwright MCP:**

- Playwright MCP: navigate, click, snapshot, codegen.
- Frap MCP: **стабилизация, audit, metrics** существующих тестов.

---

## Element map / POM export

Генерация не обязательна для PoC. Когда появится (F004):

| Формат | Путь |
|--------|------|
| TypeScript | `*.page.ts` с `frap:` locators |
| Java | Page Object + `@FrapLocator` |
| Python | pytest-playwright stubs |

POM — **один из экспортов**, не суть продукта.

---

## Миграционный путь (банк, мультикоманды)

```
Неделя 1:  Playwright команда → frapcode-playwright на 1 suite
Месяц 2:   + GitLab junit + Allure
Месяц 4:   Java команда → JUnit extension pilot
Месяц 6:   AI команда → Frap MCP analyze + C007
```

---

## Anti-patterns (не делаем)

- ❌ Собственный браузерный драйвер
- ❌ Обязательная миграция всех тестов на Frap DSL
- ❌ Облачный сервис как единственный способ healing
- ❌ Замена Playwright Codegen / MCP

---

## Связанные документы

- [positioning.md](./positioning.md) — vs Playwright MCP
- [benchmark.md](./benchmark.md) — CP005 export
- [features.md](./features.md) — F008
