use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum PrimaryCause {
    UiChange,
    ApiError,
    Infrastructure,
    Flaky,
    Unknown,
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize, Default)]
#[serde(rename_all = "snake_case")]
pub struct CauseDetails {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub endpoint: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status: Option<u16>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub element: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pattern: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub correlation: Option<f64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub component: Option<String>,
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct RootCause {
    pub primary: PrimaryCause,
    pub confidence: f64,
    pub details: CauseDetails,
    pub recommendation: String,
}

impl RootCause {
    pub fn unknown(reason: &str) -> Self {
        Self {
            primary: PrimaryCause::Unknown,
            confidence: 0.0,
            details: CauseDetails::default(),
            recommendation: reason.to_string(),
        }
    }
}

pub fn endpoint_path(url: &str) -> String {
    let without_query = url.split('?').next().unwrap_or(url);
    let without_fragment = without_query.split('#').next().unwrap_or(without_query);
    if let Some(idx) = without_fragment.find("://") {
        let after_scheme = &without_fragment[idx + 3..];
        if let Some(path) = after_scheme.find('/') {
            return without_fragment[idx + 3 + path..].to_string();
        }
        return "/".to_string();
    }
    if without_fragment.starts_with('/') {
        without_fragment.to_string()
    } else {
        format!("/{without_fragment}")
    }
}

pub fn latency_hint(url: &str, duration_ms: Option<u64>) -> Option<u64> {
    if let Some(d) = duration_ms {
        return Some(d);
    }
    let query = url.split('?').nth(1)?;
    for part in query.split('&') {
        if let Some((key, value)) = part.split_once('=') {
            if key == "delay" {
                return value.parse().ok();
            }
        }
    }
    None
}

pub fn is_infrastructure_status(status: u16) -> bool {
    matches!(status, 502 | 503)
}
