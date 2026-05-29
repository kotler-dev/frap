# Тезисы и сообщения для презентаций

Сводка готовых формулировок для питчей, митапов, докладов и внутренних сессий.

**Каталог болей и scope** (сайт, слайды «Проблема», конференция) — **[pains.md](./pains.md)**.

---

## One-liners (выбрать под контекст)

| Контекст | Текст |
|----------|-------|
| **Elevator (RU)** | Движок для извлечения структуры UI: парсит, кластеризует детерминированно, генерирует стабильные локаторы для PageObject — без ML в core, AI-ready через MCP |
| **Elevator (EN)** | Deterministic engine for UI structure extraction: parses element trees, clusters components, generates stable locators for PageObject — NO ML in core, AI-ready via MCP |
| **GitHub README** | Structure discovery engine + deterministic element resolution. Core (Rust/WASM): NO ML, NO LLM. AI-Ready: element maps via MCP for LLM grounding |
| **Официальный** | Frap — deterministic engine для автоматического извлечения структуры UI: парсит деревья элементов, кластеризует компоненты детерминированными алгоритмами, генерирует устойчивые идентификаторы для PageObject и тестов. Без ML в core, без облачных API — bank-grade deterministic. AI-ready через MCP: даёт LLM структурированные element maps. |

---

## Тезисы по аудиториям

### S1: Legacy Selenium / Java (банк, enterprise)

**Главное сообщение:** «Получи структурированную карту UI для PageObject без ручного анализа XPath — deterministic, on-prem, bank-grade»

**Ключевые тезисы:**
- `frap discover <url> → element-map.json` за секунды вместо часов ручного анализа
- Не новый раннер — JUnit listener / SDK для существующих тестов
- Core deterministic (NO ML dependencies) — проходит security review
- Экспорт healing events в JUnit XML / Allure для CI audit

### S2: Playwright / TypeScript (modern teams)

**Главное сообщение:** «Быстрое получение структуры для тестов — с устойчивыми селекторами, которые переживут рефакторинг»

**Ключевые тезисы:**
- `frap discover` даёт element map с confidence scores
- Generated PageObject с stable signatures — рефакторинг UI не ломает тесты
- Deterministic resolution — explainable diff при изменениях
- **Structural regression gate в CI**: fail при неожиданном drift, не screenshot
- Можно использовать standalone (через CDP) или как Playwright adapter

### S3: AI-native QA (LLM agents, MCP)

**Главное сообщение:** «Дай LLM структурированный доступ к UI — Frap даёт AI-агенту надёжные руки и глаза»

**Ключевые тезисы:**
- **Глаза:** element map вместо raw DOM — структурированное зрение для LLM
- **Руки:** deterministic resolution при изменении UI — стабильные действия
- MCP tools: `frap/discover`, `frap/analyze`, `frap/resolve`
- Frap не генерирует тесты — даёт grounding layer для надёжной генерации

---

## Universal value props (работают для всех)

1. **Структурный анализ UI за секунды** — не часы ручного reverse engineering
2. **Устойчивые идентификаторы** — element signatures переживают рефакторинг UI
3. **Без ML в core** — deterministic algorithms (Drain3, weighted matching), NO GPU, NO cloud API
4. **On-prem, без облака** — подходит для regulated сред (банки, enterprise, compliance)
5. **Explainable by design** — element map с confidence scores, diff при drift, не "black box"
6. **Platform-agnostic** — один element map format для Web, Android, iOS (roadmap)
7. **AI-ready grounding layer** — structured element maps для LLM через MCP, не competing с AI tools
8. **Bank-grade security** — open source, deterministic core, аудит trail, нет внешних зависимостей

---

## Workflow: Discovery → Generation

**Новый narrative для презентаций:**

### Phase 1: Discovery (сейчас)
```bash
frap discover --url https://shop.example.com/catalog
# Output: element-map.json
# - Все интерактивные элементы
# - Clusters (карточки товаров, фильтры, пагинация)
# - Confidence scores для каждого ID
```

### Phase 2: Generation (ручная / AI)
```typescript
// Human или LLM использует element map
import elementMap from './element-map.json';

// Generated PageObject с stable selectors
class CatalogPage {
  async applyFilter(category: string) {
    // Использует stable ID из element map
    await page.click(elementMap.clusters.filters[0].stableId);
  }
}
```

### Phase 3: Maintenance (автоматическая)
```bash
# При изменении UI
frap analyze --url https://shop.example.com/catalog \
  --against element-map.json
# Output: drift-report.json
# - Что изменилось
# - Новые stable IDs
# - Confidence scores для migration
```

---

## Демо-сценарий (для live demo на митапе)

**Цель:** показать за 5-7 минут ценность без подготовки

1. **Discovery** — `frap discover https://demo-store.local`, показываем element map
2. **Structure** — clusters, confidence scores, stable IDs
3. **Break it** — меняем DOM структуру (рефакторинг)
4. **Analyze** — `frap analyze` показывает drift и new mappings
5. **Explain** — confidence scores, что изменилось, почему mapping корректен

**Ключевой момент:** deterministic structure analysis, не "magical healing"

---

## Ответы на возражения

**"У нас уже getByRole, почему нужен frap?"**
> getByRole тоже ломается при смене текста или структуры. frap даёт структурный анализ — element map с confidence scores, который можно использовать для stable PageObject generation. Это не замена getByRole, а foundation для maintainable selectors.

**"Почему не Healenium?"**
> Healenium использует ML (не объяснимо), требует PostgreSQL + proxy (тяжёлый стек), сложно пройти security в банке. frap — deterministic (NO ML in core), WASM, zero external dependencies.

**"Почему не playwright-healer / AutoHeal?"**
> Часто используют AI/heuristics без детерминизма и explainability. frap даёт воспроизводимый element map — при одинаковой структуре всегда одинаковый результат, explainable для аудита в CI.

**"У нас Selenium/Java, не Playwright"**
> Core на Rust компилируется в нативный код для Java через FFI. `frap discover` — CLI tool, работает с любым фреймворком. Element map → generated PageObject для вашего стека.

**"Это замена Playwright/Selenium?"**
> Нет. frap — structure discovery engine. Playwright/Selenium остаются execution layer. Frap даёт: (1) быстрый discovery, (2) stable identifiers, (3) drift detection.

**"Как это работает с AI-агентами?"**
> Frap — grounding layer: даёт LLM структурированный element map через MCP. Агент использует эту структуру для надёжных действий. Метафора: **Frap даёт AI-агенту надёжные руки и глаза**.

---

## Конкурентное позиционирование

| | Healenium | playwright-healer | **frap** |
|--|-----------|-------------------|------------|
| **Core** | ML-based | Heuristics + optional AI | **Deterministic (NO ML)** |
| **Output** | Fixed selector | Fixed selector | **Element map + stable IDs** |
| **Explainability** | Low | Medium | **Score + diff + element map** |
| **Platform** | Selenium only | Playwright only | **Platform-agnostic (Web → Mobile)** |
| **On-prem** | Proxy+PostgreSQL | Depends | **WASM, zero dependencies** |
| **AI Integration** | No | No | **MCP grounding layer** |

---

## Сообщения для разных каналов

### Twitter/X (280 chars)
> Frap: deterministic UI structure discovery. NO ML in core. Parse DOM → element maps with confidence scores → stable locators for PageObject. AI-ready via MCP: gives LLMs reliable hands & eyes for testing. Open source, bank-grade, on-prem.

### LinkedIn (professional)
> Introducing Frap: a deterministic engine for UI structure extraction. Unlike ML-based solutions, Frap uses algorithmic clustering to generate stable element identifiers — perfect for regulated environments requiring audit trails. AI-ready through MCP for LLM agent grounding.

### Hacker News (technical)
> Show HN: Frap — deterministic UI structure analysis in Rust/WASM. No ML, no cloud calls. Extracts element trees, clusters components (Drain3), generates stable IDs. Useful for: (1) quick PageObject generation, (2) CI drift detection, (3) LLM grounding layer via MCP.

### Dev.to / Medium (educational)
> From "self-healing tests" to "structure discovery": why we built a deterministic engine for UI analysis. Explaining Frap's approach to stable identifiers without ML dependencies, and how it serves as a grounding layer for AI agents through MCP.

---

## Где ещё есть messaging

- `docs/positioning.md` — стратегическое позиционирование, one-liners, AI positioning
- `docs/audience.md` — сообщения по сегментам аудитории
- `docs/talk-topics.md` — темы докладов с тезисами
- `docs/strategy.md` — 3 слоя архитектурного развития
- `project/feature/F001-structure-discovery.md` — технические детали алгоритмов

---

*Обновлять при изменении позиционирования или добавлении новых фич.*
