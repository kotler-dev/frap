package io.github.kotlerdev.frap.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MapMetadata(
    @JsonProperty("url") String url,
    @JsonProperty("element_count") int elementCount,
    @JsonProperty("cluster_count") int clusterCount,
    @JsonProperty("timestamp_ms") long timestampMs
) {}
