package io.github.kotlerdev.frap.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Structural signature of a DOM element for self-healing.
 * Mirrors TypeScript {@code Signature} from core-types.ts.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Signature(
    @JsonProperty("path") List<DOMToken> path,
    @JsonProperty("prefix") String prefix,
    @JsonProperty("stable_attrs") Map<String, String> stableAttrs,
    @JsonProperty("text_content") String textContent,
    @JsonProperty("position_in_parent") Integer positionInParent,
    @JsonProperty("children_hash") long childrenHash,
    @JsonProperty("depth") int depth
) {
    public Signature {
        path = path != null ? List.copyOf(path) : Collections.emptyList();
        stableAttrs = stableAttrs != null ? Map.copyOf(stableAttrs) : Collections.emptyMap();
    }

    public Signature(List<DOMToken> path, String prefix, Map<String, String> stableAttrs, int depth) {
        this(path, prefix, stableAttrs, null, null, 0L, depth);
    }
}
