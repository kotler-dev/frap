# Feature: Feedback Loop (F009)

## Meta

- **Epic**: Enhancement → Learning
- **Roll-up target**: ## v1.2.0 (AI Integration)
- **Status**: draft
- **Target release**: v1.2.0
- **Created**: 2026-05-20
- **Related cases**: C001 (с F009 extension)

## Goal

Обучение на исправлениях пользователя: когда кластеризация находит не тот элемент, пользователь указывает правильный — система обновляет веса.

## User workflow

1. Тест падает из-за healing failure или неправильного элемента
2. Пользователь запускает `frap learn`
3. Интерактивно указывает правильный элемент (drag-and-drop или selector)
4. Система сравнивает: что нашли vs что правильно
5. Обновляет веса сигнатур для данного сценария
6. Следующий запуск использует обновлённые веса

## Scope

### In
- CLI команда `frap learn`
- Интерактивный выбор элемента (browser UI или drag-and-drop)
- Обновление весов сигнатур
- Сохранение весов per-scenario
- Применение весов при следующем healing

### Out
- Global learning (веса общие для всех сценариев)
- Автоматическое обучение без подтверждения
- ML/нейронные сети

## Acceptance criteria

- [ ] Команда `frap learn --name <scenario>` работает
- [ ] Интерактивный выбор элемента
- [ ] Обновление весов сигнатур после выбора
- [ ] Веса сохраняются per-scenario
- [ ] Следующий replay использует новые веса
- [ ] CP006 проходит: после одной коррекции — лучший score

## Implementation notes (sketch)

### Модули
```
crates/learning/
├── src/
│   ├── feedback.rs      # Обработка фидбека
│   ├── weights.rs       # Управление весами
│   └── storage.rs       # Сохранение per-scenario
```

### Формат весов
```rust
struct SignatureWeights {
    // Веса для разных компонентов сигнатуры
    path_weight: f64,
    text_weight: f64,
    attr_weight: f64,
    position_weight: f64,
}

// Сохранение per-scenario
scenario_weights: HashMap<String, SignatureWeights>
```

### Алгоритм обучения
1. Получить: ожидаемый селектор, найденный элемент, правильный элемент
2. Сравнить сигнатуры: что совпало, что не совпало
3. Увеличить веса для признаков, которые совпали у правильного
4. Уменьшить веса для признаков, которые совпали у неправильного
5. Сохранить обновлённые веса

### CLI
```bash
# Интерактивное обучение
frap learn --name "payment-flow"
# [открывается UI со сценарием]
# [пользователь выбирает правильный элемент]
# Saved: updated weights for scenario "payment-flow"
```

### Риски и зависимости
- Overfitting: веса могут стать слишком специфичными
- Privacy: хранение весов может раскрывать структуру приложения
- UX: интерактивный выбор должен быть удобным

## Verification / Test plan

### Manual smoke
```bash
# 1. Запуск с неправильным healing
frap replay --name "payment-flow"
# Result: FAILED или неправильный элемент

# 2. Обучение
frap learn --name "payment-flow"
# [выбираем правильный элемент]

# 3. Повторный запуск
frap replay --name "payment-flow"
# Result: PASSED с правильным элементом
```

### Automation
- CP006: feedback learn gate
- Юнит-тесты: обновление весов

## Related docs

- [F001-self-healing.md](./F001-self-healing.md) — базовый healing
- [benchmark.md](../../docs/benchmark.md) — CP006
