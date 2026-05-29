package io.github.kotlerdev.frap.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ElementNode(
    @JsonProperty("id") String id,
    @JsonProperty("selector") String selector,
    @JsonProperty("recommended_selector") String recommendedSelector,
    @JsonProperty("tag") String tag,
    @JsonProperty("signature") Signature signature,
    @JsonProperty("cluster_id") String clusterId,
    @JsonProperty("confidence") double confidence,
    @JsonProperty("locator") LocatorRecommendation locator
) {}
