package io.github.kotlerdev.frap.core.semantics;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Semantic classification of a healing event.
 * Mirrors TypeScript {@code HealingSemantics} from healing-semantics.ts.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HealingSemantics(
    @JsonProperty("trigger") HealTrigger trigger,
    @JsonProperty("policy") HealPolicy policy,
    @JsonProperty("outcome") HealOutcome outcome
) {
    public HealingSemantics {
        if (trigger == null) {
            throw new IllegalArgumentException("trigger is required");
        }
        if (policy == null) {
            throw new IllegalArgumentException("policy is required");
        }
        if (outcome == null) {
            throw new IllegalArgumentException("outcome is required");
        }
    }

    /**
     * Creates semantics by classifying the outcome.
     */
    public static HealingSemantics classify(HealTrigger trigger, HealPolicy policy, boolean healed, boolean healingAttempted) {
        HealOutcome outcome = HealOutcome.classify(policy, healed, healingAttempted);
        return new HealingSemantics(trigger, policy, outcome);
    }
}
