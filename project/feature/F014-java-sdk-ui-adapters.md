# Feature: Java SDK & UI Adapters (F014)

## Meta

- **Epic**: SDK → Java
- **Roll-up target**: ## v1.4.0 (Java / bank S1)
- **Status**: draft
- **Target release**: v1.4.0
- **Created**: 2026-05-23
- **Related cases**: CP005 (JUnit export), C004 (POM export), S1 pains

## Goal

Java SDK (JNI → Rust Core) и UI-адаптеры для существующих e2e: **JUnit 5 + WebDriver + Page Object** без смены раннера. Опционально — интеграция с **Selenide** (P1). Не заменяем Selenium/Selenide — перехватываем поиск элемента и даём explainable healing + CI audit.

> **Именование:** фича не «Selenium Adapter» — Selenium WebDriver это transport; Selenide — обёртка над тем же WebDriver. Целевой термин: **Java UI adapters**.

## User workflow

### Фаза 0 (без SDK)

1. `frap discover` → element map для критичных страниц.
2. Обновление локаторов в Page Object вручную или через F004.

### Фаза 1 (pilot)

1. Зависимости: `io.frap:frap-sdk` + `io.frap:frap-junit5`.
2. `@ExtendWith(FlettaExtension.class)` на базовом или pilot-классе.
3. Существующие PO вызывают `driver.findElement(By...)` — hook при failure → heal.
4. CI: JUnit XML + `build/frap-reports/`.

### Фаза 2 (Selenide, P1)

1. Подключение `frap-selenide` (или модуль в adapter).
2. Hook на уровне поиска `SelenideElement` без переписывания fluent-стиля тестов.

## Scope

### In (P0 — bank legacy)

- `sdk/java/` — JNI, типы `HealResult`, config, `emitEvent`
- `adapters/junit5/` — JUnit 5 Extension, lifecycle, report writers
- Hook: `WebDriver.findElement` / обёртка driver / listener (один primary вариант в MVP)
- Page Object **без обязательных** Fletta-аннотаций
- Экспорт: JUnit XML properties (`frap.healed`, `frap.confidence`, `frap.diff`), Allure attachment
- Фаза 0: CLI discover документирована в [integrations-selenium-java.md](../../docs/integrations-selenium-java.md)
- On-prem, NO ML in core

### In (P1 — Selenide)

- Adapter hook для Selenide (`ElementFinder` / listener — уточнить при реализации)
- Документация: когда выбирать WebDriver vs Selenide integration

### Out

- Замена JUnit / TestNG / Karate
- Собственный WebDriver или браузерный стек
- TestNG adapter (backlog)
- Appium / mobile (F006)
- Обязательная миграция на Fletta DSL

## Acceptance criteria

- [ ] F000 FFI: Java может вызвать `heal()` на native Core
- [ ] `sdk/java` публикуется в monorepo, smoke-test с JUnit
- [ ] JUnit 5 Extension: pilot 5–10 тестов, PO без изменений структуры
- [ ] При failed find + confidence ≥ threshold → retry с healed locator
- [ ] Safe-fail при ambiguous (аналог CP003)
- [ ] CP005-equivalent: JUnit artifact + `frap-reports/` в CI
- [ ] Документация: quick start < 30 мин на demo Selenium проекте
- [ ] (P1) Selenide pilot: 1 suite с `$` API

### Target layout

```
sdk/java/
adapters/junit5/
adapters/selenide/    # P1, optional module
```

## Implementation notes (sketch)

### Primary integration (P0)

```
@Test
@ExtendWith(FlettaExtension.class)
class CheckoutTest extends BaseUiTest {
  @Test void pay() {
    pages.checkout().payButton().click(); // → findElement + heal hook
  }
}
```

### Transport

- JNI → `cdylib` (F000), platform classifiers в Maven
- Зависит от **F000** до production-ready SDK

### Зависимости

| Фича | Связь |
|------|--------|
| F000 | FFI / native Core |
| F001 | Healing algorithms |
| F013 | Контракт API (reference) |
| F012 | Debug report format (parity) |

### Риски

- Упаковка native libs в enterprise CI (Linux agents)
- Разные фабрики WebDriver в банке — предпочтить Extension over global driver replacement
- Selenide версии и внутренние API для P1

## Verification / Test plan

### Manual smoke

```bash
# После реализации
./gradlew test --tests 'com.example.e2e.CheckoutTest'
ls build/frap-reports/
```

### Automation

- Demo module в monorepo: Selenium + JUnit 5 + intentional UI drift
- CI job: JUnit publisher + archive `frap-reports/`

## Related docs

- [integrations-selenium-java.md](../../docs/integrations-selenium-java.md)
- [sdk-strategy.md](../architecture/sdk-strategy.md)
- [audience.md](../../docs/audience.md) — сегмент S1
- [pains.md](../../docs/pains.md) — S1-P01…P05
- [F000: Core Platform API](./F000-core-platform-api.md)
- [F013: TypeScript SDK](./F013-typescript-sdk.md)
