# Feature: Page Object Generator (F004)

## Meta

- **Epic**: Feature → Developer Experience
- **Roll-up target**: ## v1.2.0 (AI Integration)
- **Status**: draft
- **Target release**: v1.2.0
- **Created**: 2026-05-20
- **Related cases**: C004

## Goal

Автоматическая генерация Page Object из структуры приложения. Генерирует сразу self-healing-селекторы.

## User workflow

1. Пользователь указывает URL или активную сессию браузера
2. Frap анализирует структуру страницы
3. Находит: повторяющиеся элементы, интерактивные элементы, формы
4. Генерирует Page Object с self-healing-селекторами
5. Экспорт в нужный язык (TypeScript/Java/Python)

## Scope

### In
- Анализ DOM-структуры страницы
- Кластеризация повторяющихся элементов (карточки товаров, списки)
- Идентификация интерактивных элементов (кнопки, ссылки, поля ввода)
- Генерация методов для взаимодействия
- Self-healing-селекторы в сгенерированном коде
- Экспорт: TypeScript, Java (Python — позже)

### Out
- Генерация тестов из требований (в F005)
- AI-генерация шагов (это MCP + LLM)
- Визуальное редактирование сгенерированного кода

## Acceptance criteria

- [ ] Генерация из URL: `Frap generate --url ...`
- [ ] Кластеризация: группировка карточек товаров, списков
- [ ] Интерактивные элементы: кнопки, поля, ссылки определяются
- [ ] Сгенерированный код использует `frap:` селекторы
- [ ] TypeScript экспорт: `.page.ts` файлы
- [ ] Java экспорт: Page Object классы
- [ ] C004: сгенерированный код компилируется и работает

## Implementation notes (sketch)

### Модули
```
crates/generator/
├── src/
│   ├── analyzer.rs      # Анализ DOM-структуры
│   ├── clustering.rs    # Кластеризация элементов
│   ├── templates.rs     # Шаблоны для генерации
│   └── export/
│       ├── typescript.rs
│       └── java.rs
```

### Алгоритм анализа

#### 1. Запись через CDP (Chrome DevTools Protocol)

```rust
pub struct CDPRecorder {
    session: CDPSession,
    actions: Vec<UserAction>,
    page_states: Vec<PageState>,
}

impl CDPRecorder {
    pub fn start_recording(&mut self) {
        // Включаем домены CDP
        self.session.send("Page.enable");
        self.session.send("DOM.enable");
        self.session.send("Runtime.enable");
        
        // Инжектируем скрипт для перехвата событий
        let script = r#"
            (function() {
                document.addEventListener('click', function(e) {
                    console.log('__FRAP_ACTION__', JSON.stringify({
                        type: 'click',
                        target: extractInfo(e.target),
                        timestamp: Date.now()
                    }));
                }, true);
                
                document.addEventListener('input', function(e) {
                    if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') {
                        console.log('__FRAP_ACTION__', JSON.stringify({
                            type: 'fill',
                            target: extractInfo(e.target),
                            value: e.target.value,
                            timestamp: Date.now()
                        }));
                    }
                });
            })()
        "#;
        
        self.session.send("Page.addScriptToEvaluateOnNewDocument", 
            json!({ "source": script }));
    }
    
    pub fn capture_page_state(&mut self) {
        let dom = self.session.send("DOM.getDocument");
        let har = self.session.send("Network.getHAR"); // если доступно
        
        self.page_states.push(PageState {
            dom_snapshot: dom,
            har_data: har,
            timestamp: now(),
        });
    }
}
```

#### 2. Определение типа элемента (ElementTypeDetector)

Алгоритмический подход без ML:

```rust
pub struct ElementTypeDetector {
    rules: Vec<TypeDetectionRule>,
}

impl ElementTypeDetector {
    pub fn detect(&self, element: &Element) -> ElementType {
        // Применяем правила по порядку
        for rule in &self.rules {
            if rule.matches(element) {
                return rule.result_type;
            }
        }
        
        // Эвристики для неизвестных
        self.heuristic_detection(element)
    }
    
    fn heuristic_detection(&self, element: &Element) -> ElementType {
        if element.has_event_listeners() {
            if element.is_clickable() { return ElementType::CLICKABLE; }
            if element.is_inputable() { return ElementType::INPUT; }
        }
        
        if element.children_count() > 3 {
            return ElementType::CONTAINER;
        }
        
        // CSS-анализ
        let display = element.css_value("display");
        if display == "flex" || display == "grid" {
            return ElementType::LAYOUT_CONTAINER;
        }
        
        ElementType::UNKNOWN
    }
}

// Правила определения типов
static DEFAULT_RULES: &[TypeDetectionRule] = &[
    // Формы
    TypeDetectionRule::new()
        .when_tag("input")
        .when_attr("type", &["text", "email", "number"])
        .then(ElementType::TEXT_INPUT),
    
    TypeDetectionRule::new()
        .when_tag("input")
        .when_attr("type", &["password"])
        .then(ElementType::PASSWORD_INPUT),
    
    TypeDetectionRule::new()
        .when_tag("input", "button")
        .when_attr("type", &["submit"])
        .then(ElementType::SUBMIT_BUTTON),
    
    TypeDetectionRule::new()
        .when_tag("button")
        .then(ElementType::BUTTON),
    
    // Навигация
    TypeDetectionRule::new()
        .when_tag("a")
        .when_has_attr("href")
        .then(ElementType::LINK),
    
    TypeDetectionRule::new()
        .when_tag("nav", "ul", "ol")
        .when_contains("a", "li")
        .then(ElementType::NAVIGATION),
    
    // Таблицы и списки
    TypeDetectionRule::new()
        .when_tag("table")
        .then(ElementType::TABLE),
    
    TypeDetectionRule::new()
        .when_tag("tr")
        .when_parent("table", "thead", "tbody")
        .then(ElementType::TABLE_ROW),
    
    TypeDetectionRule::new()
        .when_tag("select")
        .then(ElementType::DROPDOWN),
];
```

#### 3. MutationObserver для динамического контента

```rust
pub struct RecordingObserver {
    recorder: CDPRecorder,
    discovered_elements: HashMap<String, ElementInfo>,
}

impl RecordingObserver {
    pub fn on_mutation(&mut self, mutation: Mutation) {
        match mutation {
            Mutation::Added(element) => {
                let sig = extract_signature(&element);
                let cluster = self.find_or_create_cluster(&sig);
                
                // Если кластер новый — добавить поле в PO
                if cluster.is_new() {
                    let element_info = ElementInfo {
                        id: generate_id(),
                        signature: sig,
                        element_type: detect_type(&element),
                        selectors: generate_selectors(&element),
                    };
                    
                    self.discovered_elements.insert(
                        element_info.id.clone(), 
                        element_info
                    );
                    
                    // Уведомляем о новом элементе
                    self.emit(Event::NewElement(element_info));
                }
            }
            Mutation::Removed(element) => {
                // Проверить, опустел ли кластер
                if let Some(cluster) = self.find_cluster_by_element(&element) {
                    if cluster.is_empty() {
                        self.emit(Event::ClusterEmpty(cluster.id()));
                    }
                }
            }
        }
    }
}
```

#### 4. Полный алгоритм генерации

```
1. Запускаем CDP-запись
2. Пользователь проходит сценарий
3. Перехватываем действия (click, fill, navigate)
4. MutationObserver отслеживает динамические элементы
5. Для каждого взаимодействия:
   a. Определяем тип элемента (ElementTypeDetector)
   b. Извлекаем сигнатуру
   c. Находим/создаём кластер
   d. Генерируем селекторы
6. Группируем элементы по кластерам → Page Object
7. Генерируем методы по типам элементов:
   - TEXT_INPUT → fill{Name}()
   - BUTTON → click{Name}()
   - TABLE → get{Name}Rows()
   - DROPDOWN → select{Name}()
```

### Пример вывода (TypeScript)
```typescript
// Generated by Frap from http://demo-store.local/catalog
export class CatalogPage {
  // Element: product cards (cluster of 12)
  getProductCards() {
    return page.locator('frap:product-card');
  }

  // Element: category filter
  applyFilter(category: string) {
    return page.locator('frap:category-filter').selectOption(category);
  }

  // Element: pagination
  goToPage(n: number) {
    return page.locator(`frap:pagination-page[${n}]`).click();
  }
}
```

### Риски и зависимости
- Качество генерации зависит от структуры приложения
- Динамический контент: нужно ждать загрузки
- Имена методов: не всегда понятны без контекста

## Verification / Test plan

### Manual smoke
```bash
# Генерация для демо-магазина
Frap generate --url "http://demo-store.local/catalog" --output ./pages

# Проверка структуры
ls ./pages/
# Expected: CatalogPage.ts

# Проверка компиляции
cd ./pages && tsc --noEmit

# Проверка работы
# Использовать сгенерированный Page Object в тесте
```

### Automation
- Тесты генерации на фиксированных страницах
- C004: полный сценарий

## Related docs

- [cases.md](../../docs/cases.md) — C004
