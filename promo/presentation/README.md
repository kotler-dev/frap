# Presentation — project vision

16-slide HTML deck: problem, positioning, discover/clustering/heal, context/RCA, conference demo, roadmap.

| Language | File |
|----------|------|
| English (default) | [index.html](index.html) |
| Russian | [index.ru.html](index.ru.html) |

Switch language via the **EN** / **RU** link in the bottom navigation bar.

## Styles

All CSS is under [`assets/`](assets/):

| File | Purpose |
|------|---------|
| `tokens.css` | Brand accent colors, shadows |
| `theme-light.css` | Light surfaces |
| `theme-dark.css` | Dark surfaces |
| `deck.css` | Slide layout and navigation |

Load order in HTML:

```html
<link rel="stylesheet" href="assets/tokens.css">
<link rel="stylesheet" href="assets/theme-light.css">
<link rel="stylesheet" href="assets/theme-dark.css">
<link rel="stylesheet" href="assets/deck.css">
```

Set `data-theme="light"` or `data-theme="dark"` on `<html>` (toggle in the deck nav).

## Export (local only)

PDF/PPTX exports are gitignored under `export/` — generate locally if needed for a talk.
