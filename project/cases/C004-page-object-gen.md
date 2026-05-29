# C004: Page Object Generation

- **Status**: concept
- **Features**: F004 (Page Object Generator)

## Context

New project needs a Page Object for a product catalog: cards, filters, pagination.

## Scenario

1. Open catalog page
2. Run discover/generate
3. frap analyzes repeating elements, interactive controls, forms
4. Generates Page Object with self-healing selectors
5. Export to Java / TypeScript / Python

## Success criteria

Generated code compiles and runs against the target page.

## Related

Java discover E2E: [examples/java/playwright/](../../examples/java/playwright/)
