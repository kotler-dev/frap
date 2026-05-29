# Презентация — видение проекта

HTML-дек на 16 слайдов: проблема, позиционирование, discover/кластеризация/heal, context/RCA, conference demo, roadmap.

| Язык | Файл |
|------|------|
| English | [index.html](index.html) |
| Русский (по умолчанию для RU-аудитории) | [index.ru.html](index.ru.html) |

Переключение языка — ссылка **EN** / **RU** в нижней панели навигации.

## Стили

Весь CSS — в [`assets/`](assets/):

| Файл | Назначение |
|------|------------|
| `tokens.css` | Акценты бренда, тени |
| `theme-light.css` | Светлая тема |
| `theme-dark.css` | Тёмная тема |
| `deck.css` | Вёрстка слайдов и навигация |

Порядок подключения в HTML:

```html
<link rel="stylesheet" href="assets/tokens.css">
<link rel="stylesheet" href="assets/theme-light.css">
<link rel="stylesheet" href="assets/theme-dark.css">
<link rel="stylesheet" href="assets/deck.css">
```

## Экспорт (только локально)

PDF/PPTX в `export/` в gitignore — генерируйте локально перед выступлением.
