use crate::rules::{endpoint_path, latency_hint, CauseDetails, PrimaryCause, RootCause};
use fletta_context::timeline::{event_timestamp_ms, Event, Timeline};

const FLAKY_SPREAD_MS: u64 = 400;

/// Detect flaky latency spread for repeated API calls (C003-style).
pub fn detect_flaky_pattern(timeline: &Timeline) -> Option<RootCause> {
    let mut durations_by_endpoint: std::collections::BTreeMap<String, Vec<u64>> =
        std::collections::BTreeMap::new();

    for event in &timeline.events {
        if let Event::Network { request, .. } = event {
            let latency = latency_hint(&request.url, request.duration_ms);
            if let Some(latency_ms) = latency {
                if request.phase == fletta_context::network::NetworkPhase::Response
                    || request.phase == fletta_context::network::NetworkPhase::Request
                {
                    let path = endpoint_path(&request.url);
                    if path.contains("/api/cart") || request.duration_ms.is_some() {
                        durations_by_endpoint
                            .entry(path)
                            .or_default()
                            .push(latency_ms);
                    }
                }
            }
        }
    }

    let mut best: Option<(String, u64, u64)> = None;
    for (endpoint, durations) in durations_by_endpoint {
        if durations.len() < 2 {
            continue;
        }
        let min = *durations.iter().min().unwrap();
        let max = *durations.iter().max().unwrap();
        let spread = max.saturating_sub(min);
        if spread >= FLAKY_SPREAD_MS {
            if best.as_ref().map(|(_, _, s)| spread > *s).unwrap_or(true) {
                best = Some((endpoint, min, max));
            }
        }
    }

    let (endpoint, min, max) = best?;
    let spread = max.saturating_sub(min);
    let correlation = if max > 0 {
        spread as f64 / max as f64
    } else {
        0.0
    };
    let pattern = format!("api latency spread >= {FLAKY_SPREAD_MS}ms ({min}ms–{max}ms)");

    Some(RootCause {
        primary: PrimaryCause::Flaky,
        confidence: (0.75 + correlation * 0.2).min(0.95),
        details: CauseDetails {
            endpoint: Some(endpoint.clone()),
            pattern: Some(pattern),
            correlation: Some(correlation),
            ..CauseDetails::default()
        },
        recommendation: format!(
            "Investigate intermittent latency for {endpoint}; observed spread {spread}ms"
        ),
    })
}

pub fn asymmetric_window(timeline: &Timeline, failure_at_ms: i64, window_ms: i64) -> Vec<Event> {
    let start = failure_at_ms - window_ms;
    timeline
        .events
        .iter()
        .filter(|e| {
            let t = event_timestamp_ms(e);
            t >= start && t <= failure_at_ms
        })
        .cloned()
        .collect()
}

#[cfg(test)]
mod tests {
    use super::*;
    use fletta_context::network::{NetworkEvent, NetworkPhase, NetworkProtocol};

    fn cart_timeline(fast_ms: u64, slow_delay: u64) -> Timeline {
        let mut t = Timeline::new();
        t.push(Event::network(
            1000,
            Some("fast".into()),
            NetworkEvent {
                method: "GET".into(),
                url: "/api/cart?delay=100".into(),
                status: Some(200),
                duration_ms: Some(fast_ms),
                phase: NetworkPhase::Response,
                protocol: NetworkProtocol::Http,
                direction: None,
                payload_preview: None,
                error_text: None,
            },
        ));
        t.push(Event::network(
            2000,
            Some("slow".into()),
            NetworkEvent {
                method: "GET".into(),
                url: format!("/api/cart?delay={slow_delay}"),
                status: None,
                duration_ms: None,
                phase: NetworkPhase::Request,
                protocol: NetworkProtocol::Http,
                direction: None,
                payload_preview: None,
                error_text: None,
            },
        ));
        t.push(Event::ui(
            2500,
            Some("slow".into()),
            "[data-testid=cart-ready]",
            "not_found",
            None,
        ));
        t
    }

    #[test]
    fn detects_flaky_cart_spread() {
        let timeline = cart_timeline(100, 600);
        let cause = detect_flaky_pattern(&timeline).expect("flaky");
        assert_eq!(cause.primary, PrimaryCause::Flaky);
        assert!(cause.details.pattern.as_ref().unwrap().contains("spread"));
        assert!(cause.confidence >= 0.75);
    }

    #[test]
    fn no_flaky_when_single_response() {
        let mut t = Timeline::new();
        t.push(Event::network(
            1000,
            None,
            NetworkEvent {
                method: "GET".into(),
                url: "/api/cart".into(),
                status: Some(200),
                duration_ms: Some(100),
                phase: NetworkPhase::Response,
                protocol: NetworkProtocol::Http,
                direction: None,
                payload_preview: None,
                error_text: None,
            },
        ));
        assert!(detect_flaky_pattern(&t).is_none());
    }
}
