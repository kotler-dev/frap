package io.frap.core.context;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Direction of message flow.
 * Mirrors TypeScript {@code MessageDirection} from context.ts.
 */
public enum MessageDirection {
    SENT("sent"),
    RECEIVED("received");

    private final String value;

    MessageDirection(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
