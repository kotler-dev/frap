package io.frap.core.context;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Log level for context events.
 * Mirrors TypeScript {@code LogLevel} from context.ts.
 */
public enum LogLevel {
    LOG("log"),
    INFO("info"),
    WARN("warn"),
    ERROR("error"),
    DEBUG("debug");

    private final String value;

    LogLevel(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
