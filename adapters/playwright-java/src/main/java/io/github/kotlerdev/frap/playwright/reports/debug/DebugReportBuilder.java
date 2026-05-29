package io.github.kotlerdev.frap.playwright.reports.debug;

import io.github.kotlerdev.frap.core.dto.Candidate;
import io.github.kotlerdev.frap.core.dto.DOMSnapshot;
import io.github.kotlerdev.frap.core.dto.HealResult;
import io.github.kotlerdev.frap.core.semantics.HealingSemantics;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a {@link DebugReportModels.DebugReport} from a heal attempt.
 */
public final class DebugReportBuilder {
    private DebugReportBuilder() {}

    public static DebugReportModels.DebugReport fromHealResult(
        String testName,
        HealResult result,
        DOMSnapshot snapshot,
        HealingSemantics semantics,
        long durationMs
    ) {
        String timestamp = Instant.now().toString();
        int elementCount = snapshot != null ? snapshot.elements().size() : 0;
        List<Candidate> candidates = result.topCandidates();

        List<DebugReportModels.DebugStep> steps = new ArrayList<>();
        steps.add(step("dom_parsed", timestamp, Map.of(
            "element_count", elementCount,
            "total_elements", elementCount
        )));
        steps.add(step("clusters_built", timestamp, Map.of(
            "candidates_found", candidates.size()
        )));

        List<Map<String, Object>> top3 = candidates.stream()
            .limit(3)
            .map(c -> Map.<String, Object>of(
                "selector", c.selector(),
                "confidence", c.confidence()
            ))
            .toList();
        steps.add(step("candidates_ranked", timestamp, Map.of("top_3", top3)));

        Map<String, Object> decisionOutput = new LinkedHashMap<>();
        decisionOutput.put("healed", result.healed());
        decisionOutput.put("confidence", result.confidence());
        decisionOutput.put("selector", result.selector() != null ? result.selector() : "");
        if (!result.healed()) {
            decisionOutput.put("reason", inferFailureReason(result));
        }
        steps.add(step("healing_decision", timestamp, decisionOutput));

        List<DebugReportModels.CandidateSummary> topCandidates = candidates.stream()
            .map(c -> new DebugReportModels.CandidateSummary(c.selector(), c.confidence()))
            .toList();

        DebugReportModels.HealingDebugInfo healing = new DebugReportModels.HealingDebugInfo(
            result.healed(),
            result.selector() != null ? result.selector() : "",
            result.confidence(),
            topCandidates
        );

        return new DebugReportModels.DebugReport(
            timestamp,
            testName,
            (int) durationMs,
            steps,
            DebugClusterBuilder.buildClusterViews(candidates),
            healing,
            semantics
        );
    }

    private static DebugReportModels.DebugStep step(String name, String timestamp, Map<String, Object> output) {
        return new DebugReportModels.DebugStep(name, timestamp, 0, null, output);
    }

    private static String inferFailureReason(HealResult result) {
        List<Candidate> candidates = result.topCandidates();
        if (candidates.isEmpty()) {
            return "no_candidates";
        }
        if (candidates.size() >= 2) {
            double diff = candidates.get(0).confidence() - candidates.get(1).confidence();
            if (diff < 0.1) {
                return "ambiguous";
            }
        }
        return "threshold_not_met";
    }
}
