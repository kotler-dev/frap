package io.github.kotlerdev.frap.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GeneratedArtifact(
    @JsonProperty("files") List<GeneratedFile> files
) {}
