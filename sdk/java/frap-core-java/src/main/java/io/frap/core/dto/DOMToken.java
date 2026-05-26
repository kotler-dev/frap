package io.frap.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Token representing a DOM node in a signature path.
 * Mirrors TypeScript {@code DOMToken} from core-types.ts.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DOMToken(
    @JsonProperty("tag") String tag,
    @JsonProperty("role") String role,
    @JsonProperty("semantic_type") String semanticType,
    @JsonProperty("structural_class") String structuralClass,
    @JsonProperty("depth") int depth
) {
    public DOMToken {
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("tag is required");
        }
    }

    public DOMToken(String tag, int depth) {
        this(tag, null, null, null, depth);
    }

    public DOMToken(String tag, String role, int depth) {
        this(tag, role, null, null, depth);
    }
}
