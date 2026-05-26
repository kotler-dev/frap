package io.frap.playwright.reports;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.frap.core.rca.RcaReport;

import java.util.List;

/**
 * Frap report summary structure.
 * Mirrors TypeScript {@code FrapReportSummary} from reporter.ts.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FrapReport(
    @JsonProperty("timestamp") String timestamp,
    @JsonProperty("summary") Summary summary,
    @JsonProperty("events") List<HealingEventRecord> events,
    @JsonProperty("contextTests") List<ContextTestResult> contextTests
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Summary(
        @JsonProperty("totalAttempts") int totalAttempts,
        @JsonProperty("totalHeals") int totalHeals,
        @JsonProperty("expectedHeals") int expectedHeals,
        @JsonProperty("unexpectedHeals") int unexpectedHeals,
        @JsonProperty("rejectedHeals") int rejectedHeals,
        @JsonProperty("averageConfidence") double averageConfidence
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record HealingEventRecord(
        @JsonProperty("playwrightTestId") String playwrightTestId,
        @JsonProperty("originalSelector") String originalSelector,
        @JsonProperty("newSelector") String newSelector,
        @JsonProperty("healed") boolean healed,
        @JsonProperty("confidence") double confidence,
        @JsonProperty("trigger") String trigger,
        @JsonProperty("policy") String policy,
        @JsonProperty("outcome") String outcome,
        @JsonProperty("timestamp") String timestamp
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ContextTestResult(
        @JsonProperty("playwrightTestId") String playwrightTestId,
        @JsonProperty("status") String status,
        @JsonProperty("durationMs") int durationMs,
        @JsonProperty("message") String message,
        @JsonProperty("timestamp") String timestamp,
        @JsonProperty("traceId") String traceId,
        @JsonProperty("rca") RcaReport rca
    ) {}
}
