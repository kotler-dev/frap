package io.github.kotlerdev.frap.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ElementMap(
    @JsonProperty("elements") List<ElementNode> elements,
    @JsonProperty("clusters") List<Cluster> clusters,
    @JsonProperty("metadata") MapMetadata metadata
) {}
