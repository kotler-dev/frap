package io.github.kotlerdev.frap.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum CoverageMode {
    @JsonProperty("actionable")
    ACTIONABLE,
    @JsonProperty("semantic")
    SEMANTIC
}
