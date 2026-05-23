use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum LogLevel {
    Log,
    Info,
    Warn,
    Error,
    Debug,
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct LogEvent {
    pub level: LogLevel,
    pub message: String,
    #[serde(default = "default_source")]
    pub source: String,
}

fn default_source() -> String {
    "console".to_string()
}
