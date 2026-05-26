package io.frap.core.semantics;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Classified result for reports and CI gates.
 * Mirrors TypeScript {@code HealOutcome} from healing-semantics.ts.
 */
public enum HealOutcome {
    HEALED("healed"),
    REJECTED("rejected"),
    UNEXPECTED_HEAL("unexpected_heal"),
    NO_HEAL("no_heal");

    private final String value;

    HealOutcome(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    /**
     * Classifies the outcome based on policy and healing result.
     * Mirrors TypeScript {@code classifyHealOutcome}.
     */
    public static HealOutcome classify(HealPolicy policy, boolean healed, boolean healingAttempted) {
        if (!healingAttempted) {
            return NO_HEAL;
        }
        if (!healed) {
            return REJECTED;
        }
        if (policy == HealPolicy.DENY) {
            return UNEXPECTED_HEAL;
        }
        return HEALED;
    }
}
