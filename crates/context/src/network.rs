use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, Default)]
#[serde(rename_all = "snake_case")]
pub enum NetworkProtocol {
    #[default]
    Http,
    WebSocket,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum MessageDirection {
    Sent,
    Received,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum NetworkPhase {
    Request,
    Response,
    Failed,
    Open,
    Message,
    Close,
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct NetworkEvent {
    pub method: String,
    pub url: String,
    pub status: Option<u16>,
    pub duration_ms: Option<u64>,
    pub phase: NetworkPhase,
    #[serde(default)]
    pub protocol: NetworkProtocol,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub direction: Option<MessageDirection>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub payload_preview: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error_text: Option<String>,
}

impl NetworkEvent {
    pub fn is_failure(&self) -> bool {
        self.phase == NetworkPhase::Failed
            || self.error_text.is_some()
            || self.status.is_some_and(|s| s >= 400)
    }
}
