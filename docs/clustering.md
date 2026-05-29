# Кластеризация

Краткий обзор: что такое кластер, зачем нужна кластеризация в Frap и как она связана с discover.

Подробный алгоритм (Drain3, сигнатуры, similarity): [project/architecture/clustering.md](../project/architecture/clustering.md).

---

## Что такое кластер

**Кластер** — группа элементов с похожей **структурной сигнатурой** (один шаблон DOM-пути). Примеры: карточки товара, строки таблицы, пункты меню.

Элементы в кластере делят общий **prefix signature** — префикс пути от корня к элементу (теги, роли, семантические признаки). Core сравнивает сигнатуры детерминированно (Drain3-подход), без ML.

---

## ClusterType (v1.0.0)

В element map каждый кластер помечен типом:

| Тип | Условие | Смысл |
|-----|---------|--------|
| `SINGLE` | 1 элемент | Уникальный блок: одна кнопка, одно поле, один заголовок |
| `LIST` | ≥ 2 похожих элемента | Повторяющийся шаблон: карточки, строки списка, ячейки |
| `UNKNOWN` | резерв | Не классифицирован (редко в текущем pipeline) |

```java
map.clusters().stream()
    .filter(c -> c.clusterType() == ClusterType.LIST)
    .filter(c -> c.elementIds().size() >= 2)
    .forEach(/* один метод items(int index) на кластер */);
```

Roadmap: более детальные типы (`form`, `navigation`, `grid`) — после стабилизации базового контракта.

---

## Зачем не `querySelector('*')` + все xpath

На типичной странице тысячи DOM-узлов. «Взять всё» даёт:

- **Шум** — `div`, обёртки, декоративная разметка; большинство узлов не интерактивны.
- **Нет семантики повторений** — 5 одинаковых карточек и одна уникальная кнопка выглядят одинаково «просто как элементы».
- **Нет рекомендованных локаторов** — нет ranking по стабильности и confidence score.
- **Page Object вручную** — для «5 одинаковых карточек» пришлось бы писать 5 копий метода вместо одного `items(int index)`.

Frap решает это двухэтапно: сначала **сужает выборку**, затем **группирует похожее**.

---

## Element Map: плоский каталог, не дерево

`buildElementMap` возвращает два списка — `elements` и `clusters` — без parent-child ссылок между узлами. Это **каталог** для фильтрации, PO generation и baseline, а не API обхода DOM.

Структурный контекст каждого элемента уже в **`signature.path`** (цепочка `tag:role` от узла к `body`, снимается в SnapshotBuilder). Кластер добавляет контекст «это один шаблон списка». Для повторного поиска того же элемента после смены `id` используется **сигнатура + heal**, а не обход дерева Element Map.

Подробнее: [Glossary: Element Map](./glossary.md#element-map), [Signature](./glossary.md#signature).

---

## Discover → кластеризация

```
Страница → SnapshotBuilder (фильтр) → сигнатуры → кластеризация → Element Map
```

### 1. Discover сужает выборку

`SnapshotBuilder` (Playwright adapter) не обходит все узлы. Берёт интерактивные элементы и узлы с устойчивыми атрибутами:

`button`, `input`, `a`, `select`, `textarea`, `[data-testid]`, `[data-id]`, `li[id]`, `[role="button"]`, …

### 2. Core строит сигнатуры и кластеры

Для каждого элемента — structural signature (путь, теги, роли). Похожие сигнатуры попадают в один кластер. Для каждого элемента — `recommended_selector` и `confidence`.

### 3. Фильтрация element map

`FilterSpec` позволяет отсечь лишнее после построения карты:

- `interactive_only` — только интерактивные теги
- `min_cluster_size` — оставить только кластеры LIST с N+ элементами (типично для PO-генерации)
- `include_tags` — ограничить набор тегов

---

## Зачем это нужно

| Сценарий | Роль кластеризации |
|----------|-------------------|
| **Page Object generation** | LIST → один метод на шаблон (`productCards(int i)`), SINGLE → отдельное поле |
| **Healing** | Поиск кандидата сначала в том же кластере (structural context) |
| **Drift detection** | Изменился состав кластера (добавили поле в карточку) → cluster drift |
| **Фильтрация для CI** | `min_cluster_size: 2` + `LIST` — только повторяющиеся блоки |

---

## Пример: список из 5 карточек

```
Кластер cl-a1b2 (LIST, 5 элементов)
  prefix_signature: div:main:div:products:list:div:product:card
  element_ids: [el-1, el-2, el-3, el-4, el-5]

→ PageObject: List<ProductCard> items() или Product {
    ProductCard item(int index) { ... }
  }
```

Один уникальный submit-кнопка → кластер `SINGLE` → поле `submitButton()`.

---

## См. также

- [Glossary: Cluster](./glossary.md#cluster), [ClusterType](./glossary.md#clustertype)
- [Java getting started — list clusters](./en/java-getting-started.md)
- Контракт: `fixtures/contract/element-map-list/`, `fixtures/contract/clustering-id-migration/`
