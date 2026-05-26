package io.github.kotlerdev.frap.core.context;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ContextTimeline utilities.
 */
class ContextTimelineTest {

    @Test
    void testSortByTime() {
        ContextTimeline timeline = new ContextTimeline();
        timeline.addEvent(new ContextEvent.UiEvent(3000L, "btn", "click"));
        timeline.addEvent(new ContextEvent.UiEvent(1000L, "btn", "hover"));
        timeline.addEvent(new ContextEvent.UiEvent(2000L, "btn", "focus"));

        List<ContextEvent> sorted = timeline.sorted();

        assertThat(sorted).hasSize(3);
        assertThat(sorted.get(0).timestampMs()).isEqualTo(1000L);
        assertThat(sorted.get(1).timestampMs()).isEqualTo(2000L);
        assertThat(sorted.get(2).timestampMs()).isEqualTo(3000L);
    }

    @Test
    void testWindow() {
        ContextTimeline timeline = new ContextTimeline();
        timeline.addEvent(new ContextEvent.UiEvent(1000L, "btn", "click"));
        timeline.addEvent(new ContextEvent.UiEvent(2000L, "btn", "focus"));
        timeline.addEvent(new ContextEvent.UiEvent(3000L, "btn", "failure"));
        timeline.addEvent(new ContextEvent.UiEvent(5000L, "btn", "retry"));

        List<ContextEvent> window = timeline.window(2000L, 1000L);

        assertThat(window).hasSize(2); // 2000 and 3000
        assertThat(window.get(0).timestampMs()).isEqualTo(2000L);
        assertThat(window.get(1).timestampMs()).isEqualTo(3000L);
    }

    @Test
    void testByTraceId() {
        ContextTimeline timeline = new ContextTimeline();
        timeline.addEvent(new ContextEvent.UiEvent(1000L, "trace-a", "btn", "click"));
        timeline.addEvent(new ContextEvent.UiEvent(2000L, "trace-b", "btn", "click"));
        timeline.addEvent(new ContextEvent.UiEvent(3000L, "trace-a", "btn", "failure"));

        List<ContextEvent> traceA = timeline.byTraceId("trace-a");

        assertThat(traceA).hasSize(2);
        assertThat(traceA.get(0).timestampMs()).isEqualTo(1000L);
        assertThat(traceA.get(1).timestampMs()).isEqualTo(3000L);
    }

    @Test
    void testNetworkBeforeUiFailure_true() {
        ContextTimeline timeline = new ContextTimeline();
        timeline.addEvent(new ContextEvent.NetworkEvent(
            1000L,
            "test",
            new NetworkEventPayload("POST", "/api", 500, null, NetworkPhase.RESPONSE, null, null, null, null)
        ));
        timeline.addEvent(new ContextEvent.UiEvent(2000L, "test", "[data-btn]", "failure"));

        boolean result = timeline.networkBeforeUiFailure(2000L, 5000L);

        assertThat(result).isTrue();
    }

    @Test
    void testNetworkBeforeUiFailure_false_noNetwork() {
        ContextTimeline timeline = new ContextTimeline();
        timeline.addEvent(new ContextEvent.UiEvent(2000L, "test", "[data-btn]", "failure"));

        boolean result = timeline.networkBeforeUiFailure(2000L, 5000L);

        assertThat(result).isFalse();
    }

    @Test
    void testNetworkBeforeUiFailure_false_networkAfter() {
        ContextTimeline timeline = new ContextTimeline();
        timeline.addEvent(new ContextEvent.UiEvent(1000L, "test", "[data-btn]", "failure"));
        timeline.addEvent(new ContextEvent.NetworkEvent(
            2000L,
            "test",
            new NetworkEventPayload("POST", "/api", 500, null, NetworkPhase.RESPONSE, null, null, null, null)
        ));

        boolean result = timeline.networkBeforeUiFailure(1500L, 5000L);

        assertThat(result).isFalse();
    }
}
