# frap Glossary

Терминологический словарь frap — единые определения для всей документации, кода и коммуникаций.

---

## Core Concepts (ядро)

### Discover
**Определение:** Процесс автоматического извлечения структуры UI и преобразования её в element map.

**Детали:**
- Input: platform handle (Playwright `Page`, в roadmap — CDP session, Android ViewTree, …)
- Process: snapshot → filtering (interactive elements) → signature computation → clustering
- Output: structured element map (JSON)

**Playwright adapter (сейчас):** `Frap.discover(page)` **не использует CDP**. Цепочка:

```
SnapshotBuilder.build() → page.evaluate(...) → client.buildElementMap(...)
```

Снимок собирается in-page JavaScript (интерактивные элементы, path, атрибуты); Core получает `DOMSnapshot` и строит element map.

**CDP (roadmap):** standalone Chrome source (URL или CDP endpoint), запись сценариев через Chrome DevTools Protocol (F004, C009) — **отдельный transport**, тот же Core pipeline (`build_element_map`). См. [project/cases/demo/C009-recording-cdp.md](../project/cases/demo/C009-recording-cdp.md).

**Пример:**
```java
ElementMap map = Frap.discover(page);
```

**Отличие от:**
- "Scrape" — discover не извлекает контент, а структуру элементов
- "Record" — discover без действий пользователя, статический анализ
- "Parse" — discover включает clustering и signature generation, не просто DOM parsing
- "RCA" — discover строит карту UI; `analyze_rca` разбирает падение по timeline (см. ниже)

---

### Element Map
**Определение:** Результат discover — не «карта сайта», а **структурированный каталог UI-элементов страницы**: сигнатуры, confidence, рекомендованные локаторы и кластеры.

**Структура (v1.0.0):** два **плоских** списка — не дерево и не граф в ответе API.

```typescript
interface ElementMap {
  elements: ElementNode[];    // каталог элементов
  clusters: Cluster[];      // группы; связь через cluster_id
  metadata: MapMetadata;
}

interface ElementNode {
  id: string;               // ключ каталога в этом discover (el-0, el-1, …)
  selector: string;         // селектор на момент снимка
  recommended_selector: string;
  tag: string;
  signature: Signature;     // структура + устойчивые атрибуты
  cluster_id?: string;
  confidence: number;
  locator: LocatorRecommendation;
}
```

**Навигация:** перебор / stream / filter по `elements` и `clusters`. Между `ElementNode` **нет** полей `parent_id` / `children` — иерархия не материализована как ссылки.

**Где тогда структура DOM?** В `signature.path` каждого элемента: при снятии snapshot адаптер поднимается от узла к `body` и записывает цепочку токенов `tag:role`. Этого достаточно для matching и кластеризации без дерева в Element Map.

**`el-N` vs «тот же элемент после изменения UI»:** ID в element map — **внутренний ключ одного прогона discover**, не стабильный идентификатор на годы. Чтобы узнать «тот же» элемент после смены `id` / разметки, используется **сохранённая сигнатура** (при bind локатора через adapter) и алгоритм resolution/heal: сравнение с кандидатами в **новом** snapshot, сначала в том же кластере.

**Назначение:**
- Input для code generation (PageObject, тесты)
- Grounding layer для AI-агентов (structured vision)
- Baseline для drift detection

---

### Source
**Определение:** Platform-specific extractor, который предоставляет raw element tree для core processing.

**Существующие sources:**
| Source | Platform | Input | Notes |
|--------|----------|-------|-------|
| `chrome` | Web | URL or CDP endpoint | MVP, direct CDP |
| `playwright` | Web | Playwright Page object | Optional adapter |
| `android` | Android | UIAutomator | v2 roadmap |
| `ios` | iOS | XCUITest | v2 roadmap |

**Интерфейс (pseudo-code):**
```rust
trait Source {
  fn capture(&self) -> Result<RawElementTree, Error>;
}
```

**Важно:** Source не делает clustering или signature generation — это core responsibility.

---

### Signature
**Определение:** Структурный «отпечаток» элемента для matching при изменениях UI. Записывается в snapshot и копируется в element map / heal request.

**Компоненты (реализация v1.0.0):**
```rust
struct Signature {
  path: Vec<DOMToken>,              // цепочка tag:role от элемента к body
  prefix: String,                    // первые токены path (для кластеризации)
  stable_attrs: HashMap<String, String>, // data-testid, id, data-id, aria-*, name, …
  text_content: Option<String>,      // видимый текст (кнопки, ссылки)
  position_in_parent: Option<usize>, // индекс среди siblings с тем же tag
  children_hash: u64,                // зарезервировано под structural hash детей
  depth: u8,
}
```

**`path` vs parent-child в Element Map:** дерево в ответе discover не строится, но **путь уже снят** в момент snapshot. При heal сравниваются `path` (Levenshtein + LCS по токенам), `stable_attrs`, `text_content`, `position_in_parent`, плюс бонусы/штрафы (например, миграция `id` → `data-id` с тем же значением).

**`position_in_parent` — это не `:nth-child()` в локаторе.** SnapshotBuilder считает индекс элемента среди **однотипных** siblings (`parent.children` с тем же `tagName`). В `recommended_selector` nth-child **не** подставляется — приоритет: `data-testid` → `id` → `data-id` → `name` → fallback. Позиция используется как **+0.08 к confidence** при сопоставлении сигнатур, когда уникальный атрибут пропал.

**Пример (id → data-id):** было `li#2`, стало `li[data-id='2']`. Кластер списка сохраняется (тот же `prefix_signature`). Heal находит кандидата по path + `position_in_parent` + bonus за перенос значения идентификатора. Контракт: `fixtures/contract/clustering-id-migration/`.

**Устойчивость:** сигнатура рассчитана на типичные рефакторинги (смена классов, перенос атрибута) и явный fail при неоднозначности (два почти одинаковых кандидата).

---

### Cluster
**Определение:** Группа элементов с похожей **структурной сигнатурой** — один DOM-шаблон (например, карточки товара, строки таблицы, пункты меню).

**Зачем:** отличить повторяющийся блок (LIST) от уникального элемента (SINGLE); сгенерировать один метод Page Object на шаблон; сузить healing и drift к структурному контексту. Подробнее: [clustering.md](./clustering.md).

**Пример (element map v1.0.0):**
```typescript
interface Cluster {
  id: string;
  cluster_type: 'single' | 'list' | 'unknown';
  element_ids: string[];
  prefix_signature: string;   // общий префикс пути элементов кластера
}

// 12 карточек товара — один LIST-кластер
{
  id: "cl-7f3a9b",
  cluster_type: "list",
  element_ids: ["el-1", "el-2", ..., "el-12"],
  prefix_signature: "div:main:div:products:list:div:product:card"
}
```

**Алгоритм:** детерминированная кластеризация по token-based similarity (Drain3-подход). Discover сначала сужает выборку (`SnapshotBuilder`: интерактив + `data-testid` и т.п.), затем Core группирует по сигнатурам.

---

### ClusterType
**Определение:** Классификация кластера в element map (v1.0.0).

| Значение | Условие | Пример |
|----------|---------|--------|
| `single` | 1 элемент в кластере | Уникальная кнопка Submit |
| `list` | ≥ 2 элемента с похожей сигнатурой | Карточки каталога, строки таблицы |
| `unknown` | резерв | — |

**Использование:** фильтрация при PO generation (`filter(c -> c.clusterType() == LIST)`), `FilterSpec.min_cluster_size` для отбора только повторяющихся блоков.

---

## Context Layer (v1.1+)

### ContextTimeline
**Определение:** Единый **временной ряд** событий вокруг прогона теста — агрегация трёх плоскостей контекста (tri-plane):

| Плоскость | События в timeline |
|-----------|-------------------|
| **UI** | клики, failures локаторов, навигация |
| **Network** | HTTP запросы/ответы, тайминги, ошибки |
| **Logs** | console (info/warn/error), page errors |

События коррелируются по `timestamp_ms` и `trace_id`. Адаптер (Playwright) собирает поток при `captureAll(true)`; Core не ходит в браузер — получает готовый JSON.

```typescript
interface ContextTimeline {
  events: ContextEvent[];  // kind: "ui" | "network" | "console" | "log"
}
```

**Связанная фича:** [F002: Unified Context](../project/feature/F002-unified-context.md).

---

### RCA (Root Cause Analysis)
**Определение:** **Post-mortem** разбор таймлайна контекста вокруг момента падения. Core классифицирует вероятную причину и отдаёт `RcaReport`.

**RPC-метод:** `analyze_rca` → Java: `FrapCoreClient.analyzeRca(ContextTimeline, failureAtMs)`.

**Процесс:**
1. Тест падает; adapter отдаёт `ContextTimeline` (окно ±N сек от failure)
2. Core анализирует корреляцию UI failure с network/logs
3. Классификация: `UI_CHANGE` | `NETWORK_ERROR` | `TIMING_ISSUE` | `UNKNOWN`
4. `RcaReport`: primary cause, confidence, timeline excerpt, recommendation

**Это не:**
- **Healing** — RCA не ищет новый локатор
- **Discover** — RCA не строит element map
- **Drift detection** — RCA про один прогон и момент падения, не baseline vs текущая страница

**Пример сценария:** API timeout перед UI failure → RCA: `NETWORK_ERROR`, рекомендация не «лечить» селектор. Кейс C002.

**Связанная фича:** [F003: RCA](../project/feature/F003-rca.md).

---

## Resolution (formerly "Healing")

### Resolution
**Определение:** Внутренняя механика поиска элемента при несовпадении original signature с текущим UI (изменение структуры).

**Процесс:**
1. Original signature не найден (drift detected)
2. Core выполняет signature matching против current element map
3. Ranking candidates по similarity score
4. Возврат top-N candidates с confidence scores
5. Если confidence ≥ threshold — возврат best match
6. Если confidence < threshold — explicit failure для review

**Важно:** Resolution — internal mechanism, не user-facing feature. Пользователь видит element map обновления или drift report.

**Отличие от "healing":**
- "Healing" — user-centric термин ("мы починили ваш тест")
- "Resolution" — technical term ("алгоритм matching сигнатур")

---

### Drift
**Определение:** Изменение UI структуры между baseline element map и текущим состоянием.

**Типы drift:**
| Type | Description | Example |
|------|-------------|---------|
| **Element Drift** | Element signature изменился | Button text с "Buy" на "Purchase" |
| **Structural Drift** | Добавлены/удалены элементы | Новая форма в checkout flow |
| **Cluster Drift** | Cluster pattern изменился | Карточка товара добавила "rating" |

**Detection:**
```bash
frap analyze --url https://shop.example.com \
  --against element-map-baseline.json
# Output: drift-report.json
```

**Связанная фича:** [F017: Structural Contract](../project/feature/F017-structural-contract.md) — drift detection как CI gate с explainable diff.

---

### Structural Contract
**Определение:** Подход к валидации UI через структурные инварианты: baseline element map + policy + drift gate. Не pixel diff, а проверка «страница устроена как в требованиях».

**Компоненты:**
| Component | Description | Phase |
|-----------|-------------|-------|
| **Baseline** | Фиксированная element map или signatures критичных зон | ✅ v1 (element-level) / ❌ v2 (page-level) |
| **Policy** | Декларативные инварианты (cluster_exists, element_unique) | ❌ v2+ (F017.3) |
| **Drift Engine** | Сравнение текущего UI с baseline | ❌ v2 (F017.2) |
| **Gate** | CI assertion: fail при unexpected drift | ✅ v1.2 (F017.1) |

**Отличие от visual regression:**
| Aspect | Visual regression | Structural Contract |
|--------|-------------------|---------------------|
| Сравниваем | Пиксели, скриншоты | DOM-структура, кластеры |
| Типичный fail | «Красный пиксель» | «Пропал блок payment-methods» |
| Объяснимость | PNG diff | drift-report.json с severity |

**См.:** [docs/structural-contract.md](./structural-contract.md) — матрица доступности и anti-pitch.

---

### Confidence Score
**Определение:** Числовая оценка (0.0 — 1.0) устойчивости идентификатора или качества matching.

**Использование:**
- Element map generation: устойчивость stable ID
- Resolution: качество candidate matching
- Drift detection: степень изменения

**Thresholds:**
| Score | Meaning | Action |
|-------|---------|--------|
| ≥ 0.9 | Very stable | Automatic acceptance |
| 0.7 — 0.9 | Stable | Recommend with review |
| 0.5 — 0.7 | Ambiguous | Explicit review required |
| < 0.5 | Unstable | Reject, manual intervention |

---

## Architecture Concepts

### Core
**Определение:** Platform-agnostic deterministic engine (Rust/WASM) для structure analysis. **NO ML, NO LLM dependencies.**

**Ответственности:**
- Signature computation
- Clustering (Drain3)
- Resolution (matching algorithm)
- Element map generation

**НЕ делает:**
- Browser automation
- LLM calls
- Code generation (это downstream tools)

**Сборка:**
```toml
# crates/core/Cargo.toml
[dependencies]
# NO tensorflow, NO ort, NO llm-chain
serde = "1.0"  # только сериализация
regex = "1"     # для Drain3
```

---

### Source Adapter
**Определение:** Platform-specific реализация `Source` trait для предоставления raw tree в core.

**Пример:**
```rust
// crates/sources/web-chrome/src/lib.rs
impl Source for ChromeSource {
  fn capture(&self, url: &str) -> Result<RawElementTree> {
    // CDP calls to get DOM + computed styles
    // Return platform-agnostic RawElementTree
  }
}
```

---

### Enhancement Adapter
**Определение:** Optional плагин для ML/LLM-based features (semantic naming, visual matching, step generation).

**Отличие от Source Adapter:**
- Source adapter → input для core (обязательный)
- Enhancement adapter → post-processing core output (опциональный)

**Структура:**
```
frap-enhancements/
  ├── semantic-naming/      # LLM-based method names
  ├── visual-matching/      # OpenCV image features
  └── step-generation/      # AI test generation
```

**Политика:** Core работает без enhancements (NoOp default). Enhancements — M3+, separate package.

---

## AI Integration Concepts

### Grounding Layer
**Определение:** Роль frap в AI-ecosystem — предоставление структурированного, стабильного доступа к UI для LLM-агентов.

**Метафора:**
> **frap даёт AI-агенту надёжные руки и глаза**
- **Глаза (eyes):** element map вместо скриншотов/DOM
- **Руки (hands):** deterministic resolution для стабильных действий
- **Память (memory):** audit trail для анализа behavior drift

**Не делает:**
- frap ≠ AI testing tool (не генерирует тесты из требований)
- frap ≠ LLM orchestrator (не управляет диалогом)
- frap ≠ prompt engineering framework

---

### MCP (Model Context Protocol)
**Определение:** JSON-RPC протокол для интеграции frap tools с LLM-агентами.

**frap MCP tools:**
| Tool | Input | Output | Purpose |
|------|-------|--------|---------|
| `frap/discover` | URL | element map | Structure extraction |
| `frap/resolve` | signature | element + confidence | Stable execution |
| `frap/analyze` | run ID | RCA report | Drift detection |

**Важно:** frap предоставляет tools через MCP, но не управляет workflow агента.

---

## Enterprise Concepts

### Audit Trail
**Определение:** Immutable log всех resolution attempts и drift detection events для compliance и debugging.

**Структура (core fields):**
```rust
struct ResolutionAttempt {
  timestamp: u64,
  element_id: String,
  original_signature: Signature,
  candidates: Vec<Candidate>,
  selected: Option<String>,
  // enterprise extensions (optional):
  policy_override: Option<String>,
  approver: Option<UserId>,
}
```

---

### Policy
**Определение:** Набор правил для enterprise-grade control над resolution behavior.

**Примеры:**
- `minConfidence: 0.9` — только high-confidence resolutions
- `requireApproval: true` — manual review для critical paths
- `blockedSelectors: [...]` — never resolve to these elements

**Implementation:**
```rust
trait PolicyChecker {
  fn approve_resolution(&self, candidate: &Element) -> bool;
}
```

**Note:** Policy hooks — placeholder в MVP, полная реализация в Enterprise tier.

---

## Deprecated Terms (не использовать)

| Old Term | New Term | Reason |
|----------|----------|--------|
| **Healing** | **Resolution** | "Healing" implies fixing something broken; "Resolution" is technical matching |
| **Self-healing test** | **Stable test via element map** | Focus on structure, not symptom |
| **AI-powered testing** | **AI-ready grounding layer** | frap doesn't use AI in core |
| **ML algorithm** | **Deterministic clustering** | Explicit NO ML in core |
| **Test generation** | **Code generation from element map** | frap doesn't generate tests directly |

---

## Cross-References

- [positioning.md](./positioning.md) — стратегическое использование терминов
- [architecture/platform-agnostic-core.md](../project/architecture/platform-agnostic-core.md) — технические детали core
- [strategy.md](./strategy.md) — как концепции map к 3 слоям
- [api-reference.md](./api-reference.md) — programmatic использование

---

*Версия: 1.0.0 (MVP)*
*Последнее обновление: 2026-05-23*
