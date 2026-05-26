package io.github.kotlerdev.frap.core.context;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Phase of a network event.
 * Mirrors TypeScript {@code NetworkPhase} from context.ts.
 */
public enum NetworkPhase {
    REQUEST("request"),
    RESPONSE("response"),
    FAILED("failed"),
    OPEN("open"),
    MESSAGE("message"),
    CLOSE("close");

    private final String value;

    NetworkPhase(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
