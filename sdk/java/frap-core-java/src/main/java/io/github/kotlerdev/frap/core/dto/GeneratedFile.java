package io.github.kotlerdev.frap.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GeneratedFile(
    @JsonProperty("path") String path,
    @JsonProperty("content") String content
) {}
