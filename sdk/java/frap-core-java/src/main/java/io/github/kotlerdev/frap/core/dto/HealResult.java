package io.github.kotlerdev.frap.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.kotlerdev.frap.core.semantics.HealingSemantics;

import java.util.Collections;
import java.util.List;

/**
 * Result of a healing attempt.
 * Mirrors TypeScript {@code HealResult} from core-types.ts.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HealResult(
    @JsonProperty("healed") boolean healed,
    @JsonProperty("selector") String selector,
    @JsonProperty("confidence") double confidence,
    @JsonProperty("diff") String diff,
    @JsonProperty("top_candidates") List<Candidate> topCandidates,
    @JsonProperty("original_signature") Signature originalSignature,
    @JsonProperty("semantics") HealingSemantics semantics
) {
    public HealResult {
        if (selector == null) {
            selector = "";
        }
        topCandidates = topCandidates != null ? List.copyOf(topCandidates) : Collections.emptyList();
    }

    /**
     * Creates a result representing a successful find without healing.
     */
    public static HealResult noHeal(String selector, Signature originalSignature) {
        return new HealResult(false, selector, 1.0, null, Collections.emptyList(), originalSignature, null);
    }

    /**
     * Creates a result representing a successful healing.
     */
    public static HealResult healed(String originalSelector, String healedSelector, double confidence, 
                                     String diff, List<Candidate> topCandidates, Signature originalSignature) {
        return new HealResult(true, healedSelector, confidence, diff, topCandidates, originalSignature, null);
    }
}
