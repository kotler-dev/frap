package io.frap.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request to heal a selector using structural matching.
 * Mirrors TypeScript {@code HealRequest} from Rust core.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HealRequest(
    @JsonProperty("primary_selector") String primarySelector,
    @JsonProperty("original_signature") Signature originalSignature,
    @JsonProperty("dom_snapshot") DOMSnapshot domSnapshot,
    @JsonProperty("min_confidence") Double minConfidence
) {
    public HealRequest {
        if (primarySelector == null || primarySelector.isBlank()) {
            throw new IllegalArgumentException("primarySelector is required");
        }
        if (domSnapshot == null) {
            throw new IllegalArgumentException("domSnapshot is required");
        }
    }

    /**
     * Creates a heal request with the default confidence threshold.
     */
    public HealRequest(String primarySelector, Signature originalSignature, DOMSnapshot domSnapshot) {
        this(primarySelector, originalSignature, domSnapshot, null);
    }
}
