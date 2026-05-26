use crate::aggregate::{asymmetric_window, detect_flaky_pattern};
use crate::rules::{
    endpoint_path, is_infrastructure_status, CauseDetails, PrimaryCause, RootCause,
};
use frapcode_context::correlation::network_before_ui_failure;
use frapcode_context::logs::LogLevel;
use frapcode_context::timeline::{event_timestamp_ms, Event, Timeline};

const DEFAULT_WINDOW_MS: i64 = 5000;

pub fn failure_timestamp(timeline: &Timeline) -> Option<i64> {
    timeline
        .events
        .iter()
        .filter(|e| {
            matches!(
                e,
                Event::Ui {
                    action,
                    ..
                } if action == "not_found" || action == "failure"
            )
        })
        .map(event_timestamp_ms)
        .max()
}

pub fn classify(timeline: &Timeline, failure_at_ms: i64, window_ms: i64) -> RootCause {
    if timeline.events.is_empty() {
        return RootCause::unknown("Empty timeline — no events to analyze");
    }

    if let Some(flaky) = detect_flaky_pattern(timeline) {
        return flaky;
    }

    let failure_at = if failure_at_ms > 0 {
        failure_at_ms
    } else {
        failure_timestamp(timeline)
            .unwrap_or_else(|| event_timestamp_ms(timeline.events.last().unwrap()))
    };

    if failure_timestamp(timeline).is_none() && failure_at_ms <= 0 {
        return RootCause::unknown("No UI failure marker in timeline");
    }

    let window = asymmetric_window(timeline, failure_at, window_ms);

    if let Some(cause) = classify_network(&window, timeline, failure_at, window_ms) {
        return cause;
    }

    if let Some(cause) = classify_logs(&window, timeline, failure_at) {
        return cause;
    }

    if let Some(cause) = classify_ui_only(&window) {
        return cause;
    }

    RootCause::unknown("Could not determine root cause from timeline window")
}

fn ui_failure_in_window(window: &[Event]) -> Option<(i64, String)> {
    window
        .iter()
        .filter_map(|e| {
            if let Event::Ui {
                timestamp_ms,
                element,
                action,
                ..
            } = e
            {
                if action == "not_found" || action == "failure" {
                    return Some((*timestamp_ms, element.clone()));
                }
            }
            None
        })
        .max_by_key(|(t, _)| *t)
}

fn classify_network(
    window: &[Event],
    timeline: &Timeline,
    failure_at: i64,
    window_ms: i64,
) -> Option<RootCause> {
    let ui_fail = ui_failure_in_window(window)?;
    let (_, ui_element) = ui_fail;

    if !network_before_ui_failure(timeline, failure_at, window_ms) {
        return None;
    }

    let mut best_fail: Option<(i64, frapcode_context::network::NetworkEvent)> = None;
    for event in window {
        if let Event::Network {
            timestamp_ms,
            request,
            ..
        } = event
        {
            if request.is_failure()
                && best_fail
                    .as_ref()
                    .map(|(t, _)| timestamp_ms > t)
                    .unwrap_or(true)
            {
                best_fail = Some((*timestamp_ms, request.clone()));
            }
        }
    }

    let (_, request) = best_fail?;

    let endpoint = endpoint_path(&request.url);
    let status = request.status;

    if status.is_some_and(is_infrastructure_status) {
        return Some(RootCause {
            primary: PrimaryCause::Infrastructure,
            confidence: 0.88,
            details: CauseDetails {
                endpoint: Some(endpoint.clone()),
                status,
                component: Some("upstream".to_string()),
                ..CauseDetails::default()
            },
            recommendation: format!(
                "Check infrastructure health for {endpoint} (HTTP {})",
                status.unwrap_or(0)
            ),
        });
    }

    let mut confidence = 0.85;
    if status == Some(504) || request.error_text.is_some() {
        confidence = 0.92;
    }
    if has_correlated_error_log(window, &ui_element) {
        confidence = (confidence + 0.03_f64).min(0.97);
    }

    Some(RootCause {
        primary: PrimaryCause::ApiError,
        confidence,
        details: CauseDetails {
            endpoint: Some(endpoint.clone()),
            status,
            ..CauseDetails::default()
        },
        recommendation: format!("Check backend latency and errors for {endpoint}"),
    })
}

fn has_correlated_error_log(window: &[Event], _ui_element: &str) -> bool {
    window.iter().any(|e| {
        matches!(
            e,
            Event::Log {
                log,
                ..
            } if log.level == LogLevel::Error
        )
    })
}

fn classify_logs(window: &[Event], timeline: &Timeline, failure_at: i64) -> Option<RootCause> {
    let ui_fail = ui_failure_in_window(window)?;
    let (ui_at, ui_element) = ui_fail;

    let error_logs: Vec<_> = window
        .iter()
        .filter_map(|e| {
            if let Event::Log {
                timestamp_ms, log, ..
            } = e
            {
                if log.level == LogLevel::Error && *timestamp_ms <= ui_at {
                    Some((timestamp_ms, log.message.clone()))
                } else {
                    None
                }
            } else {
                None
            }
        })
        .collect();

    if error_logs.is_empty() {
        return None;
    }

    let has_network_in_window = window.iter().any(|e| matches!(e, Event::Network { .. }));
    if has_network_in_window && network_before_ui_failure(timeline, failure_at, DEFAULT_WINDOW_MS) {
        return None;
    }

    Some(RootCause {
        primary: PrimaryCause::Unknown,
        confidence: 0.55,
        details: CauseDetails {
            element: Some(ui_element),
            ..CauseDetails::default()
        },
        recommendation: format!(
            "Console error before UI failure: {}",
            error_logs.last().map(|(_, m)| m.as_str()).unwrap_or("")
        ),
    })
}

fn classify_ui_only(window: &[Event]) -> Option<RootCause> {
    let (ui_at, ui_element) = ui_failure_in_window(window)?;

    let has_network_fail = window.iter().any(|e| {
        matches!(
            e,
            Event::Network { request, .. } if request.is_failure()
        )
    });
    if has_network_fail {
        return None;
    }

    Some(RootCause {
        primary: PrimaryCause::UiChange,
        confidence: 0.6,
        details: CauseDetails {
            element: Some(ui_element.clone()),
            ..CauseDetails::default()
        },
        recommendation: format!(
            "UI element {ui_element} missing or changed at {ui_at}ms without network errors — verify selector or DOM changes"
        ),
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    use frapcode_context::logs::{LogEvent, LogLevel};
    use frapcode_context::network::{NetworkEvent, NetworkPhase, NetworkProtocol};

    fn c002_timeline() -> Timeline {
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
        t.push(Event::log(
            8100,
            Some("t1".into()),
            LogEvent {
                level: LogLevel::Error,
                message: "payment failed".into(),
                source: "console".into(),
            },
        ));
        t.push(Event::ui(
            10_000,
            Some("t1".into()),
            "[data-testid=pay-btn]",
            "not_found",
            None,
        ));
        t
    }

    #[test]
    fn c002_classifies_api_error() {
        let timeline = c002_timeline();
        let cause = classify(&timeline, 10_000, 5000);
        assert_eq!(cause.primary, PrimaryCause::ApiError);
        assert_eq!(
            cause.details.endpoint.as_deref(),
            Some("/api/payment-intent")
        );
        assert_eq!(cause.details.status, Some(504));
        assert!(cause.confidence >= 0.9);
    }

    #[test]
    fn failure_timestamp_picks_latest_ui_failure() {
        let timeline = c002_timeline();
        assert_eq!(failure_timestamp(&timeline), Some(10_000));
    }

    #[test]
    fn ui_only_classifies_ui_change() {
        let mut t = Timeline::new();
        t.push(Event::ui(
            5000,
            None,
            "[data-testid=submit]",
            "not_found",
            None,
        ));
        let cause = classify(&t, 5000, 5000);
        assert_eq!(cause.primary, PrimaryCause::UiChange);
    }

    #[test]
    fn empty_timeline_is_unknown() {
        let cause = classify(&Timeline::new(), 0, 5000);
        assert_eq!(cause.primary, PrimaryCause::Unknown);
    }
}
