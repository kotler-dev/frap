//! Root cause analysis for Frap unified context timelines.

pub mod aggregate;
pub mod classifier;
pub mod mcp;
pub mod report;
pub mod rules;

pub use aggregate::{asymmetric_window, detect_flaky_pattern};
pub use classifier::{classify, failure_timestamp};
pub use mcp::McpAnalyzeResult;
pub use report::{analyze_timeline_json, RcaReport, DEFAULT_WINDOW_MS, RCA_REPORT_VERSION};
pub use rules::{endpoint_path, latency_hint, CauseDetails, PrimaryCause, RootCause};
