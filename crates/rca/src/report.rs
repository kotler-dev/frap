use crate::classifier::{classify, failure_timestamp};
use crate::rules::PrimaryCause;
use frapcode_context::timeline::{Event, Timeline};
use serde::{Deserialize, Serialize};

pub const RCA_REPORT_VERSION: u32 = 1;
pub const DEFAULT_WINDOW_MS: i64 = 5000;

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct RcaReport {
    pub version: u32,
    pub primary_cause: PrimaryCause,
    pub confidence: f64,
    pub timeline_excerpt: Vec<Event>,
    pub recommendation: String,
    pub details: crate::rules::CauseDetails,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub failure_at_ms: Option<i64>,
}

impl RcaReport {
    pub fn from_timeline(timeline: &Timeline, failure_at_ms: i64, window_ms: i64) -> Self {
        let failure_at = if failure_at_ms > 0 {
            failure_at_ms
        } else {
            failure_timestamp(timeline).unwrap_or(0)
        };

        let cause = classify(timeline, failure_at, window_ms);
        let excerpt = crate::aggregate::asymmetric_window(timeline, failure_at, window_ms);

        Self {
            version: RCA_REPORT_VERSION,
            primary_cause: cause.primary,
            confidence: cause.confidence,
            timeline_excerpt: excerpt,
            recommendation: cause.recommendation,
            details: cause.details,
            failure_at_ms: if failure_at > 0 {
                Some(failure_at)
            } else {
                None
            },
        }
    }

    pub fn to_json(&self) -> Result<String, serde_json::Error> {
        serde_json::to_string_pretty(self)
    }

    pub fn from_json(json: &str) -> Result<Self, serde_json::Error> {
        serde_json::from_str(json)
    }
}

pub fn analyze_timeline_json(
    timeline_json: &str,
    failure_at_ms: i64,
    window_ms: i64,
) -> Result<String, serde_json::Error> {
    let timeline = Timeline::from_json(timeline_json)?;
    let report = RcaReport::from_timeline(&timeline, failure_at_ms, window_ms);
    report.to_json()
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::rules::PrimaryCause;
    use frapcode_context::network::{NetworkEvent, NetworkPhase, NetworkProtocol};
    use frapcode_context::timeline::Event;

    #[test]
    fn report_json_roundtrip() {
        let mut timeline = Timeline::new();
        timeline.push(Event::network(
            8000,
            Some("t1".into()),
            NetworkEvent {
                method: "POST".into(),
                url: "/api/payment-intent".into(),
                status: Some(504),
                duration_ms: Some(8000),
                phase: NetworkPhase::Response,
                protocol: NetworkProtocol::Http,
                direction: None,
                payload_preview: None,
                error_text: None,
            },
        ));
        timeline.push(Event::ui(
            10_000,
            Some("t1".into()),
            "[data-testid=pay-btn]",
            "not_found",
            None,
        ));

        let report = RcaReport::from_timeline(&timeline, 10_000, 5000);
        assert_eq!(report.primary_cause, PrimaryCause::ApiError);
        assert_eq!(report.version, 1);
        assert!(!report.timeline_excerpt.is_empty());

        let json = report.to_json().unwrap();
        let parsed = RcaReport::from_json(&json).unwrap();
        assert_eq!(parsed.primary_cause, PrimaryCause::ApiError);
    }
}
