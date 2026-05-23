use crate::logs::LogEvent;
use crate::network::NetworkEvent;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(tag = "kind", rename_all = "snake_case")]
pub enum Event {
    Ui {
        timestamp_ms: i64,
        #[serde(skip_serializing_if = "Option::is_none")]
        trace_id: Option<String>,
        element: String,
        action: String,
        #[serde(skip_serializing_if = "Option::is_none")]
        detail: Option<String>,
    },
    Network {
        timestamp_ms: i64,
        #[serde(skip_serializing_if = "Option::is_none")]
        trace_id: Option<String>,
        request: NetworkEvent,
    },
    Log {
        timestamp_ms: i64,
        #[serde(skip_serializing_if = "Option::is_none")]
        trace_id: Option<String>,
        log: LogEvent,
    },
}

#[derive(Debug, Clone, Default, PartialEq, Serialize, Deserialize)]
pub struct Timeline {
    pub events: Vec<Event>,
}

impl Timeline {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn push(&mut self, event: Event) {
        self.events.push(event);
    }

    pub fn sort_by_time(&mut self) {
        self.events
            .sort_by_key(|e| event_timestamp_ms(e));
    }

    /// Events with timestamps in [center_ms - window_ms, center_ms + window_ms].
    pub fn window(&self, center_ms: i64, window_ms: i64) -> Vec<Event> {
        let start = center_ms - window_ms;
        let end = center_ms + window_ms;
        self.events
            .iter()
            .filter(|e| {
                let t = event_timestamp_ms(e);
                t >= start && t <= end
            })
            .cloned()
            .collect()
    }

    pub fn from_json(json: &str) -> Result<Self, serde_json::Error> {
        serde_json::from_str(json)
    }

    pub fn to_json(&self) -> Result<String, serde_json::Error> {
        serde_json::to_string_pretty(self)
    }
}

pub fn event_timestamp_ms(event: &Event) -> i64 {
    match event {
        Event::Ui { timestamp_ms, .. }
        | Event::Network { timestamp_ms, .. }
        | Event::Log { timestamp_ms, .. } => *timestamp_ms,
    }
}

impl Event {
    pub fn ui(
        timestamp_ms: i64,
        trace_id: Option<String>,
        element: impl Into<String>,
        action: impl Into<String>,
        detail: Option<String>,
    ) -> Self {
        Event::Ui {
            timestamp_ms,
            trace_id,
            element: element.into(),
            action: action.into(),
            detail,
        }
    }

    pub fn network(timestamp_ms: i64, trace_id: Option<String>, request: NetworkEvent) -> Self {
        Event::Network {
            timestamp_ms,
            trace_id,
            request,
        }
    }

    pub fn log(timestamp_ms: i64, trace_id: Option<String>, log: LogEvent) -> Self {
        Event::Log {
            timestamp_ms,
            trace_id,
            log,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::logs::LogLevel;
    use crate::network::{NetworkPhase, NetworkProtocol};

    #[test]
    fn round_trip_json() {
        let mut timeline = Timeline::new();
        timeline.push(Event::network(
            1000,
            Some("trace-1".into()),
            NetworkEvent {
                method: "POST".into(),
                url: "/api/payment-intent".into(),
                status: None,
                duration_ms: Some(30_000),
                phase: NetworkPhase::Failed,
                protocol: NetworkProtocol::Http,
                direction: None,
                payload_preview: None,
                error_text: Some("timeout".into()),
            },
        ));
        timeline.push(Event::network(
            2000,
            Some("trace-1".into()),
            NetworkEvent {
                method: "WS".into(),
                url: "ws://localhost/ws/cart".into(),
                status: None,
                duration_ms: None,
                phase: NetworkPhase::Message,
                protocol: NetworkProtocol::WebSocket,
                direction: Some(crate::network::MessageDirection::Received),
                payload_preview: Some(r#"{"ok":true}"#.into()),
                error_text: None,
            },
        ));
        timeline.push(Event::log(
            1001,
            Some("trace-1".into()),
            LogEvent {
                level: LogLevel::Error,
                message: "payment failed".into(),
                source: "console".into(),
            },
        ));
        let json = timeline.to_json().unwrap();
        let parsed = Timeline::from_json(&json).unwrap();
        assert_eq!(parsed.events.len(), 3);
    }

    #[test]
    fn window_filters_events() {
        let mut timeline = Timeline::new();
        for t in [0, 4000, 5000, 6000, 11_000] {
            timeline.push(Event::ui(t, None, "btn", "wait", None));
        }
        timeline.sort_by_time();
        let w = timeline.window(5000, 5000);
        assert_eq!(w.len(), 4);
        assert_eq!(event_timestamp_ms(&w[0]), 0);
        assert_eq!(event_timestamp_ms(&w[3]), 6000);
    }

    #[test]
    fn sort_by_time_orders_events() {
        let mut timeline = Timeline::new();
        timeline.push(Event::ui(300, None, "a", "x", None));
        timeline.push(Event::ui(100, None, "b", "x", None));
        timeline.sort_by_time();
        assert_eq!(event_timestamp_ms(&timeline.events[0]), 100);
    }
}
