package io.github.kotlerdev.frap.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ElementNode(
    @JsonProperty("id") String id,
    @JsonProperty("selector") String selector,
    @JsonProperty("recommended_selector") String recommendedSelector,
    @JsonProperty("tag") String tag,
    @JsonProperty("signature") Signature signature,
    @JsonProperty("cluster_id") String clusterId,
    @JsonProperty("confidence") double confidence,
    @JsonProperty("locator") LocatorRecommendation locator,
    @JsonProperty("fragile") boolean fragile,
    @JsonProperty("alternatives") List<LocatorRecommendation> alternatives,
    @JsonProperty("accessible_name") String accessibleName
) {
    public ElementNode(
        String id,
        String selector,
        String recommendedSelector,
        String tag,
        Signature signature,
        String clusterId,
        double confidence,
        LocatorRecommendation locator
    ) {
        this(id, selector, recommendedSelector, tag, signature, clusterId, confidence, locator, false, List.of(), null);
    }
}
