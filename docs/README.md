# Fletta Documentation

Структурированная база знаний проекта для разработки, тестирования, позиционирования и коммуникации.

## Структура

```
docs/
├── README.md           # Этот файл — введение
├── index.md            # Связующий индекс (меню, маппинги)
├── positioning.md      # Позиционирование, one-liner, vs Playwright MCP
├── pains.md            # Боли, scope, copy для сайта и конференций
├── audience.md         # ЦА: банк, InSourceHub, сегменты S1–S3
├── benchmark.md        # PoC/MVP gates, CP001–CP005, метрики
├── integrations.md     # Интеграция, не замена (Playwright, JUnit)
├── monetization.md     # OSS + enterprise tiers
├── features.md         # Фичи (F001-F00N) — что строим (см. project/FEATURES.md)
├── cases.md            # Кейсы (C001, CP001...) — как демонстрируем
├── roadmap.md          # Приоритеты и план разработки
└── talk-topics.md      # Темы для выступлений
```

## С чего начать

| Задача | Документ |
|--------|----------|
| Питч, конкуренты, one-liner | [positioning.md](./positioning.md) |
| Боли, сайт, презентация, конференция | [pains.md](./pains.md) |
| Кого целим (банк, команды) | [audience.md](./audience.md) |
| PoC за неделю | [benchmark.md](./benchmark.md) |
| Playwright / JUnit | [integrations.md](./integrations.md) |
| Кластеризация, ClusterType | [clustering.md](./clustering.md) |
| Element Map, Signature, RCA, discover vs CDP | [glossary.md](./glossary.md) |
| Java SDK quick start | [en/java-getting-started.md](./en/java-getting-started.md) |
| Деньги / enterprise | [monetization.md](./monetization.md) |
| Карта фич и кейсов | [index.md](./index.md) |

## Формат

### ID фич: `FXXX`
- F = Feature
- XXX = номер (001, 002...)
- Пример: F001 = Self-Healing Selectors

### ID кейсов: `CXXX` / `CPXXX`
- C = Case (полный сценарий)
- CP = PoC Case (быстрая проверка, см. [benchmark.md](./benchmark.md))

## Использование с Cursor/LLM

### Добавить новую фичу
1. Открой `features.md`
2. Добавь блок по шаблону (см. существующие)
3. Обнови `index.md` — добавь в маппинги

### Добавить новый кейс
1. Открой `cases.md`
2. Добавь блок по шаблону
3. Укажи связанные фичи
4. Обнови `index.md`

### Найти связанный контент
1. Смотри `index.md` → разделы Mapping
2. Или используй теги в поиске

## Принципы

- **Кратко**: 1-2 абзаца на описание, bullet points
- **Связно**: каждый кейс привязан к фичам
- **Действенно**: каждый кейс имеет демо-скрипт
- **Проверяемо**: PoC-кейсы CP* имеют числовые gates в benchmark.md
- **Интеграция**: не заменяем Playwright/Selenium — см. integrations.md

## Для AI-агентов

При работе с этими файлами:
1. Читай `index.md` первым — получишь карту
2. Для продукта/рынка — `positioning.md`, `audience.md`
3. Для реализации PoC — `benchmark.md`, затем `cases.md` CP*
4. Обновляй статусы в `index.md` при изменениях
