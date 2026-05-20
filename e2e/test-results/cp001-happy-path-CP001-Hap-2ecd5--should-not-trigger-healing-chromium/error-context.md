# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: cp001-happy-path.spec.ts >> CP001: Happy Path >> stable test should not trigger healing
- Location: cp001-happy-path.spec.ts:5:7

# Error details

```
Error: expect(received).toBeUndefined()

Received: {"confidence": 1, "diff": "Healed with confidence 1.00", "healed": true, "original_signature": {"children_hash": 0, "depth": 3, "path": [{"depth": 0, "role": undefined, "tag": "div"}, {"depth": 1, "role": undefined, "tag": "form"}, {"depth": 2, "role": undefined, "tag": "button"}], "prefix": "div:->form:->button:-", "stable_attrs": {"data-testid": "old111-pay"}, "text_content": "
                Оплатить
            "}, "selector": "[data-testid=\"pay-btn\"]", "top_candidates": [{"confidence": 1, "selector": "[data-testid=\"pay-btn\"]", "signature": {"children_hash": 0, "depth": 3, "path": [{"depth": 0, "role": undefined, "tag": "div"}, {"depth": 1, "role": undefined, "tag": "form"}, {"depth": 2, "role": undefined, "tag": "button"}], "position_in_parent": undefined, "prefix": "div:->form:->button:-", "stable_attrs": {"type": "submit"}, "text_content": "
                Оплатить
            "}}, {"confidence": 0.6576923076923078, "selector": "[data-testid=\"email-input\"]", "signature": {"children_hash": 0, "depth": 4, "path": [{"depth": 0, "role": undefined, "tag": "div"}, {"depth": 1, "role": undefined, "tag": "form"}, {"depth": 2, "role": undefined, "tag": "div"}, {"depth": 3, "role": undefined, "tag": "input"}], "position_in_parent": undefined, "prefix": "div:->form:->div:->input:-", "stable_attrs": {"name": "email", "placeholder": "your@email.com", "type": "email"}, "text_content": undefined}}, {"confidence": 0.6576923076923078, "selector": "[data-testid=\"card-input\"]", "signature": {"children_hash": 0, "depth": 4, "path": [{"depth": 0, "role": undefined, "tag": "div"}, {"depth": 1, "role": undefined, "tag": "form"}, {"depth": 2, "role": undefined, "tag": "div"}, {"depth": 3, "role": undefined, "tag": "input"}], "position_in_parent": undefined, "prefix": "div:->form:->div:->input:-", "stable_attrs": {"name": "card", "placeholder": "0000 0000 0000 0000", "type": "text"}, "text_content": undefined}}]}
```

# Page snapshot

```yaml
- generic [ref=e2]:
  - heading "Оформление заказа (CP001)" [level=1] [ref=e3]
  - paragraph [ref=e4]: "Эта страница для теста CP001: стабильный селектор без изменений."
  - generic [ref=e5]:
    - generic [ref=e6]:
      - generic [ref=e7]: Email
      - textbox "Email" [ref=e8]:
        - /placeholder: your@email.com
    - generic [ref=e9]:
      - generic [ref=e10]: Номер карты
      - textbox "Номер карты" [ref=e11]:
        - /placeholder: 0000 0000 0000 0000
    - button "Оплатить" [active] [ref=e12] [cursor=pointer]
  - generic [ref=e13]: Оплата успешно обработана! (test)
```

# Test source

```ts
  1  | import { test, expect } from '@playwright/test';
  2  | import { withFletta, getLastHealResult } from '@fletta/playwright';
  3  | 
  4  | test.describe('CP001: Happy Path', () => {
  5  |   test('stable test should not trigger healing', async ({ page }) => {
  6  |     await page.goto('http://localhost:3000/cp001-stable.html');
  7  |     
  8  |     const payButton = await withFletta(
  9  |       page.locator('[data-testid="old111-pay"]'),
  10 |       page,
  11 |       { minConfidence: 0.85, enableHealing: true, debug: true },
  12 |       'CP001-form-submission-healing-test' 
  13 |     );
  14 |     
  15 |     await payButton.click();
  16 |     
  17 |     const status = page.locator('#status');
  18 |     await expect(status).toBeVisible();
  19 |     await expect(status).toHaveText('Оплата успешно обработана! (test)');
  20 |     
  21 |     const healResult = getLastHealResult(payButton);
> 22 |     expect(healResult).toBeUndefined();
     |                        ^ Error: expect(received).toBeUndefined()
  23 |   });
  24 | 
  25 |   test.skip('direct locator should work without healing', async ({ page }) => {
  26 |     await page.goto('http://localhost:3000/cp001-stable.html');
  27 |     
  28 |     const emailInput = page.locator('[data-testid="email-input"]');
  29 |     await emailInput.fill('test@example.com');
  30 |     
  31 |     await expect(emailInput).toHaveValue('test@example.com');
  32 |   });
  33 | 
  34 |   test.skip('form submission with stable selectors', async ({ page }) => {
  35 |     await page.goto('http://localhost:3000/cp001-stable.html');
  36 |     
  37 |     await page.fill('[data-testid="email-input"]', 'user@example.com');
  38 |     await page.fill('[data-testid="card-input"]', '1234 5678 9012 3456');
  39 |     
  40 |     const payButton = await withFletta(
  41 |       page.locator('[data-testid="pay-btn1"]'),
  42 |       page,
  43 |       { enableHealing: true, debug: true },
  44 |       'CP001-form-healing'
  45 |     );
  46 |     await payButton.click();
  47 |     
  48 |     await expect(page.locator('#status')).toBeVisible();
  49 |   });
  50 | });
  51 | 
```