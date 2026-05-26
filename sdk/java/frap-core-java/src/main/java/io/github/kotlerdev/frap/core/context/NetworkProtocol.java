package io.github.kotlerdev.frap.core.context;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Network protocol for context events.
 * Mirrors TypeScript {@code NetworkProtocol} from context.ts.
 */
public enum NetworkProtocol {
    HTTP("http"),
    WEBSOCKET("websocket");

    private final String value;

    NetworkProtocol(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
