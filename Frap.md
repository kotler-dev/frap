# Frap — проектная документация

> Детерминированная привязка селекторов к структуре DOM.
> *«Когда DOM штормит — Frap держит селекторы в узде».*

> **Другой слой, не скриншоты:** структурная и семантическая регрессия UI — «страница/блок устроены так, как в требованиях», с объяснимым diff. Visual regression (pixel diff) остаётся отдельным инструментом; Frap даёт контракт на структуре DOM, element map и drift-отчётах.

**Языки:** [English](Frap.en.md) · [Русский](Frap.md)

---

## Название

**Frap** — от морского термина "to frap": туго обтягивать и связывать тросы вместе.

**CLI / селектор:** `frap`, `frap:[data-testid="…"]`

**Сайт:** https://github.com/kotler-dev/frap (основная точка входа)

**Метафора:** Ваш инструмент "связывает" селекторы со структурой DOM, чтобы они не "болтались" при изменениях.

### Шпаргалка артефактов

```
Бренд / CLI:     frap
Сайт:            https://github.com/kotler-dev/frap

npm:             @frap/frap, @frap/frap-playwright
Maven:           io.github.kotlerdev:frap-core, :frap-selenide, …
PyPI:            frap-sdk, frap-playwright
Cargo:           frap-sdk (+ frap-context, frap-rca)
GitHub:          kotler-dev/frap
```

| Реестр | SDK / ядро | Playwright | Selenide / Selenium |
|--------|------------|------------|---------------------|
| **npm** | `@frap/frap` | `@frap/frap-playwright` | — (позже) |
| **Maven** | `io.github.kotlerdev:frap-core` | `…:frap-playwright` | `…:frap-selenide`, `…:frap-selenium` |
| **PyPI** | `frap-sdk` | `frap-playwright` | — |
| **Cargo** | `frap-sdk` | — | — |

`groupId` в Maven: **`io.github.kotlerdev`** (аккаунт GitHub `kotler-dev` → в Java без дефиса).  
Команда `npm i frap` без scope ведёт на чужой пакет — ставьте **`@frap/…`**.  
На PyPI и crates.io имя **`frap`** занято — ядро публикуется как **`frap-sdk`**.

### Сводка (одна строка на язык)

**TypeScript**

```bash
npm i @frap/frap @frap/frap-playwright
```

```
frap bind --selector '[data-testid="submit"]' --output submit.signature.json
import { frap } from '@frap/frap'
page.locator('frap:[data-testid="submit"]').click()
```

**Java (Selenide)**

```xml
<dependency>
  <groupId>io.github.kotlerdev</groupId>
  <artifactId>frap-selenide</artifactId>
</dependency>
```

```
frap bind --selector "[data-testid='submit']" --output submit.signature.json
import io.github.kotlerdev.frap.selenide.Frap;
Frap.bind($("[data-testid='submit']"));
```

**Python**

```bash
pip install frap-sdk frap-playwright
```

```
frap bind --selector '[data-testid="submit"]' --output submit.signature.json
from frap import Frap
await Frap.bind(page.locator('[data-testid="submit"]'))
```

**Rust**

```bash
cargo add frap-sdk
```

---

## Унифицированный словарь команд

### Концепция: единый DSL для всех адаптеров

Каждый адаптер (Selenide, Selenium, Playwright) реализует **один и тот же набор** команд с одинаковой семантикой.

| Команда | Семантика | Selenide | Selenium | Playwright |
|---------|-----------|----------|----------|------------|
| **bind** | Привязать селектор → извлечь сигнатуру | `Frap.bind($("#btn"))` | `Frap.bind(driver.findElement(By.id("btn")))` | `Frap.bind(page.locator("#btn"))` |
| **getSignature** | Получить сигнатуру из привязки | `.getSignature()` | `.getSignature()` | `.getSignature()` |
| **locate** | Разрешить сигнатуру в текущем DOM | `sig.locate()` | `sig.locate(driver)` | `sig.locate(page)` |
| **rebind** | Обновить привязку (повторное сканирование DOM) | `.rebind()` | `.rebind(driver)` | `.rebind(page)` |
| **unbound** | Найти непривязанные элементы | `Frap.unbound($(".dynamic"))` | `Frap.unbound(driver, By.css(".dynamic"))` | `Frap.unbound(page, ".dynamic")` |
| **serialize** | Сериализовать сигнатуру в JSON | `.serialize()` / `.serializeTo(file)` | `.serialize()` / `.serializeTo(file)` | `.serialize()` / `.serializeTo(file)` |
| **deserialize** | Десериализовать сигнатуру | `Frap.deserialize(json)` | `Frap.deserialize(json)` | `Frap.deserialize(json)` |

### Конфигурация (ключи)

| Ключ | Тип | По умолчанию | Описание |
|------|-----|--------------|----------|
| `confidence` | `float` | `0.85` | Минимальная оценка для самовосстановления (healing) |
| `maxCandidates` | `int` | `5` | Максимум кандидатов в отчёте |
| `useVisual` | `boolean` | `false` | Учитывать визуальные признаки |
| `debug` | `boolean` | `false` | Подробный отладочный вывод |
| `healStrategy` | `string` | `"auto"` | Стратегия healing: `auto`, `fail`, `report` |

### Рекомендуемые настройки по сценариям

| Параметр | Разработка | CI (строго) | CI (мягко) | Отладка |
|----------|------------|-----------|---------------|---------|
| `confidence` | 0.85 | 0.95 | 0.80 | 0.70 |
| `maxCandidates` | 5 | 3 | 10 | 10 |
| `healStrategy` | `auto` | `fail` | `report` | `auto` |
| `debug` | `false` | `false` | `true` | `true` |

### Пример конфигурации

```java
// Глобальная конфигурация
Frap.config()
    .setConfidence(0.90)
    .setMaxCandidates(3)
    .setHealStrategy(HealStrategy.FAIL)
    .setDebug(true);

// Переопределение настроек для одного вызова
Resolution res = sig.locate(driver, Frap.config()
    .setConfidence(0.95));
```

---

## Примеры кода

### Java (Selenium)

```java
import io.github.kotlerdev.frap.selenium.Frap;
import io.github.kotlerdev.frap.core.Binding;
import io.github.kotlerdev.frap.core.Signature;
import io.github.kotlerdev.frap.core.Resolution;

// Привязка — извлекает сигнатуру
Binding binding = Frap.bind(driver.findElement(By.cssSelector("[data-testid='submit']")));
Signature signature = binding.getSignature();

// Сохранить сигнатуру для версионирования
String json = signature.serialize();  // или serializeTo("submit.signature.json")

// Позже — разрешение (самовосстановление при необходимости)
Signature loaded = Frap.deserialize(json);
Resolution res = loaded.locate(driver);

if (res.isHealed()) {
    System.out.println("Селектор восстановлен: " + res.getHealedSelector());
    System.out.println("Уверенность: " + res.getConfidence());
}

WebElement element = res.getElement();

// Обновить привязку после изменений DOM
binding.rebind(driver);
signature.serializeTo("submit-v2.signature.json");
```

### Java (Playwright)

```java
import io.frap.playwright.Frap;
import io.frap.playwright.config.WithFrapOptions;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.extension.ExtendWith;
import io.frap.playwright.extension.FrapExtension;

@ExtendWith(FrapExtension.class)
class MyTest {
    Page page;

    @Test
    void testWithSelfHealing() {
        // Wrap locator with Frap - automatic healing on selector failure
        FrapLocator button = Frap.withFrap(
            page.locator("[data-testid='submit']"),
            page
        );

        // Click with automatic healing if element moved/changed
        button.click();

        // Check healing result
        if (Frap.isHealed(button)) {
            HealResult result = Frap.getLastHealResult(button);
            System.out.println("Healed to: " + result.selector());
            System.out.println("Confidence: " + result.confidence());
        }
    }

    @Test
    void testWithOptions() {
        var options = new WithFrapOptions()
            .minConfidence(0.90)
            .captureAll(true)   // Enable context capture for RCA
            .debug(true);

        FrapLocator button = Frap.withFrap(
            page.locator("[data-testid='submit']"),
            page,
            options
        );

        button.click();
    }
}
```

**Maven dependency:**
```xml
<dependency>
    <groupId>io.frap</groupId>
    <artifactId>frap-playwright</artifactId>
    <version>1.1.1-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

### Kotlin (Selenide-стиль)

```kotlin
import io.github.kotlerdev.frap.selenide.frap

// Расширение для Selenide
val element = $("[data-testid='submit']")
    .frap()                  // привязать
    .getSignature()          // получить сигнатуру
    .locate()                // разрешить (самовосстановление при необходимости)

// Или явно:
val signature = $("#username").frap().getSignature()

// Сериализация
signature.serializeTo("signatures/username.json")

// Загрузка и разрешение
val loaded = Frap.deserialize(File("signatures/username.json"))
val resolved = loaded.locate(driver)

// Отладка
println(resolved.debug.candidates)  // почему выбран этот элемент
println(resolved.debug.diff)        // что изменилось в DOM

// Обновить привязку
$("#username").frap().rebind()
```

### TypeScript (Playwright)

```typescript
import { frap } from '@frap/frap';

// Привязка
const binding = await frap.bind(page.locator('[data-testid="submit"]'));
const signature = binding.getSignature();

// Сериализация для CI
await signature.serializeTo('signatures/submit.json');

// Разрешение с самовосстановлением
const loaded = await frap.deserialize('signatures/submit.json');
const resolved = await loaded.locate(page);

if (resolved.healed) {
  console.log(`Восстановлено! Новый селектор: ${resolved.healedSelector}`);
  console.log(`Уверенность: ${resolved.confidence}`);
  console.log('Кандидаты:', resolved.debug.candidates);
}

await resolved.element.click();

// Обновить привязку после рефакторинга
await binding.rebind(page);
await signature.serializeTo('signatures/submit-v2.json');
```

### Python (Playwright/Pytest)

```python
from frap import Frap, Signature

# Привязка
binding = await Frap.bind(page.locator("[data-testid='submit']"))
signature = binding.get_signature()

# Сериализация
signature.serialize_to("signatures/submit.json")

# Разрешение (возможно самовосстановление)
loaded = Frap.deserialize("signatures/submit.json")
resolved = await loaded.locate(page)

if resolved.healed:
    print(f"Селектор восстановлен: {resolved.healed_selector}")
    print(f"Уверенность: {resolved.confidence}")
    print(f"Кандидаты: {resolved.debug.candidates}")

await resolved.element.click()

# Обновить привязку
await binding.rebind(page)

# Интеграция с Page Object
from frap.pytest import frap_fixture

@frap_fixture
def login_page(page):
    return {
        "username": Frap.bind(page.locator("[name='username']")),
        "password": Frap.bind(page.locator("[name='password']")),
        "submit": Frap.bind(page.locator("[type='submit']")),
    }
```

---

## CLI-утилита

### Установка

```bash
# npm (Playwright / Node)
npm install @frap/frap @frap/frap-playwright
# CLI (если вынесен в bin пакета @frap/frap)
npx frap --help

# PyPI
pip install frap-sdk frap-playwright

# Cargo (нативная производительность)
cargo install frap-sdk

# Homebrew (когда появится формула)
brew install frap
```

### Команды

```bash
# 🔗 bind — привязать селектор, извлечь сигнатуру
frap bind --selector "[data-testid='submit']" --output submit.signature.json

# 🔍 locate — разрешить сигнатуру, найти элемент
frap locate --signature submit.signature.json --url https://app.example.com

# 🔄 rebind — обновить привязку (повторное сканирование)
frap rebind --signature submit.signature.json --output submit-v2.signature.json

# 📊 scan — просканировать страницу, найти привязываемые элементы
frap scan --url https://app.example.com --output site-map.json

# 🔎 unbound — найти непривязанные/нестабильные элементы
frap unbound --url https://app.example.com --selector ".dynamic-class"

# 📈 report — сгенерировать отчёт о стабильности
frap report --signatures-dir ./signatures/ --output stability-report.html

# 🧪 test — проверить разрешимость всех сигнатур
frap test --signatures-dir ./signatures/ --url https://app.example.com

# 🗂️  diff — сравнить два DOM-состояния
frap diff --signature-v1 submit-v1.signature.json --signature-v2 submit-v2.signature.json

# ⚙️  config — показать/установить конфигурацию
frap config set confidence 0.90
frap config set maxCandidates 3
frap config show
```

### Примеры использования CLI

```bash
# 1. Быстрый старт: просканировать и привязать ключевые элементы
frap scan --url https://app.example.com \
  --selectors "[data-testid]" \
  --output onboarding.signatures/

# 2. CI: проверить, что все элементы разрешимы
frap test --signatures-dir ./signatures/ --url https://staging.example.com \
  --fail-on-healed \
  --report ci-report.json

# 3. Обновление после рефакторинга
frap rebind --signatures-dir ./signatures/ --url https://app.example.com \
  --backup-dir ./signatures/backup/ \
  --dry-run  # сначала показать, что изменится

# 4. Интеграция с Playwright
frap scan --url http://localhost:3000 --format playwright-po \
  --output tests/pages/LoginPage.frap.ts
```

---

## Основные концепции

### Структурная регрессия UI

Frap проверяет не «как выглядит пиксель», а **как устроена страница**: иерархия DOM, кластеры компонентов, сигнатуры элементов, при необходимости — относительная геометрия (roadmap). Требования переводятся в **структурные инварианты** (baseline element map, зоны drift, пороги confidence); при изменении UI CI получает **объяснимый diff**, а не только красно-зелёный снимок.

| Вопрос | Visual regression | Frap |
|--------|-------------------|------|
| Что сравниваем | Пиксели, скриншот | Дерево, кластеры, сигнатуры |
| Типичный сбой | Цвет, шрифт, anti-aliasing | Пропал блок, сдвинулась форма, сломалась вложенность |
| Артефакт | PNG diff | `drift-report`, кандидаты, score |

Слои дополняют друг друга: скриншоты — «как нарисовано», Frap — «как собрано».

> **Матрица доступности**: какие возможности доступны сейчас, а какие в roadmap — см. [docs/structural-contract.md](docs/structural-contract.md).

### Структура сигнатуры

```json
{
  "version": "1.0",
  "anchor": {
    "selector": "form.login",
    "features": { "tag": "form", "id": "login-form" }
  },
  "path": {
    "steps": [{ "index": 2, "tag": "button" }]
  },
  "features": {
    "text": "Submit",
    "attributes": { "type": "submit", "data-testid": "submit-btn" },
    "visualHash": "a1b2c3d4"
  },
  "fallback": {
    "originalSelector": "[data-testid='submit-btn']"
  }
}
```

### Стратегия разрешения

1. **Точное совпадение** — якорь и путь без изменений
2. **Сдвиг якоря** — якорь переместился, путь ещё валиден
3. **Структурный сдвиг** — якорь стабилен, путь изменился
4. **Нужно самовосстановление** — нечёткое сопоставление по похожести признаков
5. **Сбой** — нет кандидатов выше порога `confidence`

---

## Ошибки и граничные случаи

### Сбои при разрешении

| Сценарий | Поведение | Рекомендация |
|----------|-----------|--------------|
| **Нет кандидатов** | `Resolution.failure()` — элемент не найден | Проверить селектор, выполнить `rebind` |
| **Неоднозначное совпадение** | Несколько кандидатов с одинаковой оценкой | Выбрать вручную или поднять порог `confidence` |
| **Низкая уверенность** | Оценка ниже порога | Смотреть `debug.candidates`, обновить привязку |
| **Устаревшая сигнатура** | Версия сигнатуры не совпадает | Миграция через `rebind` |

### Поведение стратегий healing

```java
// Строгий режим — падать при любом самовосстановлении
Frap.config().setHealStrategy(HealStrategy.FAIL);

// Режим отчёта — продолжить, но зафиксировать факт healing
Frap.config().setHealStrategy(HealStrategy.REPORT);

// Авто (по умолчанию) — прозрачное самовосстановление
Frap.config().setHealStrategy(HealStrategy.AUTO);
```

---

## Типовые сценарии

### 1. Первичная привязка (разработка)

```java
// Зафиксировать стабильные селекторы на этапе разработки
Signature sig = Frap.bind(page.locator("#submit")).getSignature();
sig.serializeTo("signatures/login-submit.json");
```

### 2. Проверка в CI (строгий режим)

```bash
frap test --signatures-dir ./signatures/ \
  --confidence 0.95 \
  --fail-on-healed \
  --url https://staging.example.com
```

### 3. Обновление после рефакторинга

```bash
# Предпросмотр изменений
frap rebind --signatures-dir ./signatures/ \
  --url https://app.example.com \
  --dry-run

# Применить с резервной копией
frap rebind --signatures-dir ./signatures/ \
  --url https://app.example.com \
  --backup-dir ./signatures/backup/
```

### 4. Отладка неудачного разрешения

```typescript
const resolved = await sig.locate(page);

if (!resolved.success) {
  console.log('Не удалось найти элемент');
  console.log('Рассмотренные кандидаты:', resolved.debug.candidates);
  console.log('Изменения DOM с момента привязки:', resolved.debug.diff);
  console.log('Рекомендация:', resolved.debug.recommendation);
}
```

---

## Сравнение: классика и Frap

| Сценарий | Без Frap | С Frap |
|----------|----------|--------|
| Простой клик | `page.click("#btn")` | `Frap.bind("#btn").getSignature().locate(page).click()` |
| Рефакторинг `data-testid` | Падающие тесты | Самовосстановление с отчётом |
| Динамические классы | Нестабильные селекторы | Сопоставление по структурной сигнатуре |
| Стабильность между версиями | Ручные правки | Сценарий `rebind` |
| Разбор падений | Скриншоты в консоли | Структурированный diff и кандидаты |
| Проверка вёрстки / layout | Pixel snapshot, visual diff | Baseline element map, drift-отчёт, структурные инварианты |

---

## Терминология

| Термин | Значение |
|--------|----------|
| **Frap** | Бренд/название инструмента |
| **frap** | CLI, селектор `frap:[…]`, префикс артефактов (`frap-sdk`, `frap-core`, …) |
| **github.com/kotler-dev/frap** | Основной репозиторий и документация |
| **@frap/** | npm-scope (организация `frap` на npmjs.com) |
| **bind** | Привязка селектора к структуре DOM |
| **signature** | Сигнатура элемента — структурные признаки для идентификации |
| **anchor** (якорь) | Стабильный родительский узел, от которого строится путь |
| **rebind** | Обновить привязку (повторное сканирование DOM) |
| **locate** | Найти элемент в текущем DOM по сигнатуре |
| **resolution** | Результат разрешения сигнатуры в элемент |
| **serialize** | Сохранить сигнатуру в JSON |
| **deserialize** | Загрузить сигнатуру из JSON |
| **unbound** | Элемент без привязки (потенциально нестабильный) |
| **healed** | Элемент найден через кластеризацию при неточном совпадении |
| **confidence** | Оценка уверенности при healing (0.0–1.0) |
| **структурная регрессия UI** | Проверка «страница/блок устроены как в требованиях» по element map и drift, с объяснимым diff (не pixel diff). Подробнее: [docs/structural-contract.md](docs/structural-contract.md) |
| **drift** | Отличие текущей структуры UI от baseline (element, structural, cluster) |

---

## Сводка API

```
Frap.bind(selector) → Binding → getSignature() → Signature
                                      ↓
                              serialize() → JSON
                                      ↓
                              deserialize() → Signature
                                      ↓
                              locate(context) → Resolution
                                      ↓
                         ┌────────────┼────────────┐
                         ↓            ↓            ↓
                      успех      healed       сбой
                         ↓            ↓            ↓
                    элемент    кандидаты    отладка
                                + diff
                              + confidence
```

---

## Артефакты v1.1

### Файлы отчётов

| Файл | Содержание | Когда генерируется |
|------|------------|-------------------|
| `frap-report.json` | Общий отчёт о тестировании | Всегда при `enableReporting` |
| `frap-events.jsonl` | Healing-события (построчно) | При heal попытках |
| `frap-context.json` | Timeline: UI + network + console | При `captureAll: true` |
| `frap-rca.json` | Root Cause Analysis | При `captureAll: true` и падениях |
| `frap-debug.html` | Debug Classic отчёт | При `enableReporting` |
| `frap-debug-explorer.html` | Debug Explorer отчёт | При `enableReporting` |
| `junit.xml` | JUnit-совместимый XML | Всегда при `enableReporting` |

### Пример структуры отчётов

```
frap-reports/
├── frap-report.json           # JSON сводка
├── frap-events.jsonl          # Healing events (JSON Lines)
├── frap-context.json          # Unified Context timeline (v1.1)
├── frap-rca.json              # RCA анализ (v1.1)
├── frap-debug.html            # Debug Classic
├── frap-debug-explorer.html   # Debug Explorer
└── junit.xml                  # JUnit для CI
```

---

## Context Layer (F002) v1.1

Unified Context объединяет три источника данных в timeline:

| Источник | Что захватывается | Корреляция |
|----------|------------------|------------|
| **UI** | DOM-события, клики, heal-попытки | `trace_id` |
| **Network** | HTTP запросы/ответы, WebSocket | `trace_id` + timestamp |
| **Console** | `console.*`, `pageerror` | `trace_id` + timestamp |

### Когда Context помогает

- **C002** — API timeout: timeline показывает `requestfailed` перед UI failure
- **C003** — Flaky тест: сравнение timeline между прохождениями
- **C004** — WebSocket: отслеживание сообщений до падения

### Включение в Playwright

```typescript
// playwright.config.ts
import { frapPlaywright } from '@frap/frap-playwright';

export default defineConfig({
  ...frapPlaywright({
    captureAll: true,        // Включает context + RCA
    reportDir: './frap-reports',
    minConfidence: 0.85,
  }),
});
```

---

## Root Cause Analysis (F003) v1.1

RCA автоматически классифицирует причину падения:

| Классификация | Признаки | Пример |
|---------------|----------|--------|
| **UI-change** | DOM изменился, healed найден | Переименован `data-testid` |
| **API-error** | 4xx/5xx или timeout в network | C002: `payment/timeout` |
| **Infrastructure** | Connection refused, DNS error | Сервер недоступен |
| **Flaky** | Разные результаты при одних входных | Race condition |
| **Unknown** | Недостаточно данных | Требуется ручной анализ |

### Per-test RCA

RCA теперь привязан к конкретному тесту через `trace_id`:

```typescript
// В отчёте frap-rca.json
{
  "test_id": "payment-flow-001",
  "trace_id": "abc-123-def",
  "classification": "api_error",
  "confidence": 0.95,
  "timeline_excerpt": [ /* события ±5 сек */ ],
  "recommendation": "Проверьте эндпоинт /api/payment — timeout 30s"
}
```

---

## Конфигурация Playwright (v1.1)

### Полный пример

```typescript
import { frapPlaywright, registerFrapSelector } from '@frap/frap-playwright';

export default defineConfig({
  ...frapPlaywright({
    // Core
    minConfidence: 0.85,       // Порог для healing (0.0–1.0)
    maxCandidates: 5,          // Макс. кандидатов в отчёте

    // Reporting
    enableReporting: true,     // JSON + JUnit отчёты
    reportDir: './frap-reports',

    // Context (v1.1)
    captureAll: true,          // UI + network + console + WebSocket

    // Debug
    debug: false,              // Подробные логи
  }),
  use: {
    async setup({ selectors }) {
      await registerFrapSelector(selectors);
    },
  },
});
```

### Рекомендуемые настройки

| Сценарий | `minConfidence` | `captureAll` | `maxCandidates` |
|----------|-----------------|--------------|-----------------|
| Разработка | 0.85 | false | 5 |
| CI (строгий) | 0.95 | true | 3 |
| CI (мягкий) | 0.80 | true | 10 |
| Отладка | 0.70 | true | 10 |

---

## В двух словах:
> *Frap привязывает селекторы к структуре — детерминированная привязка, структурная регрессия UI с объяснимым diff, автоматическое самовосстановление, контекст для RCA, готовность к LLM.*
