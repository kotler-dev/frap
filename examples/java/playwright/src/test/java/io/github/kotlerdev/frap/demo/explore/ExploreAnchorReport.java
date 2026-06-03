package io.github.kotlerdev.frap.demo.explore;

import io.github.kotlerdev.frap.core.dto.ElementMap;
import io.github.kotlerdev.frap.core.dto.ElementNode;
import io.github.kotlerdev.frap.core.dto.LocatorRecommendation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Matches anchor keys against discover output for manual acceptance (C013).
 */
final class ExploreAnchorReport {

    private ExploreAnchorReport() {}

    static List<AnchorMatch> findMatches(ElementMap map, List<String> anchorKeys) {
        List<AnchorMatch> results = new ArrayList<>();
        for (String key : anchorKeys) {
            results.add(findBestMatch(map, key));
        }
        return results;
    }

    static void writeSummary(Path outputDir, String phase, List<AnchorMatch> matches) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(phase).append("\n\n");
        sb.append("| key | found | recommended_selector | strategy | fragile | alternatives |\n");
        sb.append("|-----|-------|----------------------|----------|---------|-------------|\n");
        for (AnchorMatch m : matches) {
            sb.append("| ").append(escape(m.key()));
            sb.append(" | ").append(m.found() ? "yes" : "**no**");
            sb.append(" | ").append(escape(m.recommendedSelector()));
            sb.append(" | ").append(escape(m.strategy()));
            sb.append(" | ").append(m.fragile());
            sb.append(" | ").append(escape(m.alternativesSummary()));
            sb.append(" |\n");
        }
        sb.append("\n");
        Path summary = outputDir.resolve("explore-summary.md");
        if (Files.exists(summary)) {
            Files.writeString(summary, sb.toString(), StandardOpenOption.APPEND);
        } else {
            Files.writeString(
                summary,
                "# Sber person_giga explore summary\n\n"
                    + "- URL phase artifacts in this directory\n\n"
                    + sb
            );
        }
        printToConsole(phase, matches);
    }

    private static void printToConsole(String phase, List<AnchorMatch> matches) {
        System.out.println("[explore] Anchor report: " + phase);
        for (AnchorMatch m : matches) {
            System.out.printf(
                "  %s → found=%s selector=%s strategy=%s fragile=%s%n",
                m.key(),
                m.found(),
                m.recommendedSelector(),
                m.strategy(),
                m.fragile()
            );
        }
    }

    private static AnchorMatch findBestMatch(ElementMap map, String key) {
        return map.elements().stream()
            .filter(el -> matchScore(el, key) > 0)
            .max(Comparator.comparingInt(el -> matchScore(el, key)))
            .map(el -> toMatch(key, el))
            .orElseGet(() -> new AnchorMatch(key, false, "-", "-", false, "-"));
    }

    /**
     * Prefer exact accessible name and top-level click roots (#id, role+name) over
     * mega-containers matched only via aggregated text_content.
     */
    static int matchScore(ElementNode el, String key) {
        int score = 0;
        if (key.equals(el.accessibleName())) {
            // Exact accessible name: prefer top-level click root (id) over inner role=button.
            score += 1000;
            if (el.locator() != null) {
                score += strategyScore(el.locator().strategy()) * 10;
            }
            if (el.signature() != null) {
                score -= el.signature().depth();
            }
        }
        String rec = el.recommendedSelector();
        if (rec != null) {
            if (rec.contains("name=\"" + key + "\"") || rec.contains("name=\\\"" + key)) {
                score += 90;
            } else if (rec.contains(key)) {
                score += 40;
            }
        }
        if (el.locator() != null && score < 1000) {
            score += strategyScore(el.locator().strategy());
        }
        if (el.alternatives() != null) {
            for (LocatorRecommendation alt : el.alternatives()) {
                if (alt.selector() != null && alt.selector().contains(key)) {
                    score += 5;
                }
            }
        }
        if (el.signature() != null && el.signature().textContent() != null
            && el.signature().textContent().contains(key)) {
            // Weak: parent widgets often aggregate many tile labels in one text blob.
            score += 10;
        }
        return score;
    }

    private static int strategyScore(String strategy) {
        if (strategy == null) {
            return 0;
        }
        return switch (strategy) {
            case "data-testid" -> 30;
            case "id" -> 28;
            case "data-id" -> 25;
            case "role" -> 22;
            case "aria-label" -> 18;
            case "text" -> 12;
            default -> 0;
        };
    }

    private static AnchorMatch toMatch(String key, ElementNode el) {
        String alts = el.alternatives() == null || el.alternatives().isEmpty()
            ? "-"
            : el.alternatives().stream()
                .map(a -> a.strategy() + ":" + a.selector())
                .collect(Collectors.joining("; "));
        return new AnchorMatch(
            key,
            true,
            el.recommendedSelector(),
            el.locator().strategy(),
            el.fragile(),
            alts
        );
    }

    private static String escape(String s) {
        if (s == null) {
            return "-";
        }
        return s.replace("|", "\\|").replace("\n", " ");
    }

    record AnchorMatch(
        String key,
        boolean found,
        String recommendedSelector,
        String strategy,
        boolean fragile,
        String alternativesSummary
    ) {}
}
