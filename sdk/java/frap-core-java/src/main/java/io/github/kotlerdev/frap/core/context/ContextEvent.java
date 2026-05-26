package io.github.kotlerdev.frap.core.context;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Context event for timeline analysis.
 * Mirrors TypeScript {@code ContextEvent} from context.ts.
 * Uses Jackson polymorphic deserialization based on the "kind" field.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "kind",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ContextEvent.UiEvent.class, name = "ui"),
    @JsonSubTypes.Type(value = ContextEvent.NetworkEvent.class, name = "network"),
    @JsonSubTypes.Type(value = ContextEvent.LogEvent.class, name = "log")
})
public sealed interface ContextEvent {

    @JsonProperty("timestamp_ms")
    long timestampMs();

    @JsonProperty("trace_id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String traceId();

    /**
     * UI event (click, fill, failure, etc.).
     */
    record UiEvent(
        @JsonProperty("timestamp_ms") long timestampMs,
        @JsonProperty("trace_id") @JsonInclude(JsonInclude.Include.NON_NULL) String traceId,
        @JsonProperty("element") String element,
        @JsonProperty("action") String action,
        @JsonProperty("detail") @JsonInclude(JsonInclude.Include.NON_NULL) String detail
    ) implements ContextEvent {
        public UiEvent {
            if (element == null || element.isBlank()) {
                throw new IllegalArgumentException("element is required");
            }
            if (action == null || action.isBlank()) {
                throw new IllegalArgumentException("action is required");
            }
        }

        public UiEvent(long timestampMs, String element, String action) {
            this(timestampMs, null, element, action, null);
        }

        public UiEvent(long timestampMs, String traceId, String element, String action) {
            this(timestampMs, traceId, element, action, null);
        }
    }

    /**
     * Network event (request, response, error).
     */
    record NetworkEvent(
        @JsonProperty("timestamp_ms") long timestampMs,
        @JsonProperty("trace_id") @JsonInclude(JsonInclude.Include.NON_NULL) String traceId,
        @JsonProperty("request") NetworkEventPayload request
    ) implements ContextEvent {
        public NetworkEvent {
            if (request == null) {
                throw new IllegalArgumentException("request is required");
            }
        }
    }

    /**
     * Log event (console, error).
     */
    record LogEvent(
        @JsonProperty("timestamp_ms") long timestampMs,
        @JsonProperty("trace_id") @JsonInclude(JsonInclude.Include.NON_NULL) String traceId,
        @JsonProperty("log") LogEventPayload log
    ) implements ContextEvent {
        public LogEvent {
            if (log == null) {
                throw new IllegalArgumentException("log is required");
            }
        }
    }
}
