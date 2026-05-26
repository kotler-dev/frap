package io.frap.core.rca;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.frap.core.context.ContextEvent;

import java.util.Collections;
import java.util.List;

/**
 * Root Cause Analysis report.
 * Mirrors TypeScript {@code RcaReport} from rca.ts.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RcaReport(
    @JsonProperty("version") int version,
    @JsonProperty("primary_cause") PrimaryCause primaryCause,
    @JsonProperty("confidence") double confidence,
    @JsonProperty("timeline_excerpt") List<ContextEvent> timelineExcerpt,
    @JsonProperty("recommendation") String recommendation,
    @JsonProperty("details") CauseDetails details,
    @JsonProperty("failure_at_ms") Long failureAtMs
) {
    public RcaReport {
        timelineExcerpt = timelineExcerpt != null ? List.copyOf(timelineExcerpt) : Collections.emptyList();
    }

    public RcaReport(PrimaryCause primaryCause, double confidence, String recommendation, CauseDetails details) {
        this(1, primaryCause, confidence, Collections.emptyList(), recommendation, details, null);
    }
}
