# C007: AI-Agent Tool Call Audit

- **Status**: concept
- **Features**: F011 (AI-Agent Testing), F005 (MCP)

## Context

Order-assistant agent uses frap MCP tools. Need to verify tool calls, order, and arguments.

## Scenario

1. Start agent capture
2. User: «Проверь оформление заказа»
3. Agent calls `frap/replay` (cart, checkout), optionally `frap/analyze` on failure
4. Record all tool calls with args and results
5. Assert: call count, order, valid scenario names, duration
6. Replay with another model — compare behavior

## Success criteria

Assertions pass on baseline model; report shows tool-call timeline and reasoning steps.
