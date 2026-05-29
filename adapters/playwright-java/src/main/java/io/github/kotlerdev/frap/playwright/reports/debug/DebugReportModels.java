package io.github.kotlerdev.frap.playwright.reports.debug;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.kotlerdev.frap.core.semantics.HealingSemantics;

import java.util.List;

/**
 * Debug report structures aligned with TypeScript {@code @frap/frap} debug types.
 */
public final class DebugReportModels {
    private DebugReportModels() {}

    public static final List<String> DEBUG_STEP_NAMES = List.of(
        "dom_parsed", "clusters_built", "candidates_ranked", "healing_decision"
    );

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DebugStep(
        @JsonProperty("name") String name,
        @JsonProperty("timestamp") String timestamp,
        @JsonProperty("duration_ms") int durationMs,
        @JsonProperty("input") Object input,
        @JsonProperty("output") Object output
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ClusterElement(
        @JsonProperty("selector") String selector,
        @JsonProperty("signature_preview") String signaturePreview,
        @JsonProperty("text_content") String textContent
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ClusterView(
        @JsonProperty("id") String id,
        @JsonProperty("prefix") String prefix,
        @JsonProperty("element_count") int elementCount,
        @JsonProperty("elements") List<ClusterElement> elements
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CandidateSummary(
        @JsonProperty("selector") String selector,
        @JsonProperty("confidence") double confidence
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record HealingDebugInfo(
        @JsonProperty("healed") boolean healed,
        @JsonProperty("selector") String selector,
        @JsonProperty("confidence") double confidence,
        @JsonProperty("top_candidates") List<CandidateSummary> topCandidates
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DebugReport(
        @JsonProperty("timestamp") String timestamp,
        @JsonProperty("testName") String testName,
        @JsonProperty("duration_ms") int durationMs,
        @JsonProperty("steps") List<DebugStep> steps,
        @JsonProperty("clusters") List<ClusterView> clusters,
        @JsonProperty("healing") HealingDebugInfo healing,
        @JsonProperty("semantics") HealingSemantics semantics
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DebugManifestEntry(
        @JsonProperty("id") String id,
        @JsonProperty("testName") String testName,
        @JsonProperty("groupPath") List<String> groupPath,
        @JsonProperty("leafName") String leafName,
        @JsonProperty("htmlHref") String htmlHref,
        @JsonProperty("embedHref") String embedHref,
        @JsonProperty("status") String status,
        @JsonProperty("healed") boolean healed,
        @JsonProperty("confidence") double confidence,
        @JsonProperty("timestamp") String timestamp,
        @JsonProperty("hasSemantics") boolean hasSemantics
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DebugManifest(
        @JsonProperty("generatedAt") String generatedAt,
        @JsonProperty("reportCount") int reportCount,
        @JsonProperty("entries") List<DebugManifestEntry> entries
    ) {}
}
