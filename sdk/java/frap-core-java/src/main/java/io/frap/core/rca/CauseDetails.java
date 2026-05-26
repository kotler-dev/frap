package io.frap.core.rca;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Detailed information about the root cause.
 * Mirrors TypeScript {@code CauseDetails} from rca.ts.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CauseDetails(
    @JsonProperty("endpoint") String endpoint,
    @JsonProperty("status") Integer status,
    @JsonProperty("element") String element,
    @JsonProperty("pattern") String pattern,
    @JsonProperty("correlation") Double correlation,
    @JsonProperty("component") String component
) {
}
