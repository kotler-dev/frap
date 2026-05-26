# Feature: AI-Agent Testing & Audit (F011)

## Meta

- **Epic**: Integration → AI-Native
- **Roll-up target**: ## v3.0.0 (Future)
- **Status**: draft
- **Target release**: v3.0.0
- **Created**: 2026-05-20
- **Related cases**: C007, C008

## Goal

Тестирование и аудит AI-агентов: capture MCP tool calls, assertion-based testing, A2A flow testing, prompt regression.

**Frap даёт AI-агенту надёжные руки и глаза:** 
- **Глаза** — структурированный element map вместо скриншотов/DOM
- **Руки** — deterministic resolution при изменении UI  
- **Память** — audit trail всех действий для анализа behavior drift

Frap не заменяет AI-агент — Frap делает его действия стабильными и объяснимыми.

## User workflow

1. AI-агент тестирует приложение через Frap MCP
2. Frap записывает: tool calls, аргументы, результаты
3. Тестировщик задаёт assertions: «вызвать replay ровно 2 раза"
4. Frap проверяет assertions на записанной сессии
5. При смене модели (GPT-4 → Claude) — replay сессии, сравнение behavior
6. Для мультиагентных систем: A2A flow testing

## Scope

### In
- Capture: MCP tool calls от агента
- Assertions: ожидаемые tool calls, порядок, аргументы
- Replay сессии с другой моделью
- A2A flow testing: диалоги между агентами
- Prompt regression: behavior при смене промптов/моделей
- Compliance: audit trail действий агента

### Out
- Эмуляция LLM (тестируем агента, не LLM)
- Training/fine-tuning моделей
- Автоматическое исправление агентов

## Acceptance criteria

- [ ] Capture: `Frap agent:record` записывает tool calls
- [ ] Assertions: `Frap agent:assert` проверяет правила
- [ ] Replay: `Frap agent:replay` с другой моделью
- [ ] A2A: `Frap agent:swarm` для мультиагентов
- [ ] C007: assertions проходят на исходной модели
- [ ] C008: A2A коммуникация протестирована
- [ ] Audit trail: immutable log действий агента

## Implementation notes (sketch)

### Модули
```
crates/agent/
├── src/
│   ├── capture.rs       # Захват tool calls
│   ├── assertions.rs    # Проверка assertions
│   ├── replay.rs        # Replay с другой моделью
│   └── a2a.rs           # A2A flow testing
```

### Capture формат
```rust
struct AgentSession {
    agent_id: String,
    session_id: String,
    tool_calls: Vec<ToolCall>,
    llm_calls: Vec<LlmCall>,  // опционально, для анализа
}

struct ToolCall {
    timestamp: u64,
    tool: String,           // "Frap/replay"
    arguments: Value,
    result: Value,
}
```

### Assertions
```rust
enum Assertion {
    ToolCalled { tool: String, times: u32 },
    ToolOrder { sequence: Vec<String> },
    ArgumentValid { tool: String, check: Fn },
    DurationUnder { seconds: u32 },
}
```

### CLI
```bash
# Запись сессии
Frap agent:record --agent-id "shop-assistant" --session-id "audit-001"

# Отправка запроса агенту
echo "Проверь оформление заказа" | Frap agent:prompt --session "audit-001"

# Проверка assertions
Frap agent:assert --session "audit-001" \
  --rule "Frap/replay called 2 times" \
  --rule "duration < 60s"

# Replay с другой моделью
Frap agent:replay --session "audit-001" --model "claude-sonnet-4"

# A2A swarm
Frap agent:swarm --agents "coordinator,ui-tester,reporter" --capture
```

### Риски и зависимости
- Зависит от F005 (MCP)
- Сложность: много компонентов (capture, assertions, replay)
- Privacy: записываем действия агента, возможно чувствительно

## Verification / Test plan

### Manual smoke
```bash
# C007: AI-Agent Tool Call Audit
Frap agent:record --agent-id "shop-assistant" --session-id "test-001"
echo "Проверь оформление заказа" | Frap agent:prompt --session "test-001"
Frap agent:assert --session "test-001" --rule "Frap/replay called 2 times"
# Expected: PASSED

# C008: Multi-Agent A2A
Frap agent:swarm --agents "coordinator,ui-tester" --capture
Frap agent:swarm:task --prompt "Test checkout flow"
Frap agent:swarm:analyze --run-id <id>
# Expected: messages, delegations, no data loss
```

### Automation
- Юнит-тесты: assertions engine
- Интеграционные: C007, C008
- Мок LLM для тестирования

## Related docs

- [F005-mcp-integration.md](./F005-mcp-integration.md) — базовый MCP
- [cases.md](../../docs/cases.md) — C007, C008
