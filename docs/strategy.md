# Fletta Strategic Architecture: 3 Layers

Стратегическая архитектура развития Fletta — три слоя, от фундамента к AI-интеграции.

---

## Overview

Fletta развивается через три архитектурных слоя, каждый из которых строится на предыдущем:

```
┌─────────────────────────────────────────────────────────────┐
│ Layer 3: Observability & Feedback                             │
│   Drift Detection • RCA • Health Score • Audit Trail        │
│   [v2.0+, Months 4-6]                                       │
├─────────────────────────────────────────────────────────────┤
│ Layer 2: Generation Layer                                     │
│   PageObject Gen • Test Scenarios • Semantic API            │
│   [v1.2, Month 3]                                             │
├─────────────────────────────────────────────────────────────┤
│ Layer 1: Universal Element Discovery                          │
│   Core (Rust) • Sources • Element Map • NO ML               │
│   [v1.0 MVP, Months 1-2]                                      │
└─────────────────────────────────────────────────────────────┘
```

---

## Layer 1: Universal Element Discovery (Foundation)

**Цель:** Платформо-независимый анализ UI-структур с детерминированными алгоритмами.

**Компоненты:**

| Component | Responsibility | Technology |
|-----------|--------------|------------|
| **Core (Rust)** | Signature computation, clustering (Drain3), resolution | Rust/WASM, NO ML |
| **Sources** | Platform adapters: Chrome/CDP, UIAutomator, XCUITest | Platform-specific APIs |
| **Element Map** | Unified output format: stable IDs, clusters, confidence | JSON schema |

**Ключевые принципы:**
- **NO ML in core** — deterministic algorithms only
- **Platform-agnostic** — один element map format для Web, Android, iOS
- **On-prem ready** — zero external dependencies

**Deliverables (MVP):**
- `fletta discover` CLI → element map JSON
- Chrome/CDP source (primary)
- Signature + clustering algorithms
- Confidence scoring

**User Value:**
- "Получи структурированную карту UI за секунды вместо часов ручного анализа"
- Input для PageObject generation (Layer 2)

---

## Layer 2: Generation Layer (Artifacts)

**Цель:** Генерация maintainable кода из element maps.

**Компоненты:**

| Component | Input | Output | Status |
|-----------|-------|--------|--------|
| **PageObject Generator** | Element map + templates | Java/TS/Kotlin classes | v1.2 |
| **Test Scenario Builder** | Element map + flow definition | Test code (Playwright/Selenium) | v1.2 |
| **Semantic API** | Element map + naming conventions | Method names, documentation | v1.2 |

**Workflow:**

```
Element Map (Layer 1)
    ↓
┌──────────────────────────────────────┐
│ Generation Layer (Layer 2)           │
│                                      │
│  1. Select target language/framework │
│  2. Map elements to class structure  │
│  3. Generate stable selectors        │
│  4. Create semantic method names      │
│     (human or LLM via enhancement)   │
│  5. Output: PageObject + Tests       │
└──────────────────────────────────────┘
    ↓
Generated Code in User's Repo
```

**AI Integration (Enhancement Tier):**
- LLM может использовать element map для semantic naming
- Но: Fletta core не содержит LLM, enhancement adapter — опционально
- BYO-key или managed tier (M3+)

**User Value:**
- "PageObject с устойчивыми селекторами, которые переживут рефакторинг"
- "Не пиши XPath вручную — используй стабильные IDs из element map"

---

## Layer 3: Observability & Feedback (Intelligence)

**Цель:** Непрерывный мониторинг структуры, detection drift, root cause analysis.

**Компоненты:**

| Component | Function | Input | Output |
|-----------|----------|-------|--------|
| **Drift Detection** | Сравнение current vs baseline element map | Two element maps | Drift report (what changed) |
| **RCA Engine** | Классификация причин падений | Execution context | Root cause (UI/API/Network) |
| **Health Score** | Метрика стабильности теста | Historical runs | Stability score 0-100 |
| **Audit Trail** | Immutable log всех действий | All operations | Compliance-ready logs |

**Workflow:**

```
CI Pipeline / Scheduled Run
    ↓
┌──────────────────────────────────────┐
│ Observability Layer (Layer 3)        │
│                                      │
│  fletta discover → current map       │
│       ↓                            │
│  Compare with baseline             │
│       ↓                            │
│  IF drift detected:                │
│     - Generate drift report        │
│     - Attempt resolution           │
│     - Update baseline (if approved)  │
│  ELSE:                             │
│     - Health score ++              │
│     - Continue monitoring          │
└──────────────────────────────────────┘
```

**Drift Types:**
| Type | Detection | Action |
|------|-----------|--------|
| **Element Drift** | Signature changed | Resolution mapping |
| **Structural Drift** | Elements added/removed | Coverage update |
| **Cluster Drift** | Pattern changed | Re-clustering |

**User Value:**
- "Узнай об изменении UI до падения теста"
- "Почему упал тест?" — RCA за секунды вместо часов дебага
- "Audit trail для compliance" — кто что делал, когда, почему

---

## Timeline Mapping

| Phase | Timeline | Layer | Key Deliverables |
|-------|----------|-------|------------------|
| **MVP** | Months 1-2 | Layer 1 | `fletta discover`, Core (Rust), Chrome/CDP source |
| **v1.1** | Month 3 | Layer 1-2 | Unified Context (RCA foundation) |
| **v1.2** | Month 3-4 | Layer 2 | PageObject Gen, Semantic API |
| **v2.0** | Months 4-6 | Layer 3 | Drift Detection, Health Score |
| **v3.0** | Months 6-12 | Layer 1-3 | Mobile sources, Enterprise tier |

---

## AI Integration Across Layers

**Ключевой принцип:** Fletta не использует AI в core. AI — enhancement layer.

| Layer | Core (NO AI) | AI Enhancement (Optional) |
|-------|--------------|---------------------------|
| **Layer 1** | Signature computation, clustering | Visual features (OpenCV adapter) |
| **Layer 2** | Code generation templates | Semantic naming (LLM adapter) |
| **Layer 3** | Drift detection algorithms | Smart threshold learning |

**MCP Integration:**
- Layer 1: `fletta/discover` → element map для LLM grounding
- Layer 2: `fletta/generate` → output для AI review
- Layer 3: `fletta/analyze` → RCA для AI decision support

**Метафора:**
> **Fletta даёт AI-агенту надёжные руки и глаза** — structured input (element map), stable execution (resolution), explainable output (audit trail).

---

## Platform Expansion

Layer 1 architecture позволяет добавлять платформы без изменения core:

```
┌──────────────────────────────────────┐
│           Fletta Core               │
│  (Signature, Clustering, Resolution)│
│        Platform-Agnostic              │
└──────────────┬───────────────────────┘
               │ Abstract Tree Interface
    ┌──────────┼──────────┐
    ↓          ↓          ↓
┌───────┐  ┌───────┐  ┌───────┐
│ Web   │  │Android│  │  iOS  │
│Chrome │  │UIAuto │  │XCUITest
│  CDP  │  │ mator │  │       │
└───────┘  └───────┘  └───────┘
    │          │          │
    └──────────┴──────────┘
               │
               ↓
      Unified Element Map
```

**New Platform Checklist:**
1. Implement `Source` trait for platform
2. Map platform-specific elements to `RawElementTree`
3. Core handles the rest (clustering, signatures)
4. Output: same element map format

---

## Enterprise Extensions

All layers support enterprise hooks:

| Layer | Enterprise Feature | Implementation |
|-------|-------------------|----------------|
| **Layer 1** | Policy enforcement | `PolicyChecker` trait (stub) |
| **Layer 2** | Approval workflows | Generation approval gates |
| **Layer 3** | Audit logging | `AuditLogger` trait, immutable storage |

**Note:** Enterprise features — placeholders in OSS, full implementation in Enterprise tier.

---

## Related Documents

- [positioning.md](./positioning.md) — стратегическое позиционирование
- [glossary.md](./glossary.md) — терминология (Discover, Element Map, Resolution)
- [roadmap.md](./roadmap.md) — timeline с фазами развития
- [architecture/platform-agnostic-core.md](../project/architecture/platform-agnostic-core.md) — ADR Layer 1
- [architecture/enhancements.md](../project/architecture/enhancements.md) — AI enhancement architecture

---

*Версия: 1.0.0*
*Стратегия актуальна для: MVP → v3.0*
