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
├── project/                # Планирование (engineering SSOT)
│   ├── README.md           # Карта project/
│   ├── FEATURES.md         # Статусы фич
│   ├── traceability.md     # Feature ↔ case ↔ E2E
│   ├── feature/            # Карточки F*
│   ├── cases/              # Спеки C*, CONF-*
│   ├── architecture/       # ADR + design
│   ├── release/            # Версии, matrices
│   └── conventions.md
├── examples/               # Runnable consumer projects
├── fixtures/               # Demo AUT (FixtureConf)
├── e2e/                    # Playwright gates
├── promo/                  # Slides
├── crates/                 # Rust core
├── sdk/                    # SDK по языкам
├── adapters/               # Playwright и др.
├── docs/                   # Публичная база (pitch, indexes)
└── .cursor/
    ├── rules/              # Правила Cursor
    └── skills/             # Переиспользуемые навыки
```

## Документация

| Что нужно | Файл |
|-----------|------|
| Таблица фич, статусы, релизы | `project/FEATURES.md` |
| Карта project/ | `project/README.md` |
| Feature ↔ E2E ↔ fixtures | `project/traceability.md` |
| **Release Index** — все текущие версии и координаты | [`project/release/README.md`](./project/release/README.md) |
| **Java SDK 1.0.0** — capability matrix (Maven Central) | [`project/release/java/java-sdk-1.0.0-matrix.md`](./project/release/java/java-sdk-1.0.0-matrix.md) |
| **npm 1.1.1** — capability matrix | [`project/release/typescript/npm-1.1.1-matrix.md`](./project/release/typescript/npm-1.1.1-matrix.md) |
| Карточка фичи | `project/feature/FXXX-*.md` |
| Сценарии и кейсы | `project/cases/` |
| Архитектура ядра | `project/architecture/` |
| Позиционирование, конкуренты | `docs/positioning.md` |
| Структурный контракт (vs скриншоты) | `docs/structural-contract.md` |
| Боли, scope, copy для сайта/презентаций | `docs/pains.md` |
| Целевая аудитория | `docs/audience.md` |
| PoC gates и метрики | `docs/benchmark.md` |
| Интеграции | `docs/integrations.md` |
| **Java SDK** | `sdk/java/README.md`, `docs/en/java-getting-started.md` |
| Selenium/Java (банк roadmap) | `docs/integrations-selenium-java.md` |
| SDK стратегия (Java, TS, Python) | `project/architecture/sdk-strategy.md` |
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
