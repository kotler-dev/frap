# Feature: Self-Healing Selectors (F001)

## Meta

- **Epic**: Core → Self-Healing
- **Roll-up target**: ## MVP v1.0.0
- **Status**: in-progress
- **Target release**: v1.0.0
- **Created**: 2026-05-20
- **Related cases**: C001, CP001, CP002, CP003

## Goal

Автоматическое восстановление селекторов при изменении UI без ML. Сохраняем primary-селектор, активируем fallback-цепочку только при failure.

## User workflow

1. Пользователь записывает тест с обычным селектором (например, `[data-testid="pay-btn"]`)
2. Разработчики меняют UI (например, `data-testid` становится `checkout-pay`)
3. При следующем запуске fletta не находит элемент по primary-селектору
4. Система ищет по сигнатуре: текст, структура, позиция, стабильные атрибуты
5. Находит наиболее похожий элемент с confidence score
6. Если score ≥ minConfidence — тест проходит (healed), иначе — fail с отчётом

## Scope

### In
- Извлечение сигнатуры элемента (путь + устойчивые атрибуты)
- Алгоритм кластеризации (Drain3-подход к DOM)
- Сравнение сигнатур и расчёт confidence score
- Fallback-цепочка при failure primary-селектора
- Отчёт: diff, score, топ-3 кандидата
- Policy: minConfidence порог (конфигурируемый)

### Out
- Визуальные признаки (в F007)
- Обучение на ошибках (в F009)
- UI + logs + network context (в F002)

## Acceptance criteria

- [x] Реализована сигнатура DOM-элемента (путь + атрибуты)
- [x] Алгоритм сравнения сигнатур работает (WASM/натив)
- [x] CP001 проходит: stable тест не вызывает healing
- [x] CP002 проходит: при смене testid находит элемент
- [x] CP003 проходит: при двух похожих кнопках — безопасный fail
- [x] Отчёт содержит: healed (bool), confidence, diff, top-3 кандидатов
- [x] minConfidence конфигурируется (default: 0.85)

### Implementation Status
| Component | Status | Location |
|-----------|--------|----------|
| Signature extraction | ✅ | `crates/signature/src/lib.rs` |
| Clustering (Drain3) | ✅ | `crates/clustering/src/lib.rs` |
| Healing engine | ✅ | `crates/healing/src/lib.rs` |
| TS SDK bindings | ✅ | `sdk/typescript/src/core.ts` |
| Algorithm tests | ✅ | Unit tests in each crate |
| E2E tests (CP001-CP003) | ✅ | `e2e/*.spec.ts` |

## Implementation notes (sketch)

### Модули Rust core
- `crates/signature/` — извлечение и сравнение сигнатур
- `crates/clustering/` — Drain3-алгоритм для DOM
- `crates/healing/` — orchestration: primary → fallback chain

### Алгоритм сигнатуры
```
signature = {
  path: [tag, tag, ...],           // структурный путь
  stable_attrs: {                  // устойчивые атрибуты
    role?: string,
    type?: string,
    placeholder?: string,
    aria_label?: string,
  },
  text_content?: string,           // текст (для кнопок/ссылок)
  position_in_parent?: number,   // индекс среди siblings
}
```

### Confidence calculation

Используется алгоритмический подход без ML:

#### 1. Схожесть пути (Levenshtein distance)

```rust
fn path_similarity(a: &str, b: &str) -> f64 {
    let distance = levenshtein_distance(a, b);
    1.0 - distance as f64 / max(a.len(), b.len()) as f64
}
```

#### 2. Схожесть токенов (LCS — Longest Common Subsequence)

```rust
fn token_similarity(a: &[DOMToken], b: &[DOMToken]) -> f64 {
    let lcs_len = longest_common_subsequence_len(a, b);
    lcs_len as f64 / max(a.len(), b.len()) as f64
}
```

Токены совпадают если: `tag == tag && role == role`

#### 3. Структурная схожесть

```rust
fn structural_similarity(a: u64, b: u64) -> f64 {
    // Сравнение хешей структуры детей
    if a == b { 1.0 } else { 0.0 }
}
```

#### 4. Финальная формула

```rust
fn calculate_confidence(original: &Signature, candidate: &Signature) -> f64 {
    0.5 * path_similarity(&original.path_string, &candidate.path_string)
        + 0.3 * token_similarity(&original.tokens, &candidate.tokens)
        + 0.2 * structural_similarity(original.children_hash, candidate.children_hash)
        + attribute_bonus(original, candidate)
}

fn attribute_bonus(original: &Signature, candidate: &Signature) -> f64 {
    let mut bonus = 0.0;
    // Точное совпадение текста
    if original.text_content == candidate.text_content {
        bonus += 0.1;
    }
    // Совпадение stable атрибутов
    for (key, value) in &original.stable_attrs {
        if candidate.stable_attrs.get(key) == Some(value) {
            bonus += 0.05;
        }
    }
    bonus
}
```

### Fallback-цепочка

```rust
fn heal_with_fallback(
    primary_selector: &str,
    original: &Signature,
    dom: &DOMSnapshot,
) -> HealResult {
    // 1. Пробуем primary
    if let Some(element) = dom.find(primary_selector) {
        return HealResult::success(primary_selector.to_string(), 1.0);
    }
    
    // 2. Ищем по сигнатуре в том же кластере
    let candidates = find_in_cluster(original, dom);
    let best = candidates.iter().max_by_key(|c| (c.confidence * 100.0) as u32);
    
    if let Some(candidate) = best {
        if candidate.confidence >= MIN_CONFIDENCE {
            return HealResult::healed(
                candidate.selector.clone(),
                candidate.confidence,
                candidates, // top-3 для отчёта
            );
        }
    }
    
    // 3. Fail с отчётом
    HealResult::failed(candidates)
}
```

### Кластеризация для healing

Drain3-подход к DOM кластеризации (детали в [clustering.md](../architecture/clustering.md)):

```rust
// Элементы группируются по префиксу пути (первые 4-5 токенов)
// При healing ищем в том же кластере
let cluster = clustering.find_cluster(&original_signature);
let candidates = cluster.elements.iter()
    .map(|e| Candidate {
        element: e,
        confidence: calculate_confidence(original, &e.signature),
    })
    .filter(|c| c.confidence >= 0.5) // минимальный порог
    .collect();
```

### MutationObserver для динамических элементов

```rust
pub struct DynamicElementHandler {
    observer: MutationObserver,
    pending_elements: Vec<Element>,
}

impl DynamicElementHandler {
    pub fn on_mutation(&mut self, mutation: Mutation) {
        match mutation {
            Mutation::Added(element) => {
                // Ленивая кластеризация: элемент появился → сразу в кластер
                let sig = extract_signature(&element);
                let cluster = clustering.find_or_create_cluster(&sig);
                
                // Проверяем, не искомый ли это элемент
                if self.pending_elements.iter().any(|p| could_match(p, &element)) {
                    self.try_heal_with_new_element(&element);
                }
            }
            Mutation::Removed(element) => {
                // Помечаем кластер как изменившийся
                if let Some(cluster) = clustering.find_cluster_by_element(&element) {
                    cluster.mark_changed();
                }
            }
        }
    }
}
```

### Standalone Usage via F000
Core можно использовать без адаптеров через F000 API:
- **WASM**: `new HealingEngine().heal(...)` в браузере/Node.js
- **FFI**: прямой вызов из Java/Swift/Python через C-API
- **JSON-RPC**: внешние процессы через stdin/stdout

См. [F000: Core Platform API](./F000-core-platform-api.md)

### Риски и зависимости
- Производительность: большие DOM (>10k элементов) — оптимизация через индексацию
- Детерминизм: одинаковые входы → одинаковые результаты (LCS/Levenshtein детерминированы)
- Безопасность: не кликать по неправильному элементу — порог minConfidence + safe elements check

## Verification / Test plan

### Manual smoke
```bash
# CP001: stable test
fletta replay --name "stable-flow"
# Expected: PASSED, healed=false, overhead < 10%

# CP002: refactor heal
docker-compose up -d checkout-v2  # сменили testid
fletta replay --name "payment-flow"
# Expected: PASSED (healed), confidence >= 0.85

# CP003: safe fail
docker-compose up -d checkout-ambiguous  # две кнопки
fletta replay --name "payment-flow" --min-confidence 0.85
# Expected: FAILED, top-3 candidates in report
```

### Automation
- Юнит-тесты: сравнение сигнатур
- Интеграционные: CP001–CP003 в CI
- Бенчмарк: производительность на DOM 10k+ элементов

## Related docs

- [positioning.md](../../docs/positioning.md) — vs конкуренты
- [benchmark.md](../../docs/benchmark.md) — PoC gates
- [cases.md](../../docs/cases.md) — C001, CP001–CP003
