package io.frap.core.rca;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Primary cause classification for RCA.
 * Mirrors TypeScript {@code PrimaryCause} from rca.ts.
 */
public enum PrimaryCause {
    UI_CHANGE("ui_change"),
    API_ERROR("api_error"),
    INFRASTRUCTURE("infrastructure"),
    FLAKY("flaky"),
    UNKNOWN("unknown");

    private final String value;

    PrimaryCause(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
