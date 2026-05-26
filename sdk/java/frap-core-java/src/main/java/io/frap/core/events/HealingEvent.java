package io.frap.core.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.frap.core.semantics.HealPolicy;
import io.frap.core.semantics.HealTrigger;

/**
 * Event recorded when healing is attempted.
 * Mirrors healing events from healing-events.ts.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HealingEvent(
    @JsonProperty("timestamp") String timestamp,
    @JsonProperty("test_name") String testName,
    @JsonProperty("test_id") String testId,
    @JsonProperty("original_selector") String originalSelector,
    @JsonProperty("new_selector") String newSelector,
    @JsonProperty("healed") boolean healed,
    @JsonProperty("confidence") double confidence,
    @JsonProperty("trigger") HealTrigger trigger,
    @JsonProperty("policy") HealPolicy policy
) {
    public HealingEvent {
        if (timestamp == null || timestamp.isBlank()) {
            throw new IllegalArgumentException("timestamp is required");
        }
        if (originalSelector == null || originalSelector.isBlank()) {
            throw new IllegalArgumentException("originalSelector is required");
        }
    }
}
