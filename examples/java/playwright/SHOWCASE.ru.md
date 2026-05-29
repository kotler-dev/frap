# Живое демо Frap (Java + отчёты)

Краткий сценарий для презентации: что делает библиотека, как увидеть healing и «кластеры» в отчёте.

## Два слайда

**Слайд 1 — идея.** Тест хранит «память» элемента (signature: путь в DOM, `data-testid`, текст). После рефакторинга разметки старый селектор ломается. Frap снимает снимок интерактивных узлов, сравнивает сигнатуры, ранжирует кандидатов и подставляет **новый стабильный селектор** — тест проходит без ручного фикса.

**Слайд 2 — кластеризация (для людей).** Это не «карта всей страницы», а **группировка похожих кандидатов** по структурному префиксу (`button:submit > div:- > …`). В `frap-debug.html` видно: шаги → блок **DOM clusters** → топ кандидатов с confidence → итог heal / reject.

## Healing ≠ кластеризация

| | Self-healing | «Кластеры» в отчёте |
|---|--------------|---------------------|
| Что делает | Ищет элемент, похожий на записанный, и подставляет селектор при высокой уверенности | Группирует **уже найденных** кандидатов по `signature.prefix` — объяснение решения |
| Где в коде | Один Rust `heal()` для Java и TS | Построение view после heal (`buildClusterViews`) |

Алгоритм Drain3 / ParseTree для полного discovery: [project/architecture/clustering.md](../../../project/architecture/clustering.md).

## Два сценария (показывать парой)

| Цель | Страница / тест | Что увидеть |
|------|-----------------|-------------|
| Умный PASS | `schedule-heal.html` — тест ищет `talk-open-healing`, на странице `talk-card-open-healing` | `ScheduleHealingTest` → heal OK, новый селектор |
| Умный отказ | CFP — две кнопки «Отправить», сломанный `cfp-submit-missing` | `CfpAmbiguousHealTest` → `healed == false`, ≥2 кандидата |

Кейсы: **CONF-SH-SCHED-PASS**, **CONF-SH-CFP-FAIL** — [project/cases/conference/CASES.md](../../project/cases/conference/CASES.md).

## Прогон Java

```bash
./scripts/run-java-e2e.sh
```

Открыть отчёт:

```bash
open examples/java/playwright/target/frap-reports/conference/frap-debug.html
```

В HTML: **DOM clusters**, **Top candidates**, **Healing decision**.

Одна фраза на демо: *«Мы не угадываем локаторы по всей странице — ищем элемент, похожий на записанный, и выбираем лучший по score»*.

## TypeScript (тот же движок, те же страницы)

Из корня репозитория:

```bash
node fixtures/fixtureconf/server.js &
./scripts/test.sh conference-dbg
open e2e/frap-reports/conference/frap-debug.html
```

## Что не обещать

| Не говорить | Как правильно |
|-------------|----------------|
| «Открыл сайт — получил все локаторы» | Локаторы задаёт тест; Frap **подменяет** при поломке |
| «Кластеризация = автогенерация Page Object» | Кластеры в отчёте — **объяснение** выбора кандидата |
| «Frap всегда чинит тест» | При двух равных кандидатах — **отказ** (ambiguous) |

## Питч за 5 минут

1. В браузере: `http://localhost:3000/conference/schedule-heal.html` — что «сломали» в разметке.
2. `frap-debug.html` — как думал движок (schedule, затем CFP).
3. Зелёный / красный тест в IDE.

## Одна фраза

> Кластеризация в отчёте — «похожие элементы в одной группе, чтобы не кликнуть случайную кнопку». Self-healing — «найти похожий на записанный и подставить селектор, если уверенность высокая». Показывайте пару **schedule-heal (PASS)** + **CFP (FAIL)** и **`frap-debug.html`**.
