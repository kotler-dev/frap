# ADR-002: ML/LLM Enhancement Architecture

## Status
Accepted

## Context

Frap core is deterministic (NO ML). However, users may want optional ML/LLM enhancements:
- Semantic naming for PageObject methods (LLM)
- Visual element matching (OpenCV/ML)
- AI-guided test step generation (LLM)

These must be:
1. Optional — core works without them
2. Pluggable — users choose their provider
3. Secure — BYO-key or managed, no key storage in core
4. Observable — audit trail for AI decisions

## Decision

Enhancements live in separate packages (`Frap-enhancements`), implement trait interfaces, and are dynamically loaded by core.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Frap Core (NO ML)                     │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐│
│  │         EnhancementProvider Trait (optional)            ││
│  │                                                         ││
│  │  fn suggest_name(&self, element) -> Option<String>      ││
│  │  fn visual_similarity(&self, a, b) -> Option<f64>       ││
│  │  fn generate_steps(&self, req, map) -> Option<Vec<Step>>  ││
│  │                                                         ││
│  │  Default: NoEnhancement (returns None for all)          ││
│  └─────────────────────────────────────────────────────────┘│
└──────────────────────────┬──────────────────────────────────┘
                           │ Dynamic dispatch
        ┌──────────────────┼──────────────────┐
        ↓                  ↓                  ↓
┌───────────────┐  ┌───────────────┐  ┌───────────────┐
│ SemanticNaming │  │ VisualMatch   │  │ StepGenerator │
│   Adapter      │  │   Adapter     │  │   Adapter     │
├───────────────┤  ├───────────────┤  ├───────────────┤
│ LLM Client    │  │ OpenCV        │  │ LLM Client    │
│ (BYO-key)     │  │ (local)       │  │ (BYO-key)     │
└───────────────┘  └───────────────┘  └───────────────┘
```

## Enhancement Interfaces

### 1. Semantic Naming

```rust
// Frap-enhancements/semantic-naming/src/lib.rs

use frapcode_core::extensions::SemanticNamer;

pub struct LLMSemanticNamer {
    client: Box<dyn LLMClient>,
    cache: Arc<RwLock<HashMap<String, String>>>,
}

impl SemanticNamer for LLMSemanticNamer {
    fn suggest_name(&self, element: &Element) -> Option<String> {
        // Check cache
        let cache_key = format!("{:?}", element.signature);
        if let Some(cached) = self.cache.read().unwrap().get(&cache_key) {
            return Some(cached.clone());
        }
        
        // Build prompt
        let prompt = format!(
            "Suggest a descriptive method name for this UI element:\n\
             Role: {:?}\n\
             Text: {:?}\n\
             Context: {:?}\n\
             Return only the method name in camelCase.",
            element.signature.role,
            element.signature.text_normalized,
            element.cluster_context,
        );
        
        // Call LLM
        match self.client.complete(&prompt) {
            Ok(name) => {
                // Cache and return
                self.cache.write().unwrap().insert(cache_key, name.clone());
                Some(name)
            }
            Err(e) => {
                log::warn!("LLM naming failed: {}", e);
                None  // Fallback to core default
            }
        }
    }
}
```

### 2. Visual Matching

```rust
// Frap-enhancements/visual-matching/src/lib.rs

use frapcode_core::extensions::VisualMatcher;

pub struct OpenCVVisualMatcher {
    feature_detector: Arc<dyn FeatureDetector>,
}

impl VisualMatcher for OpenCVVisualMatcher {
    fn visual_similarity(&self, original: &Element, candidate: &Element) -> Option<f64> {
        // Both elements need visual snapshots
        let original_img = original.metadata.visual_snapshot?;
        let candidate_img = candidate.metadata.visual_snapshot?;
        
        // Extract features
        let original_features = self.feature_detector.detect(&original_img)?;
        let candidate_features = self.feature_detector.detect(&candidate_img)?;
        
        // Match features
        let matches = self.feature_detector.match_features(&original_features, &candidate_features);
        
        // Return similarity score 0.0-1.0
        Some(matches.similarity_score())
    }
}
```

### 3. Step Generation

```rust
// Frap-enhancements/step-generation/src/lib.rs

use frapcode_core::extensions::StepGenerator;

pub struct LLMStepGenerator {
    client: Box<dyn LLMClient>,
}

impl StepGenerator for LLMStepGenerator {
    fn generate_steps(
        &self,
        requirements: &str,
        element_map: &ElementMap,
    ) -> Option<Vec<Step>> {
        // Build context-rich prompt
        let prompt = format!(
            "Given this UI structure:\n{}\n\
             Generate test steps for: {}\n\
             Available elements: {}\n\
             Return JSON array of steps with element references.",
            self.format_element_map(element_map),
            requirements,
            self.format_available_elements(element_map),
        );
        
        match self.client.complete_structured::<Vec<Step>>(&prompt) {
            Ok(steps) => Some(steps),
            Err(e) => {
                log::warn!("Step generation failed: {}", e);
                None
            }
        }
    }
}
```

## BYO-Key Architecture

Users provide their own API keys. Core never stores or manages keys.

```yaml
# Frap.yml (user configuration)
enhancements:
  semantic_naming:
    enabled: true
    provider: openai
    api_key: "${OPENAI_API_KEY}"  # From environment
    model: gpt-4
    cache: true
    
  visual_matching:
    enabled: false  # No API key needed, uses local OpenCV
    
  step_generation:
    enabled: true
    provider: anthropic
    api_key: "${ANTHROPIC_API_KEY}"  # Different provider
    model: claude-sonnet-4
```

```rust
// Frap-enhancements/src/providers/mod.rs

pub fn create_llm_client(config: &LLMConfig) -> Box<dyn LLMClient> {
    match config.provider {
        "openai" => Box::new(OpenAIClient::new(&config.api_key)),
        "anthropic" => Box::new(AnthropicClient::new(&config.api_key)),
        "local" => Box::new(LocalLLMClient::new(&config.endpoint)),
        _ => panic!("Unknown provider: {}", config.provider),
    }
}
```

## Managed Tier (Enterprise)

Enterprise customers can use Frap-managed LLM access:

```yaml
# Frap.enterprise.yml (managed tier)
enhancements:
  semantic_naming:
    enabled: true
    provider: frap_managed
    api_key: null  # Managed by Frap
    
    # Enterprise features
    caching:
      enabled: true
      ttl_hours: 168  # 1 week
    rate_limiting:
      requests_per_minute: 60
      burst_allowance: 10
    audit:
      log_prompts: true  # For compliance
      log_responses: false
```

**Managed tier benefits:**
- No API key management for users
- Request caching (reduces costs)
- Rate limiting (prevents runaway costs)
- Usage analytics
- SLA guarantees

## Security Considerations

### Data Privacy

**Prompts sent to LLM may contain:**
- Element signatures (attributes, text)
- UI structure (DOM patterns)
- Test context (test names, URLs)

**Mitigations:**
1. **PII Detection:** Scan prompts for PII, redact or reject
2. **Data Classification:** Tag data sensitivity, route to appropriate provider
3. **Audit Logging:** Log all LLM interactions (with retention policy)
4. **User Consent:** Explicit opt-in for LLM enhancement usage

### API Key Security

**BYO-Key:**
- Keys stored in environment variables or secret management
- Never logged or persisted by Frap
- Rotatable by user at any time

**Managed:**
- Keys stored in Frap's secure vault (HashiCorp Vault, AWS KMS)
- Access audited, rotated automatically
- User never sees actual API key

## Testing Enhancements

### Mock Implementations

```rust
// Test fixture
pub struct MockSemanticNamer {
    predefined_names: HashMap<String, String>,
}

impl SemanticNamer for MockSemanticNamer {
    fn suggest_name(&self, element: &Element) -> Option<String> {
        let key = format!("{:?}", element.signature);
        self.predefined_names.get(&key).cloned()
    }
}

#[test]
fn test_page_object_generation_with_naming() {
    let mock_namer = MockSemanticNamer::new(vec![
        ("button-1-sig", "addToCartButton"),
        ("link-1-sig", "checkoutLink"),
    ]);
    
    let engine = FrapEngine::with_extensions(
        Arc::new(PermissivePolicy),
        Arc::new(ConsoleAuditLogger),
        Arc::new(mock_namer),
    );
    
    let po = engine.generate_page_object(&element_map);
    assert_eq!(po.methods[0].name, "addToCartButton");
}
```

### Isolated Test Suite

Enhancements have their own test suite, separate from core:

```
Frap-enhancements/
├── semantic-naming/
│   ├── src/
│   └── tests/
│       ├── llm_integration_tests.rs  # Requires API key
│       └── unit_tests.rs               # Mock-based
├── visual-matching/
│   └── tests/
└── step-generation/
    └── tests/
```

## Integration with Core

Enhancements are loaded dynamically:

```rust
// crates/core/src/extensions/loader.rs

pub fn load_enhancements(config: &Config) -> Arc<dyn EnhancementProvider> {
    if !config.enhancements.enabled {
        return Arc::new(NoEnhancement);
    }
    
    let mut composite = CompositeEnhancement::new();
    
    if config.enhancements.semantic_naming.enabled {
        let provider = load_semantic_naming(&config.enhancements.semantic_naming);
        composite.add_semantic_namer(provider);
    }
    
    if config.enhancements.visual_matching.enabled {
        let provider = load_visual_matcher(&config.enhancements.visual_matching);
        composite.add_visual_matcher(provider);
    }
    
    Arc::new(composite)
}
```

## Fallback Behavior

When enhancements fail or return None, core continues with default behavior:

| Enhancement | Fallback When Failed |
|-------------|---------------------|
| Semantic Naming | Use element ID as method name (e.g., `clickButton_7f3a9b`) |
| Visual Matching | Use signature-only matching (no visual features) |
| Step Generation | Return empty list, user writes steps manually |

## Related Documents

- [ADR-001: Platform-Agnostic Core](./platform-agnostic-core.md)
- [ADR-003: Extension Points](./extension-points.md)
- [monetization.md](../../docs/monetization.md) — Pricing for managed tier

---

*ADR Accepted: 2026-05-23*
