# ADR-001: Platform-Agnostic Core Architecture

## Status
Accepted

## Context

Fletta должен работать с разными UI платформами:
- Web: DOM + computed styles
- Android: ViewTree / AccessibilityNodeInfo
- iOS: UIView hierarchy
- Potential future: Desktop (Electron, Qt), API response schemas

Требования:
1. Единый алгоритмический core для всех платформ
2. Deterministic algorithms (NO ML dependencies)
3. Platform adapters для capture-specific tree formats
4. Unified output: element map format
5. Extensible для новых платформ без изменения core

## Decision

Core (Rust) реализует platform-agnostic analysis engine, работающий с abstract tree interface. Platform-specific адаптеры реализуют `Source` trait для предоставления raw tree.

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    Fletta Core (Rust/WASM)                   │
│                                                              │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────┐   │
│  │ Signature   │  │ Clustering   │  │ Resolution       │   │
│  │ Engine      │  │ (Drain3)     │  │ Engine          │   │
│  │             │  │              │  │                 │   │
│  │ - Compute   │  │ - Hierarchical│  │ - Match scores │   │
│  │   signatures│  │   clustering │  │ - Candidate   │   │
│  │ - Weight    │  │ - Pattern    │  │   ranking     │   │
│  │   attributes│  │   detection  │  │ - Confidence  │   │
│  └─────────────┘  └──────────────┘  └──────────────────┘   │
│                                                              │
│  NO ML dependencies. NO LLM client. Deterministic only.   │
└──────────────────────┬──────────────────────────────────────┘
                       │ Abstract Element Tree Interface
    ┌──────────────────┼──────────────────┐
    ↓                  ↓                  ↓
┌──────────┐      ┌──────────┐      ┌──────────┐
│  Source  │      │  Source  │      │  Source  │
│ Adapters │      │ Adapters │      │ Adapters │
├──────────┤      ├──────────┤      ├──────────┤
│ Chrome   │      │ Android  │      │   iOS    │
│   CDP    │      │UIAutomator     │      │ XCUITest │
│  (MVP)   │      │  (v2)    │      │  (v2)    │
└──────────┘      └──────────┘      └──────────┘
    │                  │                  │
    └──────────────────┴──────────────────┘
                       │
                       ↓
┌─────────────────────────────────────────────────────────────┐
│                  Unified Element Map                        │
│                                                              │
│  {                                                           │
│    "elements": [...],     // Stable IDs, signatures        │
│    "clusters": [...],     // Detected patterns              │
│    "confidenceScores": {}, // Per-element stability         │
│    "metadata": {...}      // Source, timestamp, URL        │
│  }                                                           │
└─────────────────────────────────────────────────────────────┘
```

### Core Crate Structure

```
crates/
├── core/                      # Platform-agnostic: NO external deps
│   ├── src/
│   │   ├── lib.rs            # Public API: discover(), resolve()
│   │   ├── signature.rs      # Signature computation
│   │   ├── clustering.rs     # Drain3 algorithm
│   │   ├── resolution.rs     # Matching engine
│   │   └── element_map.rs    # Output format
│   └── Cargo.toml            # NO tensorflow, NO ort, NO llm
│
├── sources/                   # Platform adapters
│   ├── web-chrome/           # Chrome DevTools Protocol (MVP)
│   │   ├── src/
│   │   │   ├── lib.rs
│   │   │   └── cdp.rs        # CDP client
│   │   └── Cargo.toml
│   │
│   ├── web-playwright/       # Optional: Playwright Page adapter
│   │   └── ...
│   │
│   ├── mobile-android/       # UIAutomator (v2)
│   │   └── ...
│   │
│   └── mobile-ios/          # XCUITest (v2)
│       └── ...
│
└── output/                    # Format + serialization
    ├── src/
    │   ├── element_map.rs    # Unified format
    │   └── code_gen/         # PageObject templates
    └── Cargo.toml
```

### Core Dependencies

```toml
# crates/core/Cargo.toml
[package]
name = "frap-core"
version = "0.1.0"
edition = "2021"

[dependencies]
# Serialization
serde = { version = "1.0", features = ["derive"] }
serde_json = "1.0"

# Algorithms (NO ML)
regex = "1"                    # For Drain3 tokenization
levenshtein = "1.0"           # For string similarity

# Utilities
thiserror = "1.0"             # Error handling
log = "0.4"                   # Logging (no-op by default)

# WASM target (optional, for JS SDK)
wasm-bindgen = { version = "0.2", optional = true }

[features]
default = []
wasm = ["wasm-bindgen"]
```

**Critical:** Core has NO `tensorflow`, NO `ort`, NO `llm-chain`, NO `openai` dependencies.

### Interfaces

#### Abstract Tree

```rust
// crates/core/src/element.rs

/// Platform-agnostic element representation
pub struct Element {
    pub id: ElementId,                    // Generated stable ID
    pub signature: Signature,              // Computed from attributes
    pub children: Vec<Element>,            // Tree structure
    pub metadata: ElementMetadata,        // Source-specific extras
}

/// Computed signature for stable identification
pub struct Signature {
    pub role: ElementRole,                // button, link, input, etc.
    pub attributes: HashMap<String, String>, // data-testid, aria-label, type
    pub text_normalized: Option<String>,  // Normalized visible text
    pub structural_path: Vec<u32>,       // Position in tree (resilient)
    pub computed: HashMap<String, f64>,   // Weighted scores
}

pub enum ElementRole {
    Button,
    Link,
    Input,
    Heading,
    Image,
    Container,
    Text,
    Custom(String),
}
```

#### Source Trait

```rust
// crates/core/src/source.rs

/// Platform-specific tree provider
pub trait Source {
    /// Capture current state and return raw element tree
    fn capture(&self) -> Result<RawElementTree, SourceError>;
    
    /// Platform identifier
    fn platform(&self) -> Platform;
}

pub enum Platform {
    WebChrome,
    WebPlaywright,
    Android,
    IOS,
}

/// Raw tree from platform (before core processing)
pub struct RawElementTree {
    pub root: RawElement,
    pub platform: Platform,
    pub timestamp: u64,
}

pub struct RawElement {
    pub tag: String,
    pub attributes: HashMap<String, String>,
    pub text: Option<String>,
    pub children: Vec<RawElement>,
    pub bounds: Option<Bounds>,  // For visual features
}
```

#### Element Map Output

```rust
// crates/output/src/element_map.rs

/// Unified output format (platform-agnostic)
pub struct ElementMap {
    pub elements: Vec<ElementNode>,
    pub clusters: Vec<Cluster>,
    pub confidence_scores: HashMap<String, f64>,
    pub metadata: MapMetadata,
}

pub struct ElementNode {
    pub id: String,                       // Stable ID (e.g., "el-7f3a9b")
    pub signature: SerializableSignature,  // For downstream use
    pub cluster_id: Option<String>,       // Membership in cluster
    pub confidence: f64,                  // Stability score 0.0-1.0
}

pub struct Cluster {
    pub id: String,
    pub cluster_type: ClusterType,         // List, grid, navigation, etc.
    pub element_ids: Vec<String>,
    pub pattern: PatternDescription,
}

pub struct MapMetadata {
    pub url: String,
    pub timestamp: u64,
    pub platform: String,
    pub source_version: String,
    pub coverage: CoverageStats,         // % of interactive elements captured
}
```

### Algorithms

#### 1. Signature Computation (Deterministic)

```rust
fn compute_signature(element: &RawElement) -> Signature {
    let role = detect_role(element);
    
    let attributes = extract_stable_attributes(element);
    // Stable: data-testid, aria-label, role, type, name
    // Unstable: class (randomized), id (auto-generated), style
    
    let text_normalized = element.text.as_ref()
        .map(|t| normalize_text(t));  // Lowercase, trim, collapse spaces
    
    let structural_path = compute_resilient_path(element);
    // Resilient to: siblings added/removed before this element
    // Sensitive to: parent changes
    
    Signature { role, attributes, text_normalized, structural_path }
}
```

**Weight computation (deterministic formula):**
```rust
fn compute_weight(attr: &str, value: &str) -> f64 {
    match attr {
        "data-testid" => 1.0,           // Most stable (if present)
        "aria-label" => 0.9,            // Semantic, rarely changes
        "role" => 0.8,
        "type" => 0.7,
        "name" => 0.6,
        _ => 0.3,                       // Custom attributes
    }
}
```

#### 2. Clustering (Drain3 Algorithm)

Drain3: hierarchical clustering based on token similarity.

```rust
fn cluster_elements(elements: &[Element]) -> Vec<Cluster> {
    // 1. Tokenize signatures into sequences
    let tokens: Vec<Vec<String>> = elements.iter()
        .map(|e| tokenize_signature(&e.signature))
        .collect();
    
    // 2. Build prefix tree (trie) of token sequences
    let mut trie = PrefixTree::new();
    for (i, token_seq) in tokens.iter().enumerate() {
        trie.insert(token_seq, i);
    }
    
    // 3. Merge similar branches (similarity > threshold)
    let clusters = trie.cluster_similar_branches(SIMILARITY_THRESHOLD);
    
    // 4. Assign cluster types based on patterns
    clusters.into_iter()
        .map(|c| assign_cluster_type(c))
        .collect()
}
```

**NO ML training.** Algorithm works on first run, no pre-trained models.

#### 3. Resolution (Matching)

```rust
fn resolve_element(
    original: &Signature,
    candidates: &[Element],
) -> ResolutionResult {
    let scores: Vec<(f64, &Element)> = candidates.iter()
        .map(|c| (compute_match_score(original, &c.signature), c))
        .collect();
    
    // Sort by score descending
    let mut ranked = scores;
    ranked.sort_by(|a, b| b.0.partial_cmp(&a.0).unwrap());
    
    let (best_score, best_candidate) = ranked[0];
    
    if best_score >= CONFIDENCE_THRESHOLD {
        ResolutionResult::Resolved {
            element_id: best_candidate.id.clone(),
            confidence: best_score,
            alternatives: ranked[1..4].to_vec(),  // Top-3 alternatives
        }
    } else {
        ResolutionResult::Ambiguous {
            candidates: ranked,
            reason: "Confidence below threshold",
        }
    }
}
```

**Match score formula (deterministic):**
```rust
fn compute_match_score(a: &Signature, b: &Signature) -> f64 {
    let role_match = if a.role == b.role { 1.0 } else { 0.0 };
    
    let attr_score = compare_attributes(&a.attributes, &b.attributes);
    // Weighted by attribute importance
    
    let text_score = match (&a.text_normalized, &b.text_normalized) {
        (Some(a_text), Some(b_text)) => levenshtein_similarity(a_text, b_text),
        _ => 0.0,
    };
    
    let path_score = compare_structural_paths(&a.structural_path, &b.structural_path);
    // Resilient to: sibling changes
    // Sensitive to: parent changes
    
    // Weighted combination (fixed weights, no learning)
    role_match * 0.3 + attr_score * 0.4 + text_score * 0.2 + path_score * 0.1
}
```

### WASM Compilation

Core compiles to WASM for JavaScript SDK:

```bash
# Build WASM package
cd crates/core
wasm-pack build --target web --features wasm

# Output: pkg/frap_core.js + pkg/frap_core_bg.wasm
```

**WASM exports:**
```rust
#[wasm_bindgen]
pub fn discover_wasm(url: &str, source_type: &str) -> JsValue {
    let source = create_source(source_type);
    let tree = source.capture().unwrap();
    let element_map = core::process(tree);
    
    JsValue::from_serde(&element_map).unwrap()
}
```

## Consequences

### Positive

1. **New platforms without core changes** — implement `Source` trait, core handles rest
2. **Deterministic results** — same input → same output, reproducible, testable
3. **No ML dependencies** — works offline, no GPU, no cloud API calls
4. **Audit-friendly** — every decision traceable to deterministic algorithm
5. **WASM-ready** — runs in browser, Node.js, edge functions
6. **Testable** — unit tests for each algorithm component

### Negative

1. **Adapter development required** — each platform needs custom `Source` implementation
2. **Some platform features may be lost** — abstraction means lowest common denominator
3. **Manual tuning for weights** — signature weights determined by analysis, not learned
4. **No "smart" adaptation** — algorithms don't improve with usage (by design, for determinism)

## Enterprise Extension Points

Placeholder traits for Enterprise tier:

```rust
// crates/core/src/policy.rs

/// Policy enforcement hook (placeholder in OSS)
pub trait PolicyChecker: Send + Sync {
    /// Approve or reject candidate resolution
    fn approve_resolution(
        &self,
        candidate: &Element,
        context: &ResolutionContext,
    ) -> PolicyDecision;
}

pub enum PolicyDecision {
    Approve,
    Reject { reason: String },
    RequireApproval { approver: String },
}

/// Default implementation: always approve (OSS behavior)
pub struct NoOpPolicyChecker;
impl PolicyChecker for NoOpPolicyChecker {
    fn approve_resolution(&self, _: &Element, _: &ResolutionContext) -> PolicyDecision {
        PolicyDecision::Approve
    }
}
```

```rust
// crates/core/src/audit.rs

/// Audit logging hook (placeholder in OSS)
pub trait AuditLogger: Send + Sync {
    fn log_resolution_attempt(&self, event: ResolutionAttempt);
    fn log_drift_detection(&self, event: DriftEvent);
}

/// Resolution event (serializable, for compliance)
pub struct ResolutionAttempt {
    // Core fields (always present)
    pub timestamp: u64,
    pub element_id: String,
    pub original_signature: Signature,
    pub candidates: Vec<CandidateInfo>,
    pub selected: Option<String>,
    pub confidence: f64,
    
    // Enterprise extensions (optional)
    pub policy_override: Option<String>,
    pub approver: Option<UserId>,
    pub audit_reason: Option<String>,
}
```

## Related Documents

- [ADR-002: Enhancement Architecture](./enhancements.md) — Optional ML/LLM adapters
- [ADR-003: Extension Points](./extension-points.md) — Enterprise hooks
- [glossary.md](../../docs/glossary.md) — Core, Source, Element Map, Resolution
- [strategy.md](../../docs/strategy.md) — 3 Layers architecture

## References

- Drain3 paper: "Drain: An Online Log Parsing Approach with Fixed Depth Tree"
- Levenshtein distance: classic string similarity algorithm
- WASM bindgen: https://github.com/rustwasm/wasm-bindgen

---

*ADR Accepted: 2026-05-23*
*Author: Fletta Architecture Team*
*Reviewers: [TBD]*
