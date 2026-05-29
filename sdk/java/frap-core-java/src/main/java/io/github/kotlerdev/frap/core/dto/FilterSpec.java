package io.github.kotlerdev.frap.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FilterSpec(
    @JsonProperty("interactive_only") Boolean interactiveOnly,
    @JsonProperty("min_cluster_size") Integer minClusterSize,
    @JsonProperty("tags") List<String> tags
) {}
