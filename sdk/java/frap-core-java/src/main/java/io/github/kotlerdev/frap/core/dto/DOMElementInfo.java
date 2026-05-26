package io.github.kotlerdev.frap.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Information about a DOM element in a snapshot.
 * Mirrors TypeScript {@code DOMElementInfo} from core-types.ts.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DOMElementInfo(
    @JsonProperty("selector") String selector,
    @JsonProperty("tag") String tag,
    @JsonProperty("attributes") Map<String, String> attributes,
    @JsonProperty("text_content") String textContent,
    @JsonProperty("path") List<String> path
) {
    public DOMElementInfo {
        if (selector == null || selector.isBlank()) {
            throw new IllegalArgumentException("selector is required");
        }
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("tag is required");
        }
        attributes = attributes != null ? Map.copyOf(attributes) : Collections.emptyMap();
        path = path != null ? List.copyOf(path) : Collections.emptyList();
    }
}
