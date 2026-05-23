use std::fmt;

/// Errors from the public Core API (JSON boundary, serialization).
#[derive(Debug)]
pub enum CoreError {
    Json(serde_json::Error),
}

impl fmt::Display for CoreError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            CoreError::Json(e) => write!(f, "json error: {e}"),
        }
    }
}

impl std::error::Error for CoreError {
    fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
        match self {
            CoreError::Json(e) => Some(e),
        }
    }
}

impl From<serde_json::Error> for CoreError {
    fn from(value: serde_json::Error) -> Self {
        CoreError::Json(value)
    }
}
