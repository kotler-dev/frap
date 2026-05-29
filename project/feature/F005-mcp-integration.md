# Feature: MCP/A2A Integration (F005)

## Meta

- **Epic**: Integration → AI
- **Roll-up target**: ## v1.2.0 (AI Integration)
- **Status**: draft
- **Target release**: v1.2.0
- **Created**: 2026-05-20
- **Related cases**: C005, C007, C008

## Goal

Интерфейс для вызова frap из LLM-агентов через Model Context Protocol (JSON-RPC). 

**Frap как grounding layer для AI-агентов:** Frap не генерирует тесты и не управляет LLM — Frap даёт AI-агенту **надёжные руки и глаза**: структурированный доступ к UI (element maps) и стабильное выполнение действий (resolution при изменениях).

## User workflow

1. LLM-агент получает задачу на естественном языке
2. Через MCP вызывает frap tools: record, replay, analyze, generate
3. frap выполняет операцию и возвращает структурированный результат
4. Агент принимает решение на основе результата
5. Повторяет или завершает задачу

## Scope

### In
- MCP JSON-RPC server
- Tools: record, replay, analyze, generate, export
- Schema для каждого tool (inputs, outputs)
- Rate limiting (local режим)
- Интеграция с F003 (RCA) для analyze tool
- Интеграция с F004 (Generator) для generate tool

### Out
- A2A протокол (в F011)
- Cloud/облачный inference (on-prem only)
- Execution без approval (policy контролирует)

## Acceptance criteria

- [ ] MCP сервер запускается локально
- [ ] Tool `frap/record`: начать запись сценария
- [ ] Tool `frap/replay`: воспроизвести сценарий с healing
- [ ] Tool `frap/analyze`: RCA-репорт для упавшего теста
- [ ] Tool `frap/generate`: создать тест из описания
- [ ] C005: LLM генерирует тест из текста
- [ ] Schema документирована для агентов
- [ ] Rate limiting: local-only, no external calls

## Implementation notes (sketch)

### Модули
```
crates/mcp/
├── src/
│   ├── server.rs        # JSON-RPC server
│   ├── tools/
│   │   ├── record.rs
│   │   ├── replay.rs
│   │   ├── analyze.rs
│   │   └── generate.rs
│   └── schema.rs        # Tool schemas
```

### Tool Schemas
```rust
// frap/record
struct RecordInput {
    name: String,
    url: String,
}

// frap/replay
struct ReplayInput {
    name: String,
    capture_all: Option<bool>,
}
struct ReplayOutput {
    status: String, // "passed" | "failed" | "healed"
    healing_events: Vec<HealingEvent>,
}

// frap/analyze
struct AnalyzeInput {
    run_id: String,
}
struct AnalyzeOutput {
    primary_cause: String,
    confidence: f64,
    recommendation: String,
}

// frap/generate
struct GenerateInput {
    description: String,
    url: String,
}
struct GenerateOutput {
    test_code: String,
    steps: Vec<String>,
}
```

### MCP Registration
```json
{
  "name": "frap",
  "tools": [
    { "name": "frap/record", "description": "..." },
    { "name": "frap/replay", "description": "..." },
    { "name": "frap/analyze", "description": "..." },
    { "name": "frap/generate", "description": "..." }
  ]
}
```

### Разделение с Playwright MCP
- Playwright MCP: navigate, click, snapshot — агент **действует**
- frap MCP: replay, analyze, generate — агент **стабилизирует и проверяет**

### Риски и зависимости
- Зависит от F003 (analyze) и F004 (generate)
- Security: агент может запросить опасные операции
- Rate limiting: защита от бесконечных циклов

## Verification / Test plan

### Manual smoke
```json
// MCP Request: generate test
{
  "method": "frap/generate",
  "params": {
    "description": "Add product to cart and proceed to checkout",
    "url": "http://demo-store.local"
  }
}

// Expected Response
{
  "test_code": "...",
  "steps": ["navigate to catalog", "click first product", "..."],
  "status": "generated"
}
```

### Automation
- Юнит-тесты: каждый tool
- Интеграционные: MCP сервер + клиент
- C005: end-to-end с LLM

## Related docs

- [positioning.md](../../docs/positioning.md) — vs Playwright MCP
- [integrations.md](../../docs/integrations.md) — MCP раздел
- [cases.md](../../docs/cases.md) — C005
