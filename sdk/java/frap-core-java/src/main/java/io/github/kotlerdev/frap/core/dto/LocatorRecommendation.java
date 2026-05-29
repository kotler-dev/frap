package io.github.kotlerdev.frap.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LocatorRecommendation(
    @JsonProperty("selector") String selector,
    @JsonProperty("strategy") String strategy,
    @JsonProperty("confidence") double confidence
) {}
