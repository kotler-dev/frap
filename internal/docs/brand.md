# Frap Brand & Domain Strategy

Стратегия доменных имен, брендинга и digital presence для Frap.

---

## Domain Portfolio

| Domain | Purpose | Priority | Status | Timeline |
|--------|---------|----------|--------|----------|
| **frapcode.dev** | Основной сайт, документация, landing pages | P0 | Зарегистрировать сейчас | MVP |
| **Frap.org** | Open-source организация, GitHub Pages mirror | P1 | Зарегистрировать после MVP | v1.0 |
| **Frap.ru** | Русскоязычная документация (InSourceHub) | P1 | Опционально, если нужен RU presence | v1.1 |
| **Frap.tools** | CLI endpoint, инструменты, API docs | P2 | Поддомены основного | v1.2 |
| **Frap.cloud** | Managed cloud offering (future commercial) | P3 | Не регистрировать сейчас | v3.0+ |

---

## frapcode.dev (Primary)

### Структура сайта

```
frapcode.dev/
├── /                      # Landing page: structure discovery engine
├── /docs                  # Полная документация
│   ├── /quickstart        # 5-minute getting started
│   ├── /architecture      # ADRs, design docs
│   ├── /api               # CLI reference, SDK docs
│   └── /guides            # Tutorials, examples
├── /playground            # Interactive element map demo
│   └── [WASM demo: paste URL → see element map]
├── /mcp                   # MCP integration docs
├── /enterprise            # Enterprise features, pricing
└── /blog                  # Release notes, case studies
```

### Техническая реализация

**Phase 1 (MVP): GitHub Pages**
```yaml
# GitHub Pages configuration
Repository: Frap/frapcode.dev
Branch: main /docs folder
Custom domain: frapcode.dev
SSL: Automatic (Let's Encrypt)
CDN: GitHub's CDN
```

**Phase 2 (v2.0): Self-hosted**
- Netlify или Vercel для better performance
- ISR (Incremental Static Regeneration) для docs
- Edge functions для playground demo

### Поддомены (tools.frapcode.dev)

| Subdomain | Purpose | Implementation |
|-----------|---------|----------------|
| `cli.frapcode.dev` | CLI download links, installation docs | Redirect to GitHub releases |
| `registry.frapcode.dev` | Enhancement registry (future) | NPM-like for adapters |
| `status.frapcode.dev` | Service status (managed tier) | Status page |

---

## Frap.org (Open Source)

### Назначение

- Community-driven content
- GitHub Pages mirror (redundancy)
- Internationalization foundation
- Non-commercial positioning

### Контент

```
Frap.org/
├── /community             # Contributors, governance
├── /i18n                  # Internationalization effort
│   ├── /ru               # Russian translation
│   ├── /zh               # Chinese translation
│   └── ...
└── /foundation            # Если создаём foundation
```

---

## Frap.ru (Russian)

### Целевая аудитория

- InSourceHub в российских банках
- Russian-speaking developers
- CIS market expansion

### Контент

- Переводы core документации
- Case studies из российских компаний
- Локальные митапы, конференции

### Регистрация

**Рекомендация:** Держать на frapcode.dev/ru, не отдельный домен.

Если отдельный домен критичен для SEO/присутствия:
- Зарегистрировать после v1.0 релиза
- 301 redirect с frapcode.dev/ru → Frap.ru

---

## Frap.tools (CLI & API)

### Назначение

- CLI endpoint для установки
- API documentation
- Tool-specific landing pages

### Примеры

```bash
# CLI installation
curl -fsSL https://tools.frapcode.dev/install.sh | sh

# Or
npm install -g @Frap/cli
# With registry: --registry https://registry.frapcode.dev
```

### Registry (Future)

```
registry.frapcode.dev/
├── /v1/adapters           # Platform adapters index
├── /v1/enhancements       # Enhancement packages
└── /v1/element-maps       # Community element maps (optional)
```

---

## Visual Identity

### Logo

**Requirements:**
- Должен работать в 16x16 (favicon) и 512x512 (app icon)
- Monochrome version для документации
- Не содержать текст (icon only)

**Concept directions:**
1. **Structure/Tree** — абстрактное дерево элементов
2. **Discovery/Magnifier** — лупа с структурой внутри
3. **Stability/Anchor** — якорь с цифровыми элементами

### Color Palette

| Role | Color | Hex | Usage |
|------|-------|-----|-------|
| Primary | Deep Blue | `#1a365d` | Headers, primary buttons |
| Secondary | Teal | `#319795` | Links, accents |
| Accent | Coral | `#ff6b6b` | CTAs, warnings |
| Success | Green | `#48bb78` | Success states |
| Background | Light Gray | `#f7fafc` | Page background |
| Text | Dark | `#2d3748` | Body text |

### Typography

- **Headers:** Inter or Space Grotesk (modern, technical)
- **Body:** Inter or Source Sans Pro (readable)
- **Code:** JetBrains Mono or Fira Code (developer-focused)

---

## Messaging Guidelines

### Tone of Voice

| Aspect | Description | Example |
|--------|-------------|---------|
| **Technical** | Precise, accurate | "Deterministic clustering algorithm" |
| **Clear** | No buzzwords | "Structure discovery" not "AI-powered" |
| **Confident** | No hedging | "Generates stable identifiers" not "tries to generate" |
| **Helpful** | Solution-focused | "Saves hours of manual analysis" |

### Forbidden Words

| Don't Use | Use Instead | Why |
|-----------|-------------|-----|
| "AI-powered" | "AI-ready", "deterministic" | Core has NO AI |
| "Self-healing" | "Resolution", "stable identification" | Technical, not magical |
| "Magic" | "Algorithm", "pattern matching" | Explainable by design |
| "Cloud" | "On-prem", "local" | Default is NO cloud |
| "Automatic" | "Deterministic", "automated" | Implies no randomness |

### Key Phrases

**Always use:**
- "Deterministic structure discovery engine"
- "Platform-agnostic element analysis"
- "NO ML in core"
- "Bank-grade, on-prem ready"
- "Gives AI agents reliable hands and eyes"

**Never use:**
- "Black box"
- "Neural network"
- "Deep learning"
- "Cloud-based"

---

## GitHub Organization

### Repository Structure

```
github.com/kotler-dev/frapcode/
├── Frap                 # Core monorepo
│   ├── /crates           # Rust crates
│   ├── /sdk              # TypeScript SDK
│   └── /docs             # Documentation source
│
├── frapcode.dev            # Website source (GitHub Pages)
├── Frap.org            # Community site (GitHub Pages)
│
├── adapters/
│   ├── playwright-adapter
│   └── selenium-adapter    # Future
│
├── enhancements/           # M3+
│   ├── semantic-naming
│   └── visual-matching
│
└── .github/
    ├── ISSUE_TEMPLATE
    ├── CONTRIBUTING.md
    └── CODE_OF_CONDUCT.md
```

### Organization Profile

**Display name:** Frap
**Description:** Deterministic UI structure discovery engine. NO ML in core. AI-ready via MCP.
**URL:** https://frapcode.dev
**Email:** hi@frapcode.dev

---

## Social Media

### Priority Channels

| Channel | Handle | Priority | Content |
|---------|--------|----------|---------|
| **Twitter/X** | @frapdev | P0 | Release notes, tips, community |
| **LinkedIn** | Frap | P1 | Enterprise, case studies |
| **YouTube** | Frap | P2 | Tutorials, demos |
| **Dev.to** | @Frap | P2 | Technical deep-dives |

### Handle Consistency

**Preferred:** `@frapdev` (Frap was taken on most platforms)

**Fallbacks:**
- `@frapeng` (engine)
- `@frapio` (if .io domain acquired)

### Content Calendar (MVP Phase)

**Week 1-2:**
- Twitter: "We're building..." teaser
- Blog: Introducing Frap post

**Week 3-4:**
- Twitter: Architecture thread (NO ML explanation)
- Dev.to: Technical deep-dive

**Week 5-8:**
- Twitter: Weekly tips (#FrapFriday)
- YouTube: Demo videos

---

## Legal & Compliance

### Trademark

**Recommendation:** Register trademark for "Frap" in:
1. US (USPTO) — primary market
2. EU (EUIPO) — secondary market
3. Russia (Rospatent) — if Frap.ru launched

**Timeline:** After v1.0 release, before v2.0

### License Headers

All source files should include:

```rust
// Copyright (c) 2026 Frap Contributors
// SPDX-License-Identifier: Apache-2.0
```

### Privacy Policy

Required sections:
- No telemetry by default
- Local processing only (core)
- Optional enhancement features (with consent)
- Data retention (none for core)

---

## Launch Checklist

### Pre-MVP
- [ ] Register frapcode.dev
- [ ] Set up GitHub Pages
- [ ] Create Twitter @frapdev
- [ ] Design logo (contract designer)

### MVP Launch
- [ ] Publish documentation
- [ ] Announce on Twitter
- [ ] Post on Hacker News
- [ ] Share in relevant subreddits

### Post-MVP
- [ ] Register Frap.org
- [ ] Apply for trademark
- [ ] Set up LinkedIn
- [ ] Create YouTube channel

---

## Related Documents

- [positioning.md](./positioning.md) — Brand messaging
- [monetization.md](./monetization.md) — Commercial strategy
- [strategy.md](./strategy.md) — Product strategy

---

*Version: 1.0.0 (MVP)*
*Last updated: 2026-05-23*
