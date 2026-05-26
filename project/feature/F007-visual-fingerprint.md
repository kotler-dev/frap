# Feature: Visual Fingerprint (F007)

## Meta

- **Epic**: Enhancement → Accuracy
- **Roll-up target**: ## v2.0.0 (Scale)
- **Status**: draft
- **Target release**: v2.0.0
- **Created**: 2026-05-20
- **Related cases**: — (улучшение F001)

## Goal

Добавление визуальных признаков в сигнатуру элемента для улучшения точности при полной смене атрибутов.

## User workflow

1. При записи сценария сохраняются визуальные признаки элемента
2. При healing: если атрибуты сильно изменились, используем визуальные признаки
3. Сравниваем: относительный размер, положение, стабильные стили
4. Улучшенный confidence score

## Scope

### In
- Извлечение визуальных признаков: размер (относительно контейнера), положение
- Стабильные CSS-свойства: background-color, border-color
- Компактное представление (не скриншоты целиком)
- Интеграция в signature comparison

### Out
- Пиксельное сравнение изображений (как в Applitools)
- Скриншоты в отчётах (это enterprise фича)
- ML-based visual matching

## Acceptance criteria

- [ ] Визуальные признаки извлекаются при записи
- [ ] Относительный размер (не абсолютный пиксели)
- [ ] Положение в контейнере
- [ ] Стабильные стили: background-color, border-color
- [ ] Интеграция в алгоритм confidence calculation
- [ ] Точность healing > 95% на benchmark с визуальными изменениями

## Implementation notes (sketch)

### Модули
```
crates/signature/
├── src/
│   └── visual.rs
```

### Визуальная сигнатура
```rust
struct VisualSignature {
    // Относительные величины (0.0–1.0 относительно контейнера)
    relative_width: f64,
    relative_height: f64,
    relative_x: f64,
    relative_y: f64,

    // Стабильные стили
    background_color: Option<String>,
    border_color: Option<String>,
    color: Option<String>,
}
```

### Алгоритм
1. Получить bounding box элемента и его контейнера
2. Рассчитать относительные размеры и позицию
3. Извлечь computed styles (только стабильные)
4. При сравнении: добавить вес визуальным признакам

### Риски и зависимости
- Производительность: computed styles тяжёлые
- Относительные размеры: зависят от viewport
- Не все стили стабильны (hover, анимации)

## Verification / Test plan

### Manual smoke
```bash
# Сценарий: полная смена атрибутов, стиль остался
fletta replay --name "visual-test"
# Expected: PASSED, confidence boosted by visual match
```

### Automation
- Бенчмарки: сравнение с/без visual fingerprint
- Точность > 95%

## Related docs

- [F001-self-healing.md](./F001-self-healing.md) — базовый healing
