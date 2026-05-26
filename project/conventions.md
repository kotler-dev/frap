# Конвенции проекта frap

## Именование

### Фичи
- ID: `F001`, `F002`, ... `F011`
- Файлы: `F001-self-healing.md`, `F002-unified-context.md`

### Кейсы
- PoC: `CP001`–`CP005`
- Демо: `C001`–`C008`
- Файлы: `C001-payment-button.md`

### Релизы
- `v1.0.0` — MVP
- `v1.1.0` — Context Layer
- `v1.2.0` — AI Integration
- `v2.0.0` — Scale
- `v3.0.0` — Future

## Статусы

### Фичи
- `draft` — проектирование
- `in-progress` — в разработке
- `done` — реализовано
- `frozen` — заморожено
- `cancelled` — отменено

### Кейсы
- `concept` — идея
- `script-ready` — скрипт готов
- `demo-recorded` — демо записано
- `validated` — проверено

### Общие
- ❌ — не реализовано
- ⚠️ — частично
- ✅ — полностью

## Git

### Коммиты
- Формат: `type(scope): message`
- Types: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`
- Примеры:
  - `feat(signature): add path extraction`
  - `fix(healing): correct confidence calculation`
  - `docs(readme): update installation guide`

### Ветки
- `main` — стабильная версия
- `develop` — разработка
- `feature/F001-*` — фичи
- `fix/*` — багфиксы

## Код

### Rust
- `snake_case` для функций
- `PascalCase` для типов
- `SCREAMING_SNAKE_CASE` для констант
- Нет `unwrap()` в production

### TypeScript
- `camelCase` для функций
- `PascalCase` для классов/типов
- `kebab-case` для файлов
- Строгий TypeScript, нет `any`

### Документация
- Rust: `///` для публичных API
- TypeScript: TSDoc
- Фичи: шаблон `_template.md`

## Тесты

### Покрытие
- Unit: каждый публичный метод
- Integration: каждая фича
- E2E: каждый кейс (CP*, C*)

### Нейминг
- Unit: `test_<function>_<scenario>`
- Integration: `test_<feature>_<case>`
- E2E: `test_<case_id>`

## Документация

### Файлы
- `CONTEXT.md` — точка входа
- `project/FEATURES.md` — трекер фич
- `project/feature/*.md` — карточки фич
- `docs/*.md` — исходные материалы

### Обновление
- При изменении фичи — обновить её карточку
- При добавлении фичи — добавить в `FEATURES.md`
- При изменении статуса — обновить статистику

## Код-ревью

### Чеклист
- [ ] Код компилируется
- [ ] Тесты проходят
- [ ] Документация обновлена
- [ ] Соответствует конвенциям
- [ ] Нет `unwrap()`/`any` без причины

### Кто ревьюит
- Core (Rust) — tech lead
- SDK (TS) — frontend lead
- Фичи — product owner
