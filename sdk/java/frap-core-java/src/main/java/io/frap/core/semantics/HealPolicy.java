package io.frap.core.semantics;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Test-level expectation for healing.
 * Mirrors TypeScript {@code HealPolicy} from healing-semantics.ts.
 */
public enum HealPolicy {
    ALLOW("allow"),
    DENY("deny"),
    EXPECT_HEAL("expect_heal");

    private final String value;

    HealPolicy(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
