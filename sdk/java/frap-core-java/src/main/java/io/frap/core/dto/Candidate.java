package io.frap.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A candidate element found during healing with its confidence score.
 * Mirrors TypeScript {@code Candidate} from core-types.ts.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Candidate(
    @JsonProperty("selector") String selector,
    @JsonProperty("signature") Signature signature,
    @JsonProperty("confidence") double confidence
) {
    public Candidate {
        if (selector == null || selector.isBlank()) {
            throw new IllegalArgumentException("selector is required");
        }
        if (signature == null) {
            throw new IllegalArgumentException("signature is required");
        }
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be between 0.0 and 1.0");
        }
    }
}
