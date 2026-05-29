# Feature: Java SDK & UI Adapters (F014)

## Meta

- **Epic**: SDK → Java
- **Roll-up target**: ## v1.4.0 (Java / bank S1)
- **Status**: in_progress (Playwright track ✅; WebDriver/Selenide pending)
- **Target release**: v1.4.0
- **Created**: 2026-05-23
- **Updated**: 2026-05-28
- **Related cases**: CP005 (JUnit export), C002 (RCA), S1 pains

## Goal

Java SDK (JNI → Rust Core) и UI-адаптеры для существующих e2e: **JUnit 5 + WebDriver + Page Object** без смены раннера. Опционально — интеграция с **Selenide** (P1). Не заменяем Selenium/Selenide — перехватываем поиск элемента и даём explainable healing + CI audit.

Архитектурный guardrail: Java слой остаётся thin (transport/hooks/reports) и не реализует отдельный scoring/clustering движок; алгоритмика идёт через Core (см. [ADR-001](../architecture/ADR-001-core-language-strategy.md)).

> **Именование:** фича не «Selenium Adapter» — Selenium WebDriver это transport; Selenide — обёртка над тем же WebDriver. Целевой термин: **Java UI adapters**.

## Progress summary (2026-05-27)

| Track | Статус | Артефакты |
|-------|--------|-----------|
| **A — Core + Playwright Java** | ✅ PoC готов | `sdk/java/frap-core-java`, `adapters/playwright-java`, `internal/demo/showcase/java-playwright` |
| **B — WebDriver + Page Object** | ❌ не начато | `adapters/junit5/` (план) |
| **C — Selenide** | ❌ P1 | `adapters/selenide/` (план) |

**Gate:** `./scripts/run-java-e2e.sh` — 13 E2E, `BUILD SUCCESS` (conference healing + CP005 reports + C002 RCA).

## User workflow

### Фаза 0 (без SDK)

1. `frap discover` → element map для критичных страниц.
2. Обновление локаторов в Page Object вручную или через F004.

### Фаза 1 (pilot)

1. Зависимости: `io.frap:frap-sdk` + `io.frap:frap-junit5`.
2. `@ExtendWith(FrapExtension.class)` на базовом или pilot-классе.
3. Существующие PO вызывают `driver.findElement(By...)` — hook при failure → heal.
4. CI: JUnit XML + `build/frap-reports/`.

### Фаза 2 (Selenide, P1)

1. Подключение `frap-selenide` (или модуль в adapter).
2. Hook на уровне поиска `SelenideElement` без переписывания fluent-стиля тестов.

### Track A — Playwright Java (реализовано)

1. `mvn install` в `sdk/java` + `FRAP_CORE_BIN` → `frap-core-rpc`.
2. `@ExtendWith(FrapExtension.class)` + `Frap.withFrap(locator, page)` или `Frap.withFrap(page, selector, options)`.
3. `./scripts/run-java-e2e.sh` для полного L4 gate.

## Scope

### In (P0 — bank legacy)

- `sdk/java/` — JNI, типы `HealResult`, config, `emitEvent`
- `adapters/junit5/` — JUnit 5 Extension, lifecycle, report writers
- Hook: `WebDriver.findElement` / обёртка driver / listener (один primary вариант в MVP)
- Page Object **без обязательных** Frap-аннотаций
- Экспорт: JUnit XML properties (`frap.healed`, `frap.confidence`, `frap.diff`), Allure attachment
- Фаза 0: CLI discover документирована в [integrations-selenium-java.md](../../docs/integrations-selenium-java.md)
- On-prem, NO ML in core

### In (Track A — Playwright Java, PoC ✅)

- `sdk/java/frap-core-java` — DTOs, `FrapRpcClient`, context, RCA types
- `adapters/playwright-java` — `withFrap`, healing proxy, `SnapshotBuilder`, `FrapExtension`
- `internal/demo/showcase/java-playwright` — conference + context E2E (`@Tag("e2e")`)
- Selector resolution: `Locator@` + `locator('...')`; pre-record via Playwright API (не `querySelector` на мусорных строках)
- CI: `java-sdk` (unit) + `java-playwright-e2e` (tag / workflow_dispatch)

### In (P1 — Selenide)

- Adapter hook для Selenide (`ElementFinder` / listener — уточнить при реализации)
- Документация: когда выбирать WebDriver vs Selenide integration

### Out

- Замена JUnit / TestNG / Karate
- Собственный WebDriver или браузерный стек
- TestNG adapter (backlog)
- Appium / mobile (F006)
- Обязательная миграция на Frap DSL

## Acceptance criteria

### Track A — Playwright Java (✅)

- [x] `sdk/java` в monorepo, smoke с `frap-core-rpc` (CI `java-sdk`)
- [x] Playwright adapter: `withFrap`, healing on click/fill/…
- [x] Conference healing: CP002-equivalent (`ScheduleHealingTest`, confidence 1.0)
- [x] CP005-equivalent: `frap-report.json`, `frap-events.jsonl`, `junit.xml`
- [x] Context/RCA: `PaymentTimeoutTest` (C002)
- [x] E2E gate: `./scripts/run-java-e2e.sh` (13 tests)
- [x] Документация: `internal/demo/showcase/java-playwright/README.md`, `adapters/playwright-java/README.md`
- [x] Discovery API: `build_element_map`, `filter_element_map`, `Frap.discover`
- [x] Page Object: `generate_page_object`, `Frap.generatePageObject`
- [ ] Maven Central 1.0.0 published (workflow ready; tag + secrets required)

### Track B — WebDriver / bank S1 (❌)

- [ ] F000 FFI: production JNI path (optional; RPC works today)
- [ ] JUnit 5 Extension: pilot 5–10 тестов, PO без изменений структуры
- [ ] При failed find + confidence ≥ threshold → retry с healed locator
- [ ] Safe-fail при ambiguous (аналог CP003)
- [ ] Документация: quick start < 30 мин на demo Selenium проекте
- [ ] (P1) Selenide pilot: 1 suite с `$` API

### Target layout

```
sdk/java/
  frap-core-java/          ✅
  frap-core-native/        ⚠️ (FFI module, optional)
adapters/playwright-java/  ✅
adapters/junit5/           ❌ planned
adapters/selenide/         ❌ P1
internal/demo/showcase/java-playwright/  ✅
```

## Implementation notes (sketch)

### Primary integration (P0)

```
@Test
@ExtendWith(FrapExtension.class)
class CheckoutTest extends BaseUiTest {
  @Test void pay() {
    pages.checkout().payButton().click(); // → findElement + heal hook
  }
}
```

### Playwright Java (реализовано)

```java
FrapLocator btn = Frap.withFrap(
    page.locator("[data-testid='talk-open-healing']"),
    page,
    ConferencePaths.confFrap()
);
btn.click();
```

### Transport

- **Сейчас:** JSON-RPC subprocess → `frap-core-rpc` (`FrapRpcClient`)
- **Позже:** JNI → `cdylib` (`frap-core-native`, F000 P1)

### Зависимости

| Фича | Связь |
|------|--------|
| F000 | FFI / native Core |
| F001 | Healing algorithms |
| F008 | Playwright adapter (TS reference) |
| F013 | Контракт API (reference) |
| F012 | Debug report format (parity) |

### Риски

- Упаковка native libs в enterprise CI (Linux agents)
- Разные фабрики WebDriver в банке — предпочтить Extension over global driver replacement
- Selenide версии и внутренние API для P1

## Verification / Test plan

### Playwright Java E2E (автоматизация ✅)

```bash
./scripts/run-java-e2e.sh
# или
cd adapters/playwright-java && mvn test          # unit (9–10 tests)
cd internal/demo/showcase/java-playwright && mvn test   # требует test-app + RPC + browsers
```

### WebDriver smoke (после Track B)

```bash
./gradlew test --tests 'com.example.e2e.CheckoutTest'
ls build/frap-reports/
```

## Related docs

### User Documentation (1.0.0)
- [sdk/java/README.md](../../sdk/java/README.md) — Java SDK entry point
- [docs/en/java-getting-started.md](../../docs/en/java-getting-started.md) — 5-minute quick start
- [docs/en/java-api-reference.md](../../docs/en/java-api-reference.md) — Complete API reference
- [docs/en/java-maven-central.md](../../docs/en/java-maven-central.md) — Maven Central usage
- [docs/en/java-sdk-rpc.md](../../docs/en/java-sdk-rpc.md) — JSON-RPC protocol

### Maintainer / Roadmap
- [sdk/java/IMPLEMENTATION_STATUS.md](../../sdk/java/IMPLEMENTATION_STATUS.md)
- [sdk/java/VERIFICATION.md](../../sdk/java/VERIFICATION.md) — Test matrix
- [sdk/java/MAVEN_RELEASE_CHECKLIST.md](../../sdk/java/MAVEN_RELEASE_CHECKLIST.md)
- [integrations-selenium-java.md](../../docs/integrations-selenium-java.md) — WebDriver roadmap
- [sdk-strategy.md](../architecture/sdk-strategy.md)

### Related Features
- [F000: Core Platform API](./F000-core-platform-api.md)
- [F008: Playwright Adapter](./F008-playwright-adapter.md)
- [F013: TypeScript SDK](./F013-typescript-sdk.md)
