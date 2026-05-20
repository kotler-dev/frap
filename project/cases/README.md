# Кейсы fletta

Сценарии для демонстрации, тестирования и разработки.

## Структура

```
project/cases/
├── README.md           # Этот файл
├── poc/               # PoC кейсы (CP001–CP005)
│   ├── CP001-happy-path.md
│   ├── CP002-refactor-heal.md
│   ├── CP003-safe-fail.md
│   ├── CP004-role-locator.md
│   └── CP005-ci-export.md
└── demo/              # Полные демо (C001–C009)
    ├── C001-payment-button.md
    ├── C002-api-timeout.md
    ├── C003-flaky-diagnosis.md
    ├── C004-page-object-gen.md
    ├── C005-llm-generation.md
    ├── C006-mobile-self-healing.md
    ├── C007-ai-agent-audit.md
    ├── C008-multi-agent-a2a.md
    └── C009-recording-cdp.md
```

## PoC Cases (быстрая проверка)

| ID | Название | Фичи | Статус |
|----|----------|------|--------|
| CP001 | Happy path | F008 | concept |
| CP002 | Refactor heal | F001, F008 | concept |
| CP003 | Safe fail | F001 | concept |
| CP004 | Role locator | F008 | concept |
| CP005 | CI export | F008 | concept |

## Demo Cases (полные сценарии)

| ID | Название | Фичи | Статус |
|----|----------|------|--------|
| C001 | Payment Button | F001 | concept |
| C002 | API Timeout RCA | F002, F003 | concept |
| C003 | Flaky Diagnosis | F001, F002, F003 | concept |
| C004 | Page Object Gen | F004 | concept |
| C005 | LLM Generation | F005 | concept |
| C006 | Mobile Self-Healing | F006 | concept |
| C007 | AI-Agent Audit | F011 | concept |
| C008 | Multi-Agent A2A | F011 | concept |
| C009 | CDP Recording & Playback | F001, F004, F008 | concept |

## Формат кейса

Каждый кейс содержит:
- **Meta**: ID, название, фичи, статус
- **Goal**: цель сценария
- **Setup**: что нужно подготовить
- **Steps**: пошаговый сценарий
- **Demo script**: команды для воспроизведения
- **Success criteria**: как понять что работает
- **Related**: ссылки на фичи и документацию

## Статусы

- `concept` — идея, не начато
- `script-ready` — скрипт готов, но не проверен
- `demo-recorded` — демо записано, но не отвалидировано
- `validated` — проверено, работает

## Как добавить новый кейс

1. Скопировать `_template.md` из соседнего кейса
2. Заполнить все разделы
3. Добавить в таблицу выше
4. Обновить `docs/cases.md` и `docs/index.md`
5. Указать связанные фичи в карточках фич
