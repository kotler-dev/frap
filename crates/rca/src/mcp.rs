use crate::report::RcaReport;
use crate::rules::PrimaryCause;
use serde::{Deserialize, Serialize};

/// MCP-shaped `fletta/analyze` result (stub for F005).
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct McpAnalyzeResult {
    pub primary_cause: PrimaryCause,
    pub confidence: f64,
    pub details: crate::rules::CauseDetails,
    pub recommendation: String,
}

impl From<&RcaReport> for McpAnalyzeResult {
    fn from(report: &RcaReport) -> Self {
        Self {
            primary_cause: report.primary_cause,
            confidence: report.confidence,
            details: report.details.clone(),
            recommendation: report.recommendation.clone(),
        }
    }
}

impl RcaReport {
    pub fn to_mcp_analyze_result(&self) -> McpAnalyzeResult {
        McpAnalyzeResult::from(self)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::report::RcaReport;
    use crate::rules::CauseDetails;
    use fletta_context::timeline::Timeline;

    #[test]
    fn mcp_result_matches_fixture_shape() {
        let report = RcaReport {
            version: 1,
            primary_cause: PrimaryCause::ApiError,
            confidence: 0.92,
            timeline_excerpt: vec![],
            recommendation: "Check backend latency".into(),
            details: CauseDetails {
                endpoint: Some("/api/payment-intent".into()),
                status: Some(504),
                ..CauseDetails::default()
            },
            failure_at_ms: Some(10_000),
        };

        let mcp = report.to_mcp_analyze_result();
        let json = serde_json::to_string(&mcp).unwrap();
        let parsed: McpAnalyzeResult = serde_json::from_str(&json).unwrap();
        assert_eq!(parsed.primary_cause, PrimaryCause::ApiError);
        assert_eq!(
            parsed.details.endpoint.as_deref(),
            Some("/api/payment-intent")
        );
    }

    #[test]
    fn fixture_file_roundtrip() {
        let fixture = include_str!("../tests/fixtures/analyze_response.json");
        let parsed: McpAnalyzeResult = serde_json::from_str(fixture).unwrap();
        assert_eq!(parsed.primary_cause, PrimaryCause::ApiError);
        assert!(parsed.confidence > 0.9);

        let _timeline = Timeline::new();
        let _ = parsed;
    }
}
