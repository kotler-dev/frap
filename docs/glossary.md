# Fletta Glossary

Терминологический словарь Fletta — единые определения для всей документации, кода и коммуникаций.

---

## Core Concepts (ядро)

### Discover
**Определение:** Процесс автоматического извлечения полной структуры UI из platform-specific source и преобразования её в element map.

**Детали:**
- Input: URL или platform handle (CDP session, Android ViewTree, etc.)
- Process: tree extraction → filtering (interactive elements) → signature computation → clustering
- Output: structured element map (JSON)

**Пример:**
```bash
frap discover --url https://shop.example.com
# Output: element-map.json с интерактивными элементами, clusters, confidence scores
```

**Отличие от:**
- "Scrape" — discover не извлекает контент, а структуру элементов
- "Record" — discover без действий пользователя, статический анализ
- "Parse" — discover включает clustering и signature generation, не просто DOM parsing

---

### Element Map
**Определение:** Результат discover — структурированное, platform-agnostic представление UI элементов с stable IDs и confidence scores.

**Структура:**
```typescript
interface ElementMap {
  elements: ElementNode[];           // Все интерактивные элементы
  clusters: Cluster[];                // Группы схожих элементов
  confidenceScores: Map<string, number>; // Устойчивость каждого ID
  metadata: {
    url: string;
    timestamp: number;
    source: 'chrome' | 'playwright' | 'android';
    coverage: number;                 // % интерактивных элементов
  };
}

interface ElementNode {
  id: string;                         // Stable ID (генерируется core)
  signature: Signature;               // Устойчивые атрибуты
  clusterId?: string;                 // Принадлежность к cluster
  confidence: number;                 // 0.0 - 1.0
}
```

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
**Определение:** Набор устойчивых атрибутов элемента, используемых для stable identification при изменениях UI.

**Компоненты signature:**
```rust
struct Signature {
  role: ElementRole,           // button, link, input, heading, etc.
  attributes: HashMap<String, String>,  // data-testid, aria-label, type, etc.
  text_hash: Option<String>,   // Hash видимого текста (normalized)
  structural_path: Vec<u32>,   // Позиция относительно parent (resilient)
  visual_hints: Vec<VisualHint>, // CSS-based (optional, enhancement)
}
```

**Устойчивость:** Signature выбирается так, чтобы переживать типичные рефакторинги (смена CSS классов, перестановка в DOM) но быть чувствительным к семантическим изменениям.

---

### Cluster
**Определение:** Группа элементов с похожими signatures, идентифицируемая как повторяющаяся структура (например, карточки товаров, пункты меню).

**Пример:**
```typescript
interface Cluster {
  id: string;
  type: 'list' | 'grid' | 'navigation' | 'form' | 'other';
  elements: string[];           // Element IDs
  pattern: SignaturePattern;   // Общая signature элементов
  count: number;
}

// Пример: cluster "product-cards" с 12 элементами
{
  id: "cl-7f3a9b",
  type: "grid",
  elements: ["el-1", "el-2", ..., "el-12"],
  pattern: { role: "link", has: ["image", "heading", "price"] },
  count: 12
}
```

**Алгоритм:** Hierarchical clustering на основе Drain3 (token-based similarity).

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
**Определение:** Роль Fletta в AI-ecosystem — предоставление структурированного, стабильного доступа к UI для LLM-агентов.

**Метафора:**
> **Fletta даёт AI-агенту надёжные руки и глаза**
- **Глаза (eyes):** element map вместо скриншотов/DOM
- **Руки (hands):** deterministic resolution для стабильных действий
- **Память (memory):** audit trail для анализа behavior drift

**Не делает:**
- Fletta ≠ AI testing tool (не генерирует тесты из требований)
- Fletta ≠ LLM orchestrator (не управляет диалогом)
- Fletta ≠ prompt engineering framework

---

### MCP (Model Context Protocol)
**Определение:** JSON-RPC протокол для интеграции Fletta tools с LLM-агентами.

**Fletta MCP tools:**
| Tool | Input | Output | Purpose |
|------|-------|--------|---------|
| `frap/discover` | URL | element map | Structure extraction |
| `frap/resolve` | signature | element + confidence | Stable execution |
| `frap/analyze` | run ID | RCA report | Drift detection |

**Важно:** Fletta предоставляет tools через MCP, но не управляет workflow агента.

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
| **AI-powered testing** | **AI-ready grounding layer** | Fletta doesn't use AI in core |
| **ML algorithm** | **Deterministic clustering** | Explicit NO ML in core |
| **Test generation** | **Code generation from element map** | Fletta doesn't generate tests directly |

---

## Cross-References

- [positioning.md](./positioning.md) — стратегическое использование терминов
- [architecture/platform-agnostic-core.md](../project/architecture/platform-agnostic-core.md) — технические детали core
- [strategy.md](./strategy.md) — как концепции map к 3 слоям
- [api-reference.md](./api-reference.md) — programmatic использование

---

*Версия: 1.0.0 (MVP)*
*Последнее обновление: 2026-05-23*
