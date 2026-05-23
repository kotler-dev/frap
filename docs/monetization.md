# Монетизация

Модель: **core всегда OSS (Apache-2.0, NO ML)** → **enhancements tier (Pro/BYO-key vs Enterprise/managed)** → **enterprise tier (compliance, support)**.

---

## Принципы

1. **Core всегда OSS** — structure discovery, resolution, clustering, SDK, CLI — deterministic algorithms only, NO ML dependencies.
2. **Enhancements опциональны** — ML/LLM features в отдельном пакете (`fletta-enhancements`), bring-your-own-key или managed.
3. **Банк использует core бесплатно** — InSourceHub strategy, monetization через внешний рынок.
4. **Нет paywall на core безопасность** — audit log базовый в OSS; расширенный compliance — paid enterprise tier.

---

## Tiers Overview

```
┌─────────────────────────────────────────────────────────────────┐
│  Tier 3: Enterprise                                             │
│  Policy engine • SSO/SAML • Extended audit • Support          │
│  On-prem registry • Air-gapped deployment                       │
│  $$$ — Annual contract                                          │
├─────────────────────────────────────────────────────────────────┤
│  Tier 2: Enhancements (Pro / Managed)                           │
│  Semantic naming • Visual matching • Step generation            │
│  Pro: BYO-key (OpenAI/Anthropic) • Managed: SLA + caching       │
│  $ — Per-seat or usage-based                                    │
├─────────────────────────────────────────────────────────────────┤
│  Tier 1: Core (Always Free, OSS)                               │
│  Structure discovery • Resolution • Clustering • CLI • SDK      │
│  NO ML • NO cloud calls • Apache-2.0                            │
│  $0 — Free forever                                              │
└─────────────────────────────────────────────────────────────────┘
```

---

## Tier 1: Core (Open Source, Free)

**Лицензия:** Apache-2.0

**What's included:**

| Component | Description |
|-----------|-------------|
| **Rust/WASM Core** | Signature computation, Drain3 clustering, resolution engine |
| **Sources** | Chrome/CDP, Playwright adapter |
| **CLI** | `fletta discover`, `fletta analyze`, `fletta resolve` |
| **SDK** | TypeScript SDK, Java SDK (roadmap) |
| **Element Map Format** | Open specification |
| **MCP Server** | `fletta/discover`, `fletta/analyze` tools |
| **Basic Audit** | Local logs, console output |
| **Documentation** | Full docs, quickstart guides |

**Ключевые характеристики:**
- **NO ML dependencies** — deterministic algorithms only
- **NO cloud API calls** — works offline, air-gapped
- **NO telemetry** — by default, zero data collection
- **Self-hosted** — no required external services

**User Value:**
- "Get structured UI maps in seconds, not hours"
- "Stable identifiers that survive refactoring"
- "Works on-prem without cloud dependencies"

---

## Tier 2: Enhancements (Pro / Managed)

**Package:** `@fletta/enhancements` (npm) / `fletta-enhancements` (crate)

### Pro: Bring-Your-Own-Key (BYO-key)

**Who:** Teams that want AI features, have their own API keys.

**Features:**

| Feature | Description | LLM Provider |
|---------|-------------|--------------|
| **Semantic Naming** | LLM-generated method names for PageObject | OpenAI, Anthropic, local |
| **Visual Matching** | Image-based element matching | OpenCV (local) |
| **Step Generation** | Generate test steps from requirements | OpenAI, Anthropic |

**Pricing:**
- **Free:** Package is free (Apache-2.0 license)
- **User pays:** API usage to their provider (OpenAI/Anthropic)
- **Support:** Community support only

**Example:**
```yaml
# User provides their own API key
enhancements:
  semantic_naming:
    enabled: true
    provider: openai
    api_key: "${OPENAI_API_KEY}"  # From user's environment
    model: gpt-4
```

### Managed: Fletta-Hosted

**Who:** Enterprises that don't want to manage API keys or need SLA.

**Features (Pro +):**

| Feature | Description |
|---------|-------------|
| **All Pro features** | Semantic naming, visual matching, step generation |
| **Request caching** | Reduce LLM costs (cache TTL: 7 days) |
| **Rate limiting** | Prevent runaway costs |
| **Usage analytics** | Dashboard: requests, costs, cache hit rate |
| **SLA guarantees** | 99.9% uptime for enhancement API |
| **No key management** | Fletta manages provider keys |

**Pricing Models:**

| Model | Price | Best For |
|-------|-------|----------|
| **Per-seat/month** | $20-50/user/month | Small teams, predictable usage |
| **Usage-based** | Per 1K requests | Variable usage, cost control |
| **Annual contract** | Custom | Large orgs, volume discounts |

**Example:**
```yaml
# Fletta-managed
enhancements:
  semantic_naming:
    enabled: true
    provider: fletta_managed
    # No API key needed
    caching: true
    rate_limit: 60/minute
```

---

## Tier 3: Enterprise

**For:** Banks, regulated industries, large enterprises.

**Includes:** Core (free) + Enhancements (managed or BYO-key) + Enterprise features.

### Enterprise Features

| Feature | Description | Why Paid |
|---------|-------------|----------|
| **Policy Engine** | Enforce rules: min confidence, blocked selectors, approval gates | Compliance, risk management |
| **SSO / SAML** | OIDC integration for team management | Enterprise identity |
| **Extended Audit Log** | Immutable, tamper-proof logs with retention policies | Compliance (SOX, GDPR) |
| **RBAC** | Role-based access: admin, QA, viewer | Team governance |
| **On-prem Registry** | Store element maps/scenarios inside security perimeter | Data sovereignty |
| **Air-gapped Deployment** | No external network required | Classified environments |
| **Priority Support** | SLA 4h response, dedicated channel | Business critical |
| **Training & Onboarding** | Custom workshops, documentation | Adoption acceleration |

### Enterprise Architecture

```
┌─────────────────────────────────────────┐
│         Enterprise Customer              │
│  ┌─────────────────────────────────┐   │
│  │   On-prem Fletta Deployment     │   │
│  │                                 │   │
│  │  ┌─────────┐  ┌──────────────┐  │   │
│  │  │  Core   │  │  Enterprise │  │   │
│  │  │  (OSS)  │  │   Plugins   │  │   │
│  │  │         │  │  - Policy    │  │   │
│  │  │ - Disco-│  │  - Audit     │  │   │
│  │  │   very  │  │  - RBAC      │  │   │
│  │  │ - Reso- │  │  - Registry  │  │   │
│  │  │   lution│  │              │  │   │
│  │  └─────────┘  └──────────────┘  │   │
│  │                                 │   │
│  │  ┌─────────────────────────┐   │   │
│  │  │   Local SIEM / Audit    │   │   │
│  │  │   (Splunk, ELK, etc.)   │   │   │
│  │  └─────────────────────────┘   │   │
│  └─────────────────────────────────┘   │
│                                         │
│  [Optional] Enhancement Managed        │
│  (via secure connection)                 │
└─────────────────────────────────────────┘
```

### Enterprise Pricing

**Annual contract:** Custom pricing based on:
- Number of teams/projects
- Element map volume
- Retention requirements
- Support SLA level

**Typical range:** $50K-500K/year

**Includes:**
- All Enterprise features
- Managed Enhancements (unlimited requests)
- Priority support (4h SLA)
- Quarterly business reviews
- Custom training

---

## What is NOT Monetized

| Idea | Why Not | Alternative |
|------|---------|-------------|
| **Core features paywall** | Destroys OSS adoption | Core always free |
| **Resolution as paid** | Core value proposition | Free in OSS |
| **Element map limits** | Anti-pattern for discovery | No limits |
| **Cloud inference required** | Against on-prem promise | Local only |
| **ML in core** | Against deterministic positioning | Enhancement tier |

---

## GTM Strategy: Bank → Market

```
┌────────────────────────────────────────────────────────┐
│  Phase 1: InSourceHub (Internal Bank)                   │
│  Core OSS — 0₽                                          │
│  • 3+ teams adopt                                       │
│  • Build case studies                                   │
│  • Validate enterprise features                         │
└────────────────────────────────────────────────────────┘
                        ↓
┌────────────────────────────────────────────────────────┐
│  Phase 2: GitHub Public                                │
│  Core OSS — 0₽                                          │
│  • Open source release                                  │
│  • Community adoption                                   │
│  • Issue triage, PRs                                    │
└────────────────────────────────────────────────────────┘
                        ↓
┌────────────────────────────────────────────────────────┐
│  Phase 3: Enhancements Launch                          │
│  Pro (BYO-key) — Free package, user pays API           │
│  Managed — $/seat or usage                             │
│  • Target: AI-native teams, startups                    │
└────────────────────────────────────────────────────────┘
                        ↓
┌────────────────────────────────────────────────────────┐
│  Phase 4: Enterprise Pilots                            │
│  Enterprise tier — $$$ annual contracts                 │
│  • 2-3 external banks/regulated                         │
│  • Case studies (with approval)                         │
└────────────────────────────────────────────────────────┘
```

---

## Comparison with Competitors

| | Healenium | Testim/Mabl | fletta |
|---|---|---|---|
| **Core** | Partially OSS | Closed | **Fully OSS (Apache-2.0)** |
| **ML in core** | Yes | Yes | **NO — deterministic** |
| **On-prem** | Proxy+DB | Limited | **Native, zero dependencies** |
| **Enhancements** | N/A | Built-in | **Optional, BYO-key or managed** |
| **Enterprise** | Support | Expensive | **Policy, audit, support** |
| **Pricing** | Freemium | $$$ subscription | **Core free, pay for value-adds** |

---

## Metrics & Success Criteria

| Phase | Timeline | Metric | Target |
|-------|----------|--------|--------|
| **InSourceHub** | M1-3 | Internal teams | 5+ teams in bank |
| **GitHub Stars** | M3-6 | Community interest | 500+ stars |
| **Enhancements** | M6-9 | Paid managed seats | 100+ seats |
| **Enterprise** | M9-12 | Annual contracts | 3 pilots, 1 closed |

---

## Related Documents

- [audience.md](./audience.md) — InSourceHub strategy
- [positioning.md](./positioning.md) — Value proposition by tier
- [architecture/enhancements.md](../project/architecture/enhancements.md) — Technical architecture
- [brand.md](./brand.md) — Domain strategy

---

*Version: 1.0.0 (MVP)*
*Last updated: 2026-05-23*
