use crate::timeline::{event_timestamp_ms, Event, Timeline};

/// Events sharing the same `trace_id`, sorted by time.
pub fn events_by_trace_id(timeline: &Timeline, trace_id: &str) -> Vec<Event> {
    let mut matched: Vec<Event> = timeline
        .events
        .iter()
        .filter(|e| event_trace_id(e).as_deref() == Some(trace_id))
        .cloned()
        .collect();
    matched.sort_by_key(|e| event_timestamp_ms(e));
    matched
}

/// Group events by trace_id (only events that have trace_id set).
pub fn group_by_trace_id(timeline: &Timeline) -> std::collections::BTreeMap<String, Vec<Event>> {
    let mut groups: std::collections::BTreeMap<String, Vec<Event>> =
        std::collections::BTreeMap::new();
    for event in &timeline.events {
        if let Some(id) = event_trace_id(event) {
            groups.entry(id).or_default().push(event.clone());
        }
    }
    for events in groups.values_mut() {
        events.sort_by_key(|e| event_timestamp_ms(e));
    }
    groups
}

/// First network failure or HTTP error before a UI failure in the window.
pub fn network_before_ui_failure(timeline: &Timeline, failure_at_ms: i64, window_ms: i64) -> bool {
    let window = timeline.window(failure_at_ms, window_ms);
    let mut last_network_fail: Option<i64> = None;
    let mut ui_fail_at: Option<i64> = None;

    for event in window {
        match &event {
            Event::Network {
                timestamp_ms,
                request,
                ..
            } => {
                if request.is_failure() {
                    last_network_fail = Some(*timestamp_ms);
                }
            }
            Event::Ui {
                timestamp_ms,
                action,
                ..
            } if action == "not_found" || action == "failure" => {
                ui_fail_at = Some(*timestamp_ms);
            }
            _ => {}
        }
    }

    match (last_network_fail, ui_fail_at) {
        (Some(n), Some(u)) => n < u,
        _ => false,
    }
}

fn event_trace_id(event: &Event) -> Option<String> {
    match event {
        Event::Ui { trace_id, .. }
        | Event::Network { trace_id, .. }
        | Event::Log { trace_id, .. } => trace_id.clone(),
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::logs::{LogEvent, LogLevel};
    use crate::network::{NetworkEvent, NetworkPhase, NetworkProtocol};

    fn sample_timeline() -> Timeline {
        let mut t = Timeline::new();
        t.push(Event::network(
            1000,
            Some("t1".into()),
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
        t.push(Event::ui(
            1500,
            Some("t1".into()),
            "[data-testid=pay-btn]",
            "not_found",
            None,
        ));
        t.push(Event::log(
            900,
            Some("t2".into()),
            LogEvent {
                level: LogLevel::Info,
                message: "other".into(),
                source: "console".into(),
            },
        ));
        t
    }

    #[test]
    fn groups_by_trace_id() {
        let timeline = sample_timeline();
        let groups = group_by_trace_id(&timeline);
        assert_eq!(groups.get("t1").map(|v| v.len()), Some(2));
        assert_eq!(groups.get("t2").map(|v| v.len()), Some(1));
    }

    #[test]
    fn events_by_trace_id_filters_and_sorts() {
        let timeline = sample_timeline();
        let t1 = events_by_trace_id(&timeline, "t1");
        assert_eq!(t1.len(), 2);
        assert_eq!(event_timestamp_ms(&t1[0]), 1000);
        assert_eq!(event_timestamp_ms(&t1[1]), 1500);
        assert!(events_by_trace_id(&timeline, "missing").is_empty());
    }

    #[test]
    fn network_precedes_ui_failure() {
        let timeline = sample_timeline();
        assert!(network_before_ui_failure(&timeline, 1500, 5000));
    }

    #[test]
    fn http_error_status_counts_as_network_failure() {
        let mut t = Timeline::new();
        t.push(Event::network(
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
        t.push(Event::ui(
            10_000,
            Some("t1".into()),
            "[data-testid=pay-btn]",
            "not_found",
            None,
        ));
        assert!(network_before_ui_failure(&t, 10_000, 5000));
    }
}
