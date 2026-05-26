package io.frap.core.semantics;

/**
 * Why healing was attempted.
 * Mirrors TypeScript {@code HealTrigger} from healing-semantics.ts.
 */
public enum HealTrigger {
    SELECTOR_MISSING("selector_missing"),
    ACTION_FAILED("action_failed");

    private final String value;

    HealTrigger(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
