package io.github.kotlerdev.frap.core.context;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Timeline of context events for RCA analysis.
 * Mirrors TypeScript {@code ContextTimeline} from context.ts.
 */
public record ContextTimeline(
    @JsonProperty("events") List<ContextEvent> events
) {
    public ContextTimeline {
        events = events != null ? new ArrayList<>(events) : new ArrayList<>();
    }

    public ContextTimeline() {
        this(new ArrayList<>());
    }

    /**
     * Adds an event to the timeline.
     */
    public void addEvent(ContextEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event cannot be null");
        }
        events.add(event);
    }

    /**
     * Returns events sorted by timestamp.
     * Mirrors TypeScript {@code sortTimelineEvents}.
     */
    public List<ContextEvent> sorted() {
        return events.stream()
            .sorted(Comparator.comparingLong(ContextEvent::timestampMs))
            .collect(Collectors.toList());
    }

    /**
     * Returns events within a window around a center time.
     * Mirrors TypeScript {@code getTimelineWindow}.
     */
    public List<ContextEvent> window(long centerMs, long windowMs) {
        long start = centerMs - windowMs;
        long end = centerMs + windowMs;
        return events.stream()
            .filter(e -> {
                long t = e.timestampMs();
                return t >= start && t <= end;
            })
            .sorted(Comparator.comparingLong(ContextEvent::timestampMs))
            .collect(Collectors.toList());
    }

    /**
     * Returns events filtered by trace ID.
     * Mirrors TypeScript {@code eventsByTraceId}.
     */
    public List<ContextEvent> byTraceId(String traceId) {
        if (traceId == null) {
            return Collections.emptyList();
        }
        return events.stream()
            .filter(e -> traceId.equals(e.traceId()))
            .sorted(Comparator.comparingLong(ContextEvent::timestampMs))
            .collect(Collectors.toList());
    }

    /**
     * Checks if a network failure precedes a UI failure in the window.
     * Mirrors TypeScript {@code networkBeforeUiFailure}.
     */
    public boolean networkBeforeUiFailure(long failureAtMs, long windowMs) {
        List<ContextEvent> windowEvents = window(failureAtMs, windowMs);

        Long lastNetworkFail = null;
        Long uiFailAt = null;

        for (ContextEvent event : windowEvents) {
            if (event instanceof ContextEvent.NetworkEvent net) {
                if (isNetworkFailure(net.request())) {
                    lastNetworkFail = net.timestampMs();
                }
            } else if (event instanceof ContextEvent.UiEvent ui) {
                if ("not_found".equals(ui.action()) || "failure".equals(ui.action())) {
                    uiFailAt = ui.timestampMs();
                }
            }
        }

        return lastNetworkFail != null && uiFailAt != null && lastNetworkFail < uiFailAt;
    }

    private boolean isNetworkFailure(NetworkEventPayload req) {
        if (req.phase() == NetworkPhase.FAILED) {
            return true;
        }
        if (req.errorText() != null && !req.errorText().isBlank()) {
            return true;
        }
        return req.status() != null && req.status() >= 400;
    }
}
