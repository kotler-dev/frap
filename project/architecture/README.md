# Архитектура fletta

Общая архитектура системы и описание основных компонентов.

## Общая схема

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              Пользователь                               │
│                    (тестировщик, LLM-агент, CI)                          │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           SDK / Adapters                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ TypeScript   │  │ Java         │  │ Python       │  │ MCP Server   │  │
│  │ (Playwright) │  │ (Selenium)   │  │ (pytest)     │  │ (JSON-RPC)   │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           Rust Core (WASM/Native)                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ Signature    │  │ Clustering   │  │ Healing      │  │ Context      │  │
│  │ (сигнатуры)  │  │ (Drain3)     │  │ (fallback)   │  │ (timeline)   │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ RCA          │  │ Generator    │  │ Learning     │  │ Health       │  │
│  │ (анализ)     │  │ (POM gen)    │  │ (feedback)   │  │ (score)      │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
           ┌────────────┐   ┌────────────┐   ┌────────────┐
           │ DOM/Web    │   │ Android    │   │ iOS        │
           │ (WASM)     │   │ (JNI)      │   │ (Swift)    │
           └────────────┘   └────────────┘   └────────────┘
```

## Компоненты

### Core (Rust)

| Crate | Назначение | Статус |
|-------|------------|--------|
| `signature` | Извлечение и сравнение сигнатур DOM-элементов | MVP |
| `clustering` | Drain3-алгоритм для DOM/логов | MVP |
| `healing` | Orchestration: primary → fallback chain | MVP |
| `context` | Capture: UI + network + logs → timeline | v1.1 |
| `rca` | Root Cause Analysis | v1.1 |
| `generator` | Page Object Generator | v1.2 |
| `mcp` | MCP JSON-RPC server | v1.2 |
| `learning` | Feedback loop, обновление весов | v1.2 |
| `health` | Health score, метрики | v2.0 |
| `agent` | AI-Agent testing, capture, assertions | v3.0 |

### SDK

| Язык | Фреймворк | Статус |
|------|-----------|--------|
| TypeScript | Playwright | MVP |
| Java | Selenium/JUnit | v1.4 |
| Python | pytest-playwright | backlog |

### Платформы

| Платформа | Технология | Статус |
|-----------|------------|--------|
| Web | WASM + Playwright | MVP |
| Android | JNI + UI Automator | v2.0 |
| iOS | Swift + XCUITest | v2.0 |

## Поток данных

### Self-Healing Flow

```
1. Запуск теста с primary селектором
2. Поиск по primary → не найден
3. Получение snapshot текущего DOM
4. Извлечение сигнатур всех кандидатов
5. Сравнение с оригинальной сигнатурой
6. Ранжирование по confidence score
7. Если max(confidence) >= minConfidence → healed
8. Иначе → fail с отчётом (top-3 кандидатов)
```

### Context Capture Flow

```
1. Тест запущен в режиме --capture-all
2. Interceptors активны:
   - network: все HTTP запросы/ответы
   - logs: console + application logs
   - UI: все DOM-события
3. События записываются с timestamp и trace_id
4. При падении: timeline строится для окна [t-5s, t+5s]
5. RCA использует timeline для классификации
```

## Интерфейсы

### Rust Core API (WASM)

```rust
// Сигнатуры
pub fn extract_signature(dom: &str, selector: &str) -> Result<Signature, Error>;
pub fn compare_signatures(a: &Signature, b: &Signature) -> f64;

// Healing
pub fn heal(
    primary_selector: &str,
    original_signature: &Signature,
    current_dom: &str,
    min_confidence: f64,
) -> Result<HealResult, Error>;

// Context
pub fn capture_timeline(events: Vec<Event>) -> Timeline;
pub fn analyze_rca(timeline: &Timeline, failure_time: u64) -> RootCause;
```

### TypeScript SDK API

```typescript
// Playwright adapter
export function flettaPlaywright(config: FlettaConfig): PlaywrightConfig;
export function withFletta<T extends Locator>(locator: T): FlettaLocator<T>;

// Core (через WASM)
export class HealingEngine {
  heal(selector: string, options: HealOptions): Promise<HealResult>;
  extractSignature(element: Element): Signature;
}
```

## Директории

```
project/architecture/
├── README.md           # Этот файл — обзор
├── core.md             # Детали Rust core
├── sdk.md              # Детали SDK
├── adapters.md         # Интеграции с фреймворками
├── platforms.md        # Мультиплатформа
├── mcp.md              # MCP/A2A протокол
└── data-flow.md        # Потоки данных
```
