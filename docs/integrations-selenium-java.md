# Минимальная интеграция: JUnit 5 + Selenium + Page Object (банк)

Целевая схема для **S1** ([audience.md](./audience.md)): legacy Selenium/Java без смены раннера и без переписывания Page Object.

**Статус:** specification (код Java SDK — roadmap P4 / v1.4). TypeScript/Playwright — reference implementation сегодня.

**SDK на три языка:** общая стратегия — [sdk-strategy.md](../project/architecture/sdk-strategy.md).

---

## Что не меняем

| Компонент | Действие |
|-----------|----------|
| JUnit 5 / TestNG | Остаётся раннером |
| Selenium 4 `WebDriver` | Остаётся драйвером |
| Page Object классы | Структура, имена методов, бизнес-шаги — без изменений |
| CI (GitLab/Jenkins) | Тот же job; + артефакты `fletta-reports/` и JUnit properties |

Fletta встраивается **ниже** теста: на `findElement` / `@FindBy`, не в сценарий.

---

## Рекомендуемый вариант (минимум магии)

**Primary:** JUnit 5 Extension + перехват поиска элемента.

```
@Test class
    → FlettaExtension (before/after, config, reports)
        → WebDriver (существующий)
            → FlettaElementLocator (обёртка)
                → driver.findElement(By...) 
                → при failure: snapshot → Core.heal() → retry или fail + diff
```

**Почему не только SelfHealingDriver:** Extension проще для ИБ-review (явная точка включения в `@ExtendWith`), совместим с уже обёрнутым driver (логирование, скриншоты).

**Альтернатива** (если вся фабрика driver в одном месте): `FlettaWebDriver` decorator — тот же hook на `findElement`.

| Подход | Когда выбирать |
|--------|----------------|
| **JUnit 5 Extension** | Стандартный банк: JUnit 5, PO через `driver` в base class |
| **WebDriver wrapper** | Один `DriverFactory` на проект |
| **Selenium 4 listener** | Уже есть listener-инфраструктура; версионная связка |

---

## Фазы внедрения (банк)

### Фаза 0 — без Java SDK (можно сейчас)

Цель: discovery и element map для новых/больных страниц, **без** runtime healing в Selenium.

```bash
fletta discover --url https://stand.example/app/login
# → element-map.json → ручное или F004 обновление локаторов в PO
```

- Раннер и тесты **не трогаем**.
- Ценность: [pains.md](./pains.md) P01, S1-P01.
- Подходит для пилота с ИБ до появления JNI.

### Фаза 1 — pilot (1 suite, 5–10 сценариев)

1. Зависимость: `fletta-junit5` + native Core (через JNI) — **один** модуль pilot.
2. `@ExtendWith(FlettaExtension.class)` на базовом классе или pilot-классе.
3. `fletta.conf` / env: `FLETTA_MIN_CONFIDENCE=0.85`, `FLETTA_REPORT_DIR=build/fletta-reports`.
4. CI: junit artifact + `fletta-reports/**` (как CP005 для Playwright).

```java
@ExtendWith(FlettaExtension.class)
class CheckoutE2ETest extends BaseUiTest {

  @Test
  void payWithCard() {
    checkoutPage.payButton().click(); // внутри — findElement с healing hook
  }
}
```

Page Object **без** аннотаций Fletta в идеале:

```java
public class CheckoutPage {
  private final WebDriver driver;

  public WebElement payButton() {
    return driver.findElement(By.cssSelector("[data-testid='pay-btn']"));
  }
}
```

### Фаза 2 — масштаб

- Base class с `@ExtendWith` для всех UI-тестов.
- Allure attachment `fletta-healing.json`.
- Policy: safe-fail при ambiguous (как CP003).
- Опционально: `@FlettaLocator` на полях Page Object — только если нужен явный signature в PO.

---

## Maven / Gradle (целевой sketch)

> Координаты и артефакты — TBD при реализации.

```xml
<!-- pilot module -->
<dependency>
  <groupId>io.fletta</groupId>
  <artifactId>fletta-junit5</artifactId>
  <version>${fletta.version}</version>
  <scope>test</scope>
</dependency>
```

Native library: platform classifier (`linux-x86_64`, `macos-aarch64`) или fat JAR с bundled `.so`/`.dylib` — решение при F000 FFI.

---

## Конфигурация (минимум)

| Параметр | Default | Описание |
|----------|---------|----------|
| `minConfidence` | `0.85` | Порог auto-heal |
| `reportDir` | `build/fletta-reports` | JSONL, debug HTML |
| `healOnFailure` | `true` | Только после failed find |
| `safeFailOnAmbiguous` | `true` | Не кликать при низкой уверенности |

---

## CI (GitLab — типичный банк)

```yaml
ui-e2e:
  script:
    - ./gradlew test --tests 'com.bank.e2e.**'
  artifacts:
    when: always
    reports:
      junit: build/test-results/test/TEST-*.xml
    paths:
      - build/fletta-reports/
```

JUnit XML: properties `fletta.healed`, `fletta.confidence`, `fletta.diff` на упавших/вылеченных шагах — см. [integrations.md](./integrations.md).

---

## Сигнатуры и Page Object

| Практика | Рекомендация |
|----------|--------------|
| Primary locator | `data-testid` / `aria-label` / role — как сейчас |
| Signature | Core вычисляет при первом успешном find; хранится в event log |
| Рефакторинг testid | Healing на том же `By`; diff в отчёте |
| Page Object gen (F004) | Опция: импорт element map → новый PO, не обязателен для healing |

---

## Python SDK (для сравнения)

Тот же контракт Core, другой hook:

| Стек | Hook |
|------|------|
| pytest + Selenium | `pytest` plugin + fixture `fletta_driver` |
| pytest-playwright | Аналог `@fletta/playwright` (позже) |

Python v1: **JSON-RPC к CLI** ([sdk-strategy.md](../project/architecture/sdk-strategy.md)); JNI не обязателен в первом релизе Python.

---

## TypeScript (уже MVP)

Банк с параллельным Playwright-пилотом: `@fletta/playwright` — тот же Core, те же отчёты. Java и TS команды сходятся в **одном** формате `fletta-events.jsonl` / junit properties.

---

## Чек-лист для пилота с legacy-командой

- [ ] Фаза 0: `discover` на 1–2 критичных flow
- [ ] Согласование с ИБ: on-prem, no cloud, NO ML in core
- [ ] Выбран hook: Extension vs WebDriver wrapper
- [ ] 1 модуль, 5–10 тестов, `minConfidence` + safe-fail
- [ ] CI artifacts: JUnit + `fletta-reports/`
- [ ] Критерий успеха: меньше ручных правок PO после рефакторинга UI + audit trail

---

## Связанные документы

- [integrations.md](./integrations.md) — обзор, экспорт JUnit/Allure
- [pains.md](./pains.md) — S1 боли
- [audience.md](./audience.md) — сегмент S1, rollout в банке
- [sdk-strategy.md](../project/architecture/sdk-strategy.md) — Java / TS / Python
- [F000](../project/feature/F000-core-platform-api.md) — FFI для Java
