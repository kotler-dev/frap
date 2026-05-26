package io.frap.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * Snapshot of the DOM for healing analysis.
 * Mirrors TypeScript {@code DOMSnapshot} from core-types.ts.
 */
public record DOMSnapshot(
    @JsonProperty("html") String html,
    @JsonProperty("elements") List<DOMElementInfo> elements
) {
    public DOMSnapshot {
        if (html == null) {
            html = "";
        }
        elements = elements != null ? List.copyOf(elements) : Collections.emptyList();
    }

    public DOMSnapshot(String html) {
        this(html, Collections.emptyList());
    }
}
