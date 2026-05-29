# C005: LLM Test Generation

- **Status**: concept
- **Features**: F005 (MCP Integration)

## Context

User prompt: «Проверь что пользователь может добавить товар в корзину и перейти к оплате». LLM agent creates and runs a test via MCP.

## Scenario

1. LLM receives natural-language request
2. Calls frap via MCP (`generate` or equivalent)
3. Browser explores the page
4. Steps generated: add to cart, verify counter, checkout
5. Returns test code or execution result

## Success criteria

Generated test passes on the demo application.
