package io.frap.core.context;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload for log context events.
 * Mirrors TypeScript {@code LogEventPayload} from context.ts.
 */
public record LogEventPayload(
    @JsonProperty("level") LogLevel level,
    @JsonProperty("message") String message,
    @JsonProperty("source") String source
) {
    public LogEventPayload {
        if (level == null) {
            throw new IllegalArgumentException("level is required");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message is required");
        }
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("source is required");
        }
    }
}
