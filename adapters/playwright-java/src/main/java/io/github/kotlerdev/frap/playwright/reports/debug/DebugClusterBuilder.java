package io.github.kotlerdev.frap.playwright.reports.debug;

import io.github.kotlerdev.frap.core.dto.Candidate;
import io.github.kotlerdev.frap.core.dto.DOMToken;
import io.github.kotlerdev.frap.core.dto.Signature;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Groups healing candidates by structural prefix for debug HTML (mirrors TS {@code buildClusterViews}).
 */
public final class DebugClusterBuilder {
    private DebugClusterBuilder() {}

    public static List<DebugReportModels.ClusterView> buildClusterViews(List<Candidate> candidates) {
        Map<String, DebugReportModels.ClusterView> clusters = new LinkedHashMap<>();

        for (Candidate candidate : candidates) {
            Signature sig = candidate.signature();
            String prefix = sig.prefix() != null ? sig.prefix() : "";
            DebugReportModels.ClusterView cluster = clusters.get(prefix);
            if (cluster == null) {
                String id = "cluster_" + prefix.replace(">", "_");
                cluster = new DebugReportModels.ClusterView(id, prefix, 0, new ArrayList<>());
                clusters.put(prefix, cluster);
            }

            int count = cluster.elementCount() + 1;
            List<DebugReportModels.ClusterElement> elements = new ArrayList<>(cluster.elements());
            if (elements.size() < 5) {
                elements.add(new DebugReportModels.ClusterElement(
                    candidate.selector(),
                    signaturePreview(sig),
                    truncate(sig.textContent(), 50)
                ));
            }
            clusters.put(prefix, new DebugReportModels.ClusterView(
                cluster.id(), cluster.prefix(), count, elements
            ));
        }

        return new ArrayList<>(clusters.values());
    }

    private static String signaturePreview(Signature sig) {
        String tag = "unknown";
        if (sig.path() != null && !sig.path().isEmpty()) {
            DOMToken first = sig.path().get(0);
            tag = first.tag() != null ? first.tag() : "unknown";
        }
        String text = sig.textContent() != null ? truncate(sig.textContent(), 20) : "";
        return tag + ":" + text;
    }

    private static String truncate(String value, int max) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
