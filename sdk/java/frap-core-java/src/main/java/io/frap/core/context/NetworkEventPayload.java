package io.frap.core.context;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload for network context events.
 * Mirrors TypeScript {@code NetworkEventPayload} from context.ts.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NetworkEventPayload(
    @JsonProperty("method") String method,
    @JsonProperty("url") String url,
    @JsonProperty("status") Integer status,
    @JsonProperty("duration_ms") Long durationMs,
    @JsonProperty("phase") NetworkPhase phase,
    @JsonProperty("protocol") NetworkProtocol protocol,
    @JsonProperty("direction") MessageDirection direction,
    @JsonProperty("payload_preview") String payloadPreview,
    @JsonProperty("error_text") String errorText
) {
    public NetworkEventPayload {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("method is required");
        }
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("url is required");
        }
        if (phase == null) {
            throw new IllegalArgumentException("phase is required");
        }
    }

    public NetworkEventPayload(String method, String url, NetworkPhase phase) {
        this(method, url, null, null, phase, null, null, null, null);
    }
}
