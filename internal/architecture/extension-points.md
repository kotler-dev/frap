# ADR-003: Extension Points for Enterprise & Integrations

## Status
Accepted

## Context

Frap core provides deterministic structure discovery. However, enterprise users need additional capabilities:
- Policy enforcement (approve/reject resolutions)
- Audit logging (compliance requirements)
- Custom integrations (internal systems)

These capabilities should be:
1. Available in Enterprise tier without changing core logic
2. Placeholder/NoOp in OSS version
3. Extensible without forking

## Decision

Define trait-based extension points in core with NoOp default implementations. Enterprise and custom integrations implement these traits.

## Extension Points

### 1. Policy Checker

**Purpose:** Control which resolutions are allowed based on business rules.

```rust
// crates/core/src/extensions/policy.rs

/// Policy enforcement trait
pub trait PolicyChecker: Send + Sync {
    /// Evaluate if a resolution candidate should be approved
    fn evaluate(
        &self,
        candidate: &Element,
        original: &Signature,
        context: &ResolutionContext,
    ) -> PolicyDecision;
    
    /// Policy identifier for logging
    fn policy_id(&self) -> &str;
}

pub enum PolicyDecision {
    /// Allow this resolution
    Approve,
    
    /// Reject, use next candidate or fail
    Reject { reason: String },
    
    /// Require human approval (Enterprise workflow)
    RequireApproval {
        approver_role: String,
        justification_required: bool,
    },
    
    /// Override with specific element (policy knows better)
    Override { element_id: String, reason: String },
}

/// Resolution context for policy decisions
pub struct ResolutionContext {
    pub test_name: String,
    pub test_path: String,
    pub environment: String,      // prod, staging, etc.
    pub criticality: Criticality, // Critical path vs utility
    pub historical_accuracy: f64, // Past resolution success rate
}

pub enum Criticality {
    Critical,      // Payment flows, auth
    Standard,      // Regular features
    Utility,       // Dev tools, logs
}

/// Default OSS implementation: always approve
pub struct PermissivePolicy;
impl PolicyChecker for PermissivePolicy {
    fn evaluate(&self, _: &Element, _: &Signature, _: &ResolutionContext) -> PolicyDecision {
        PolicyDecision::Approve
    }
    
    fn policy_id(&self) -> &str {
        "permissive"
    }
}
```

**Enterprise Example:**
```rust
/// Block resolutions to certain elements in production
pub struct ProductionSafetyPolicy {
    blocked_selectors: Vec<String>,
    require_approval_for: Vec<String>,
}

impl PolicyChecker for ProductionSafetyPolicy {
    fn evaluate(&self, candidate: &Element, _: &Signature, ctx: &ResolutionContext) -> PolicyDecision {
        // Block admin panels in prod
        if ctx.environment == "production" && self.is_admin_element(candidate) {
            return PolicyDecision::Reject {
                reason: "Admin elements blocked in production".to_string(),
            };
        }
        
        // Require approval for payment flows
        if ctx.criticality == Criticality::Critical {
            return PolicyDecision::RequireApproval {
                approver_role: "qa-lead".to_string(),
                justification_required: true,
            };
        }
        
        PolicyDecision::Approve
    }
}
```

### 2. Audit Logger

**Purpose:** Immutable log of all operations for compliance and debugging.

```rust
// crates/core/src/extensions/audit.rs

/// Audit logging trait
pub trait AuditLogger: Send + Sync {
    /// Log a resolution attempt
    fn log_resolution(&self, event: ResolutionEvent);
    
    /// Log a drift detection
    fn log_drift(&self, event: DriftEvent);
    
    /// Log a discovery operation
    fn log_discovery(&self, event: DiscoveryEvent);
    
    /// Flush logs (for batching implementations)
    fn flush(&self) -> Result<(), AuditError>;
}

/// Core resolution event (all fields serializable)
#[derive(Serialize)]
pub struct ResolutionEvent {
    // Required fields
    pub timestamp: u64,
    pub event_id: Uuid,
    pub element_id: String,
    pub test_identifier: TestId,
    
    pub original_signature: SerializableSignature,
    pub candidates: Vec<CandidateInfo>,
    pub selected_candidate: Option<String>,
    pub confidence_score: f64,
    pub policy_decision: PolicyDecision,
    
    // Optional enterprise fields
    #[serde(skip_serializing_if = "Option::is_none")]
    pub approver: Option<String>,
    
    #[serde(skip_serializing_if = "Option::is_none")]
    pub approval_timestamp: Option<u64>,
    
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy_override_reason: Option<String>,
}

/// Default OSS implementation: log to stdout/file only
pub struct ConsoleAuditLogger;
impl AuditLogger for ConsoleAuditLogger {
    fn log_resolution(&self, event: ResolutionEvent) {
        log::info!("Resolution: {:?}", event);
    }
    // ...
}

/// Enterprise implementation: structured logging to SIEM
pub struct EnterpriseAuditLogger {
    endpoint: String,
    api_key: String,
    buffer: Vec<AuditEvent>,
}
```

### 3. Custom Source Provider

**Purpose:** Allow users to add custom platform sources without forking.

```rust
// crates/core/src/extensions/source.rs

/// Dynamic source registration
pub trait SourceProvider: Send + Sync {
    /// Create a source for given platform type
    fn create_source(&self, config: &SourceConfig) -> Result<Box<dyn Source>, SourceError>;
    
    /// List supported platform types
    fn supported_platforms(&self) -> Vec<&str>;
}

/// Source configuration (serializable)
pub struct SourceConfig {
    pub platform_type: String,
    pub connection_string: String,
    pub authentication: AuthConfig,
    pub custom_params: HashMap<String, String>,
}
```

### 4. Enhancement Provider (ML/AI Integration)

See [ADR-002: Enhancements](./enhancements.md) for full details.

```rust
// crates/core/src/extensions/enhancement.rs

/// Optional ML/AI enhancement trait
pub trait EnhancementProvider: Send + Sync {
    /// Semantic naming for elements
    fn suggest_name(&self, element: &Element) -> Option<String>;
    
    /// Visual similarity score (if applicable)
    fn visual_similarity(&self, a: &Element, b: &Element) -> Option<f64>;
    
    /// Step generation from requirements
    fn generate_steps(&self, requirements: &str, element_map: &ElementMap) -> Option<Vec<Step>>;
}

/// Default: no enhancements (OSS)
pub struct NoEnhancement;
impl EnhancementProvider for NoEnhancement {
    fn suggest_name(&self, _: &Element) -> Option<String> { None }
    fn visual_similarity(&self, _: &Element, _: &Element) -> Option<f64> { None }
    fn generate_steps(&self, _: &str, _: &ElementMap) -> Option<Vec<Step>> { None }
}
```

## Integration in Core

Extension points are composed into the core engine:

```rust
// crates/core/src/engine.rs

pub struct FrapEngine {
    policy_checker: Arc<dyn PolicyChecker>,
    audit_logger: Arc<dyn AuditLogger>,
    enhancement: Arc<dyn EnhancementProvider>,
}

impl FrapEngine {
    /// Create with defaults (OSS)
    pub fn default() -> Self {
        Self {
            policy_checker: Arc::new(PermissivePolicy),
            audit_logger: Arc::new(ConsoleAuditLogger),
            enhancement: Arc::new(NoEnhancement),
        }
    }
    
    /// Create with custom extensions (Enterprise)
    pub fn with_extensions(
        policy: Arc<dyn PolicyChecker>,
        audit: Arc<dyn AuditLogger>,
        enhancement: Arc<dyn EnhancementProvider>,
    ) -> Self {
        Self {
            policy_checker: policy,
            audit_logger: audit,
            enhancement,
        }
    }
    
    pub fn resolve(&self, ctx: &ResolutionContext) -> ResolutionResult {
        // 1. Find candidates
        let candidates = self.find_candidates(ctx);
        
        // 2. Apply policy
        let approved: Vec<_> = candidates.iter()
            .filter(|c| matches!(
                self.policy_checker.evaluate(c, &ctx.original, ctx),
                PolicyDecision::Approve | PolicyDecision::Override { .. }
            ))
            .collect();
        
        // 3. Select best
        let result = self.select_best(approved);
        
        // 4. Audit log
        self.audit_logger.log_resolution(result.to_event());
        
        result
    }
}
```

## Enterprise Configuration

```yaml
# Frap.enterprise.yml (Enterprise tier)
extensions:
  policy:
    type: "production_safety"
    config:
      blocked_selectors:
        - "[data-testid='admin-panel']"
      require_approval_for:
        - "payment"
        - "user_delete"
  
  audit:
    type: "siem"
    config:
      endpoint: "https://siem.company.com/api/events"
      batch_size: 100
      flush_interval_secs: 60
  
  enhancement:
    type: "llm_bring_your_own_key"
    config:
      provider: "openai"
      api_key: "${OPENAI_API_KEY}"  # From env
      cache_dir: "/var/cache/Frap"
```

## Related Documents

- [ADR-001: Platform-Agnostic Core](./platform-agnostic-core.md)
- [ADR-002: Enhancements](./enhancements.md)
- [glossary.md](../../docs/glossary.md)

---

*ADR Accepted: 2026-05-23*
