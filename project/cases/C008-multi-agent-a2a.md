# C008: Multi-Agent A2A Flow Testing

- **Status**: concept
- **Features**: F011 (AI-Agent Testing)

## Context

Swarm: Coordinator, UI-Tester (frap), Reporter — communicate via A2A. Test delegation and context handoff.

## Scenario

1. Capture full swarm session
2. Coordinator receives «Test checkout»
3. Delegates to UI-Tester → frap replay → result to Reporter
4. Verify: task split, full context transfer, error handling on failure

## Success criteria

All subtasks complete; no context loss; retries handled correctly.
