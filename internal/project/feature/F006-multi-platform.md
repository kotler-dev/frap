# Feature: Multi-Platform Core (F006)

## Meta

- **Epic**: Architecture → Platform
- **Roll-up target**: ## v2.0.0 (Scale)
- **Status**: draft
- **Target release**: v2.0.0
- **Created**: 2026-05-20
- **Related cases**: C006

## Goal

Единое ядро на Rust для веб, Android, iOS. Один формат сценария — разные платформы.

## User workflow

1. Тестировщик записывает сценарий на Android
2. Тот же JSON-сценарий работает на iOS и в вебе
3. Ядро адаптирует: ViewTree (Android) ↔ DOM (веб) ↔ View (iOS)
4. Сигнатуры строятся по платформенным признакам
5. Self-healing работает одинаково на всех платформах

## Scope

### In
- Ядро Rust: кластеризация, сигнатуры — платформонезависимые
- WASM-сборка для веб/Node.js (уже есть в MVP)
- FFI для нативных SDK (Java, Swift)
- ViewTree адаптер для Android
- View адаптер для iOS
- Унифицированный формат сценария (JSON)

### Out
- Appium-замена (отдельный продукт)
- IDE для мобильной записи (CLI сначала)
- Облачное устройство-ферма

## Acceptance criteria

- [ ] Android: ViewTree capture и анализ
- [ ] iOS: View capture и анализ
- [ ] Единый формат сценария работает на всех платформах
- [ ] Сигнатуры: адаптация Drain3 для ViewTree/UIView
- [ ] C006: сценарий из C001 работает на Android
- [ ] FFI мосты: Java (Android), Swift (iOS)
- [ ] Документация: platform-specific selectors

## Implementation notes (sketch)

### Модули
```
crates/core/
├── src/
│   ├── platform/
│   │   ├── web.rs        # DOM адаптер (уже есть)
│   │   ├── android.rs    # ViewTree адаптер
│   │   └── ios.rs        # UIView адаптер
│   └── signature/
│       └── unified.rs    # Унифицированная сигнатура
```

### Унифицированная сигнатура
```rust
enum ElementSignature {
    Web { tag: String, attrs: HashMap },
    Android { class: String, resource_id: Option<String>, text: Option<String> },
    Ios { element_type: String, identifier: Option<String>, label: Option<String> },
}

// Сравнение cross-platform: по структуре и тексту
fn compare_signatures(a: &ElementSignature, b: &ElementSignature) -> f64 {
    // Платформонезависимое сравнение
}
```

### FFI мосты
```rust
// Android (JNI)
#[no_mangle]
pub extern "C" fn Java_com_frapcode_core_heal(...)

// iOS (C header)
pub extern "C" fn frap_heal(...)
```

### Риски и зависимости
- Большая работа: 2 новые платформы
- Тестирование на реальных устройствах
- Appium уже существует — нужно отличие

## Verification / Test plan

### Manual smoke
```bash
# Android
Frap record --platform android --name "mobile-payment"
# Меняем resource_id в приложении
Frap replay --platform android --name "mobile-payment"
# Expected: PASSED (healed)

# iOS — аналогично
```

### Automation
- Интеграционные: Android emulator, iOS simulator
- C006: полный сценарий

## Related docs

- [cases.md](../../docs/cases.md) — C006
