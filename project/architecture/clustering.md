# Кластеризация DOM: Drain3-подход

Детальное описание алгоритма кластеризации DOM-элементов, адаптированного из Drain3 (кластеризация логов).

**Краткий обзор (зачем, ClusterType, discover → filter):** [docs/clustering.md](../../docs/clustering.md).

## Зачем кластеризация (кратко)

- **Кластер** — группа элементов с одной структурной сигнатурой (один шаблон).
- **ClusterType v1.0.0:** `SINGLE` (1 элемент) и `LIST` (≥ 2 похожих).
- **Не замена discover:** SnapshotBuilder сужает DOM до интерактивных и помеченных узлов; кластеризация группирует похожее для генерации PO, healing и `FilterSpec` (`min_cluster_size`, тип LIST).

## Принцип работы

Drain3 строит дерево фиксированной глубины для кластеризации лог-строк. Мы адаптируем этот подход к DOM:

```
DOM-дерево → [Парсер сигнатур] → [Дерево кластеров] → [PO модель]
                    ↑                                   ↓
            [MutationObserver] ← [изменения DOM] → [инкрементальное обновление]
```

## 1. Построение сигнатуры элемента

### DOMSignature

Каждый элемент получает "сигнатуру" — путь из токенов фиксированной глубины:

```rust
pub struct DOMSignature {
    pub prefix: String,           // Первые N токенов пути (ключ кластера)
    pub tokens: Vec<DOMToken>,    // Все токены пути
    pub depth: u8,                // Глубина узла в DOM
    pub children_hash: u64,       // Хеш структуры детей
}

pub struct DOMToken {
    pub tag: String,              // Имя тега (div, button, input)
    pub role: Option<String>,     // Семантическая роль (form, navigation)
    pub semantic_type: Option<String>, // Тип (TEXT_INPUT, SUBMIT_BUTTON)
    pub structural_class: Option<String>, // Структурный класс
    pub depth: u8,                // Глубина токена в пути
}
```

### Извлечение токенов

```rust
fn extract_tokens(element: &Element, max_depth: usize) -> Vec<DOMToken> {
    let mut tokens = Vec::new();
    let mut current = element;
    let mut depth = 0;
    
    while let Some(el) = current.parent_element() && depth < max_depth {
        let tag = el.tag_name().to_lowercase();
        
        // Приоритетные атрибуты как ключевые слова
        let role = extract_role(&el);
        let semantic_type = detect_semantic_type(&el);
        let structural_class = find_structural_class(&el);
        
        tokens.push(DOMToken {
            tag,
            role,
            semantic_type,
            structural_class,
            depth: depth as u8,
        });
        
        current = el;
        depth += 1;
    }
    
    tokens.reverse(); // От корня к элементу
    tokens
}
```

### Глубина дерева (MAX_DEPTH)

- **MAX_DEPTH = 4-5** — как в Drain3
- Первые N токенов — префиксная сигнатура для кластеризации
- Остальные токены — вариативная часть внутри кластера

## 2. Дерево кластеров

### ParseTree структура

```rust
pub struct ParseTreeRoot {
    children: HashMap<String, ParseTreeNode>,
}

pub struct ParseTreeNode {
    token_value: String,
    depth: u8,
    children: HashMap<String, ParseTreeNode>,
    clusters: Vec<ClusterNode>,
}

pub struct ClusterNode {
    prefix_signature: String,
    elements: Vec<ClusteredElement>,
    signature_template: DOMSignature, // Общая структура кластера
}
```

### Поиск или создание кластера

```rust
fn find_or_create_cluster(signature: &DOMSignature) -> &mut ClusterNode {
    let mut current = &mut self.root;
    
    // Идем по дереву по первым 5 токенам
    for i in 0..min(signature.tokens.len(), 5) {
        let token = &signature.tokens[i];
        let key = format!("{}:{}", token.tag, token.role.as_deref().unwrap_or("-"));
        
        if !current.has_child(&key) {
            current.add_child(key.clone(), ParseTreeNode::new(&key, i as u8));
        }
        
        current = current.get_child_mut(&key).unwrap();
    }
    
    // Ищем лучший кластер или создаем новый
    match find_best_cluster(current, signature) {
        Some(cluster) => cluster,
        None => {
            let new_cluster = ClusterNode::new(signature.prefix.clone());
            current.add_cluster(new_cluster);
            current.clusters.last_mut().unwrap()
        }
    }
}
```

### Алгоритмический поиск лучшего кластера

```rust
fn find_best_cluster(node: &ParseTreeNode, signature: &DOMSignature) -> Option<&mut ClusterNode> {
    let mut best_score = 0.0;
    let mut best_cluster = None;
    
    for cluster in &node.clusters {
        let score = calculate_similarity(&cluster.signature_template, signature);
        
        // Порог как в Drain3: 0.7 для схожести
        if score > best_score && score > 0.7 {
            best_score = score;
            best_cluster = Some(cluster);
        }
    }
    
    best_cluster
}
```

## 3. Вычисление схожести сигнатур

### Алгоритм без ML

```rust
fn calculate_similarity(a: &DOMSignature, b: &DOMSignature) -> f64 {
    // 1. Схожесть пути через Levenshtein distance
    let path_similarity = 1.0 - levenshtein_distance(&a.prefix, &b.prefix) as f64
        / max(a.prefix.len(), b.prefix.len()) as f64;
    
    // 2. Структурная схожесть детей
    let structural_similarity = compare_children_structure(
        a.children_hash, 
        b.children_hash
    );
    
    // 3. Схожесть токенов (LCS)
    let token_similarity = lcs_similarity(&a.tokens, &b.tokens);
    
    // Взвешенная комбинация
    0.5 * path_similarity + 0.3 * token_similarity + 0.2 * structural_similarity
}
```

### Levenshtein distance для путей

```rust
fn levenshtein_distance(a: &str, b: &str) -> usize {
    let len_a = a.chars().count();
    let len_b = b.chars().count();
    let mut matrix = vec![vec![0; len_b + 1]; len_a + 1];
    
    for i in 0..=len_a { matrix[i][0] = i; }
    for j in 0..=len_b { matrix[0][j] = j; }
    
    for (i, ca) in a.chars().enumerate() {
        for (j, cb) in b.chars().enumerate() {
            let cost = if ca == cb { 0 } else { 1 };
            matrix[i + 1][j + 1] = *[
                matrix[i][j + 1] + 1,      // deletion
                matrix[i + 1][j] + 1,      // insertion
                matrix[i][j] + cost,       // substitution
            ].iter().min().unwrap();
        }
    }
    
    matrix[len_a][len_b]
}
```

### LCS (Longest Common Subsequence) для токенов

```rust
fn lcs_similarity(a: &[DOMToken], b: &[DOMToken]) -> f64 {
    let lcs_len = longest_common_subsequence_len(a, b);
    let max_len = max(a.len(), b.len());
    
    lcs_len as f64 / max_len as f64
}

fn longest_common_subsequence_len(a: &[DOMToken], b: &[DOMToken]) -> usize {
    let mut dp = vec![vec![0; b.len() + 1]; a.len() + 1];
    
    for i in 1..=a.len() {
        for j in 1..=b.len() {
            if tokens_match(&a[i-1], &b[j-1]) {
                dp[i][j] = dp[i-1][j-1] + 1;
            } else {
                dp[i][j] = max(dp[i-1][j], dp[i][j-1]);
            }
        }
    }
    
    dp[a.len()][b.len()]
}

fn tokens_match(a: &DOMToken, b: &DOMToken) -> bool {
    a.tag == b.tag && a.role == b.role
}
```

## 4. Результат кластеризации

### Кластеры элементов

```rust
pub struct ClusteringResult {
    pub clusters: HashMap<String, Vec<ClusteredElement>>, // role -> elements
}

pub struct ClusteredElement {
    pub element: Element,
    pub signature: DOMSignature,
    pub cluster: ClusterNode,
}
```

### Примеры кластеров

| Ключ | Элементы | Описание |
|------|----------|----------|
| `INPUT` | все поля ввода | кластер INPUT |
| `BUTTON` | все кнопки | кластер BUTTON |
| `TABLE_ROW` | строки таблицы | кластер ROW |
| `product-card` | карточки товаров | кастомный кластер |

## 5. Динамические элементы (MutationObserver)

### Отслеживание изменений DOM

```rust
pub struct DOMObserver {
    clusterer: DOMElementClusterer,
    clusters: Arc<Mutex<ClusteringResult>>,
}

impl DOMObserver {
    pub fn on_mutation(&mut self, mutation: Mutation) {
        match mutation {
            Mutation::Added(node) => {
                let signature = self.build_signature(&node);
                let cluster = self.find_or_create_cluster(&signature);
                
                // Если кластер новый — добавить поле в PO
                if cluster.is_new() {
                    self.emit_event(ClusterEvent::NewCluster(cluster.clone()));
                }
            }
            Mutation::Removed(node) => {
                // Проверить, опустел ли кластер
                if let Some(cluster) = self.find_cluster_by_element(&node) {
                    if cluster.is_empty() {
                        self.emit_event(ClusterEvent::ClusterEmpty(cluster.id()));
                    }
                }
            }
        }
    }
}
```

### Стратегии для React/Vue

1. **Ленивая кластеризация**: элемент попадает в дерево только когда появился в DOM
2. **Предиктивная кластеризация**: анализ контейнера с обработчиком, создание "ожидаемого" кластера

## 6. Стабильность селекторов

### Эвристическая оценка

```rust
fn calculate_stability_score(element: &Element, attribute: &str, dom: &PageDOM) -> f64 {
    let mut score = 0.0;
    
    // Уникальность атрибута на странице
    let occurrences = dom.count_elements_with_attribute(attribute);
    score += if occurrences == 1 { 0.4 } else { 0.1 };
    
    // Динамичность значения
    if let Some(value) = element.get_attribute(attribute) {
        if looks_like_generated(&value) {
            score -= 0.3; // Динамические ID менее стабильны
        }
        if looks_like_semantic(&value) {
            score += 0.2; // Семантические значения стабильнее
        }
    }
    
    // Глубина в DOM (выше = стабильнее)
    let depth = calculate_depth(element);
    score += if depth < 10 { 0.2 } else { 0.0 };
    
    Math::min(1.0, Math::max(0.0, score))
}
```

## 7. Жадный алгоритм покрытия

### SelectorSetOptimizer

```rust
pub struct SelectorSetOptimizer;

impl SelectorSetOptimizer {
    pub fn build_optimal_selectors(&self, elements: &[Element]) -> OptimizedSelectorSet {
        let element_selectors: HashMap<Element, Vec<Selector>> = elements
            .iter()
            .map(|e| (*e, self.generate_all_selectors(e)))
            .collect();
        
        let mut covered = HashSet::new();
        let mut selected = Vec::new();
        
        // Жадный выбор: максимум покрытия, минимум селекторов
        while covered.len() < elements.len() {
            let mut best_selector = None;
            let mut best_score = 0.0;
            let mut best_new_coverage = HashSet::new();
            
            for (element, selectors) in &element_selectors {
                if covered.contains(element) { continue; }
                
                for selector in selectors {
                    let new_coverage = self.find_covered_elements(selector, &element_selectors);
                    let new_count = new_coverage.difference(&covered).count();
                    let score = new_count as f64 * selector.stability();
                    
                    if score > best_score {
                        best_score = score;
                        best_selector = Some(selector.clone());
                        best_new_coverage = new_coverage;
                    }
                }
            }
            
            if let Some(selector) = best_selector {
                selected.push(selector);
                covered.extend(best_new_coverage);
            } else {
                break;
            }
        }
        
        OptimizedSelectorSet::new(selected, covered.len())
    }
}
```

## Примеры использования

### Пример 1: Кластеризация формы логина

```
DOM путь: form:main → div:form:group → input:text:email
Префикс (ключ кластера): form:main:div:form:group

Элементы в кластере:
- input:text:email (username)
- input:password:password (password)
- button:submit:action (login button)
```

### Пример 2: Кластеризация списка товаров

```
DOM путь: div:main:content → div:products:list → div:product:card
Префикс: div:main:content:div:products:list

Элементы в кластере (12 штук):
- div:product:card + img + h3 + p + button
```

## Интеграция с другими модулями

| Модуль | Использование кластеризации |
|--------|----------------------------|
| `signature` | Извлечение сигнатур для кластеризации |
| `healing` | Поиск альтернативных элементов в том же кластере |
| `generator` | Генерация PO на основе кластеров |
| `learning` | Обновление весов на основе успеха кластеров |
