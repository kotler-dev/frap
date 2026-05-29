# Frap + Playwright Java (демо)

Пример: оборачиваете локаторы Playwright, чтобы тесты переживали небольшие изменения UI (переименовали `data-testid`, поменяли разметку) — с понятным результатом: новый селектор, confidence, HTML-отчёт.

**Пакеты:** `io.github.kotlerdev.frap.*`  
**Maven:** `groupId` `io.github.kotler-dev`, артефакт `frap-playwright`

Живое демо (2 слайда + прогон): [SHOWCASE.ru.md](SHOWCASE.ru.md)  
Сервер и окружение: [DEMO_SERVER.ru.md](DEMO_SERVER.ru.md)

## Зачем это нужно

- **Стабильная автоматизация** — меньше ручных правок селекторов после каждого рефакторинга UI.
- **Привычный API** — тот же Playwright `Locator`, обёртка `FrapLocator`.
- **Объяснимость** — после heal смотрите `HealResult`: был ли heal, новый селектор, confidence, список кандидатов.
- **Отчёты** — в `frap-debug.html` видны шаги, кластеры кандидатов и итоговое решение.

## Как это работает (простыми словами)

1. Оборачиваете локатор: `Frap.withFrap(page.locator("..."), page)`.
2. Если элемента нет, Frap снимает снимок интерактивных узлов и **сравнивает сигнатуры** с тем, что тест «запомнил».
3. Движок **ранжирует кандидатов** (confidence). Если два лидера слишком близки (разница &lt; 0.1), heal **отклоняется** — чтобы не кликнуть не ту кнопку.
4. При успехе действие выполняется с новым селектором; в коде — `Frap.getLastHealResult(locator)`.
5. С `debug: true` в HTML-отчёте похожие кандидаты сгруппированы по structural prefix — блок **DOM clusters** (подробнее в [SHOWCASE.ru.md](SHOWCASE.ru.md)).

## Быстрый старт

**Java 17+** и **Maven**.

Из корня репозитория (рекомендуется):

```bash
./scripts/run-java-e2e.sh
```

Или из этой папки после [запуска сервера](DEMO_SERVER.ru.md):

```bash
mvn test
```

## Команды

| Команда | Что делает |
|---------|------------|
| `mvn test` | Все E2E-демо (`@Tag("e2e")`) |
| `mvn test -Dtest=ScheduleHealingTest` | Сценарий self-healing (расписание) |
| `mvn test -Dtest=ScheduleHealingTest#testOpensTalkAfterRefactor` | Один healing-тест |
| `mvn test -Dtest=CfpAmbiguousHealTest` | Отказ heal при неоднозначности (CFP) |
| `mvn test -DskipIT` | Пропуск E2E, если окружение не готово |
| `mvn -q exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"` | Установить браузер один раз |

## Минимальный пример

```java
import io.github.kotlerdev.frap.core.dto.HealResult;
import io.github.kotlerdev.frap.playwright.wrapper.Frap;
import io.github.kotlerdev.frap.playwright.wrapper.FrapLocator;

FrapLocator button = Frap.withFrap(
    page.locator("[data-testid='talk-open-healing']"),
    page
);

button.click();

if (Frap.isHealed(button)) {
    HealResult r = Frap.getLastHealResult(button);
    System.out.println("Новый селектор: " + r.selector());
    System.out.println("Confidence: " + r.confidence());
}
```

## Примеры демо-тестов

| Класс | Чему учит |
|-------|-----------|
| `ScheduleHealingTest` | Heal после смены testid на `schedule-heal.html`; политики heal |
| `CfpAmbiguousHealTest` | Две похожие кнопки — heal отклонён, несколько кандидатов |
| `PaymentTimeoutTest` | Захват контекста и RCA при медленном API |
| `ZzzReportingVerificationTest` | После прогона появляются файлы отчётов |

Демо-страница: тест ищет `talk-open-healing`, в HTML — `talk-card-open-healing`. Откройте `http://localhost:3000/conference/schedule-heal.html`, когда сервер запущен.

## Отчёты

После `mvn test`:

```bash
open target/frap-reports/conference/frap-debug.html
```

| Файл | Описание |
|------|----------|
| `frap-debug.html` | Debug UI: шаги, DOM clusters, кандидаты, решение |
| `frap-debug-explorer.html` | Боковая панель, если несколько тестов с `debug: true` |
| `debug-reports/*.json` | JSON по каждому тесту |
| `frap-report.json` | Сводка healing-событий |
| `frap-events.jsonl` | Поток событий (JSONL) |
| `junit.xml` | Результаты JUnit |

Подробный отчёт на локаторе:

```java
Frap.withFrap(locator, page, options.debug(true));
```

## Своя страница

Другой хост:

```bash
mvn test -Dtest.server.url=http://localhost:8080
```

Статика и свой сервер — [DEMO_SERVER.ru.md](DEMO_SERVER.ru.md).

## Конфигурация

| Свойство / env | По умолчанию | Смысл |
|----------------|--------------|--------|
| `frap.reportDir` | `target/frap-reports/conference` | Каталог отчётов |
| `test.server.url` | `http://localhost:3000` | Базовый URL демо-приложения |
| `frap.minConfidence` | `0.85` | Минимальный confidence для heal |

`FRAP_CORE_BIN` и ручная сборка ядра — только в [DEMO_SERVER.ru.md](DEMO_SERVER.ru.md).

## Зависимость

```xml
<dependency>
    <groupId>io.github.kotler-dev</groupId>
    <artifactId>frap-playwright</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

На класс тестов — `@ExtendWith(FrapExtension.class)` для автоматической генерации отчётов в конце прогона.
