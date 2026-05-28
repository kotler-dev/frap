package io.github.kotlerdev.frap.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MapOptions(
    @JsonProperty("url") String url,
    @JsonProperty("include_non_interactive") Boolean includeNonInteractive,
    @JsonProperty("max_elements") Integer maxElements
) {
    public static MapOptions defaults() {
        return new MapOptions(null, false, null);
    }
}
