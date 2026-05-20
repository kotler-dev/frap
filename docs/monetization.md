# Монетизация

Модель: **полностью open source ядро** + **enterprise-слой** для банков и внешних клиентов. Согласовано с InSourceHub: внутри банка бесплатно (OSS), снаружи — платные возможности для compliance и масштаба.

---

## Принципы

1. **Ядро всегда OSS** — healing, SDK, Playwright adapter, CLI, MCP read-only analyze (базовый).
2. **Enterprise не форкает ядро** — только плагины, policy, hosting опции.
3. **Банк может использовать OSS без оплаты** — монетизация с внешнего рынка и опциональный vendor support contract.
4. **Нет paywall на безопасность** — audit log не «только в платной версии» для критичного минимума; расширенный compliance — paid.

---

## Open Source (бесплатно)

| Компонент | Включено |
|-----------|----------|
| Rust/WASM core | Signature matching, clustering |
| `@fletta/playwright` | Adapter, reports (local) |
| CLI | record, replay, analyze (basic) |
| Benchmarks | CP001–CP005 scripts |
| MCP server (community) | replay, analyze, export (rate-limited local) |
| Документация | RU/EN quick start |

**Лицензия (целевая):** Apache-2.0 — совместимость с InSourceHub и enterprise downstream.

---

## Enterprise (платно)

### Tier: Team (внешние SMB / пилоты)

| Фича | Описание |
|------|----------|
| CI plugins | GitLab Premium template, Jenkins shared library |
| Central report UI | История healing по репозиториям |
| Email support | SLA 48h |

**Ориентир цены:** per-seat или per-repo/month (уточнять после 3 пилотов).

---

### Tier: Enterprise (банки, regulated)

| Фича | Описание |
|------|----------|
| **Policy engine** | min confidence, forbidden heal rules, approve-before-merge |
| **SSO / SAML** | OIDC для dashboard |
| **Audit log** | Immutable log: who healed what, diff, commit, CI run id |
| **RBAC** | Команды, проекты, секреты не в отчётах |
| **On-prem registry** | Хранение сигнатур/scenario DB внутри контура |
| **Air-gapped MCP** | MCP server без внешней сети |
| **Priority support** | SLA 4h, выделенный канал |

**Ориентир:** годовой контракт + внедрение (не блокирует InSourceHub OSS внутри того же банка, если политика «внутренний OSS бесплатен»).

---

### Tier: Managed (опционально, v2+)

| Фича | Описание |
|------|----------|
| Hosted analytics | Агрегированный UI drift dashboard |
| Flaky intelligence | C003 patterns at scale |
| Agent audit retention | F011 long-term storage |

**Для банка:** обычно **не продаём cloud** — только on-prem appliance или managed **внутри** контура заказчика.

---

## Что НЕ монетизируем (слабые гипотезы — отказ)

| Идея | Почему нет |
|------|------------|
| «Просто дашборды» | Слабый wedge, Allure/ReportPortal есть |
| Paywall на healing | Убивает OSS adoption и InSourceHub |
| ML healing как paid | Против позиционирования no-ML |
| Облачный inference | Неприемлемо для ЦА банка |

---

## Сильные paid wedges (фиксировано)

1. **Policy + audit + RBAC** — compliance, ИБ.
2. **CI enterprise plugins** — GitLab/Jenkins с approval workflow.
3. **On-prem scenario registry** — multi-team в банке.
4. **Support / training** — внедрение в Legacy Selenium командах.
5. **Agent compliance (F011)** — audit MCP tool calls для AI-команд (enterprise).

---

## GTM: банк → внешний рынок

```
InSourceHub (OSS, 0₽ внутри)
    → 3+ команд, метрики CP/benchmark
    → Case study T005 (без NDA утечек)
    → GitHub public + Habr
    → 2–3 внешних enterprise пилота (paid Tier Enterprise)
```

---

## Метрики монетизации

| Этап | Метрика |
|------|---------|
| M6 | 2–3 внешних enterprise пилота |
| M12 | ARR от support + enterprise licenses |
| Внутри банка | Adoption teams (не ARR) |

---

## Сравнение с конкурентами

| | Healenium Pro | Testim/Mabl | fletta |
|---|---------------|-------------|--------|
| OSS core | Частично | Нет | **Да** |
| On-prem | Да | Ограничено | **Да, по умолчанию** |
| Paid | Pro features | Subscription | Policy, audit, support |

---

## Связанные документы

- [audience.md](./audience.md) — InSourceHub
- [positioning.md](./positioning.md)
- [roadmap.md](./roadmap.md) — M4-6 enterprise
- [talk-topics.md](./talk-topics.md) — T005
