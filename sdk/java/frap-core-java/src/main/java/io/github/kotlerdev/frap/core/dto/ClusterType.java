package io.github.kotlerdev.frap.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ClusterType {
    @JsonProperty("single")
    SINGLE,
    @JsonProperty("list")
    LIST,
    @JsonProperty("unknown")
    UNKNOWN
}
