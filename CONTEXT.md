# frap — контекст для агента

Перед большой задачей прочитай этот файл и при необходимости — указанные ссылки.

## Стек

- **Core:** Rust (алгоритмы кластеризации, сигнатуры DOM/логов) → компиляция в WASM и нативные сборки
- **SDK:** TypeScript (frozen for release); Java 1.0.0 on Maven Central path (`frap-core-java`, `frap-playwright`, discovery + PO gen)
- **Интеграции:** Playwright (custom selectors), MCP (JSON-RPC для LLM-агентов), JUnit export
- **Платформы:** Web (v1), Android/iOS (v3)

## Структура репо

```
frap/
├── CONTEXT.md              # Этот файл — точка входа
├── project/                # Планирование, фичи, архитектура
│   ├── FEATURES.md         # Главная таблица фич F001–F011
│   ├── feature/            # Карточки фич
│   │   ├── _template.md
│   │   ├── F001-self-healing.md
│   │   ├── F002-unified-context.md
│   │   ├── F003-rca.md
│   │   ├── F004-page-object-gen.md
│   │   ├── F005-mcp-integration.md
│   │   ├── F006-multi-platform.md
│   │   ├── F007-visual-fingerprint.md
│   │   ├── F008-playwright-adapter.md
│   │   ├── F013-typescript-sdk.md
│   │   ├── F014-java-sdk-ui-adapters.md
│   │   ├── F015-python-sdk-adapters.md
│   │   ├── F009-feedback-loop.md
│   │   ├── F010-test-health.md
│   │   ├── F011-ai-agent-testing.md
│   │   └── F017-structural-contract.md
│   ├── cases/              # Сценарии (C001–C008, CP001–CP005)
│   ├── architecture/       # Архитектурные документы
│   └── conventions.md      # Соглашения по коду
├── crates/                 # Rust core (workspace: signature, core, context, …)
├── sdk/                    # SDK разных языков
│   └── typescript/
├── adapters/               # Интеграции с фреймворками
│   └── playwright/
├── docs/                   # Исходные материалы (positioning, audience и т.д.)
└── .cursor/
    ├── rules/              # Правила Cursor
    └── skills/             # Переиспользуемые навыки
```

## Документация

| Что нужно | Файл |
|-----------|------|
| Таблица фич, статусы, релизы | `project/FEATURES.md` |
| Карточка фичи | `project/feature/FXXX-*.md` |
| Сценарии и кейсы | `project/cases/` |
| Архитектура ядра | `project/architecture/` |
| Позиционирование, конкуренты | `docs/positioning.md` |
| Структурный контракт (vs скриншоты) | `docs/structural-contract.md` |
| Боли, scope, copy для сайта/презентаций | `docs/pains.md` |
| Целевая аудитория | `docs/audience.md` |
| PoC gates и метрики | `docs/benchmark.md` |
| Интеграции | `docs/integrations.md` |
| Selenium/Java (банк) | `docs/integrations-selenium-java.md` |
| SDK (Java, TS, Python) | `project/architecture/sdk-strategy.md` |
| Монетизация | `docs/monetization.md` |

## Принципы

1. **Explainable by design** — все решения должны быть объяснимы (score, diff, policy)
2. **Integration, not replacement** — дополняем Playwright/Selenium, не заменяем
3. **No-ML by default** — детерминированные алгоритмы, ML только опционально
4. **On-prem first** — работает без облачных API, bank-grade security
5. **Tri-plane context** — UI + logs + network в одном сценарии

## Как давать задачи

- Указывать конкретные файлы или папки (`@file`, `@folder`)
- Дробить большие постановки на подзадачи с явным списком файлов на итерацию
- Для фичи — читать `project/feature/FXXX-*.md` перед реализацией
- Для архитектурных решений — сверяться с `project/architecture/`

## Терминология

| Термин | Значение |
|--------|----------|
| Self-healing | Автоматическое восстановление селектора при изменении UI |
| Signature | Сигнатура пути — устойчивые атрибуты + структура элемента |
| Drain3 | Алгоритм кластеризации логов, адаптированный к DOM |
| MCP | Model Context Protocol — интеграция с LLM-агентами |
| A2A | Agent-to-Agent protocol — мультиагентное взаимодействие |
| RCA | Root Cause Analysis — определение первопричины падения |
| Structural Contract | Структурный контракт — baseline + policy + drift gate для валидации UI |
| Drift report | Отчёт об изменениях структуры UI (element/structural/cluster drift) |

## Статусы релизов

- **v1.0.0 (MVP)** — F001 + F008: Self-healing + Playwright adapter
- **v1.1.0** — F002 + F003: Unified Context + RCA
- **v1.2.0** — F004 + F005 + F009: Page Object Gen + MCP + Feedback Loop
- **v2.0.0** — F006 + F007 + F010 + F017: Multi-platform + Visual Fingerprint + Health Score + Structural Contract
- **v3.0.0** — F011: AI-Agent Testing & Audit
