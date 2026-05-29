package io.github.kotlerdev.frap.playwright.reports.debug;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Generates HTML debug reports from JSON artifacts (mirrors TS {@code generateAllDebugHtml}).
 */
public final class DebugHtmlGenerator {
    private static final Logger logger = LoggerFactory.getLogger(DebugHtmlGenerator.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    private DebugHtmlGenerator() {}

    public static void generateAll(Path reportDir) throws IOException {
        Path subDir = DebugReportWriter.debugReportsDir(reportDir);
        List<ReportItem> items = new ArrayList<>();

        if (Files.isDirectory(subDir)) {
            try (Stream<Path> files = Files.list(subDir)) {
                files.filter(p -> p.toString().endsWith(".json"))
                    .filter(p -> !p.getFileName().toString().equals("manifest.json"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .forEach(p -> {
                        try {
                            DebugReportModels.DebugReport report = MAPPER.readValue(p.toFile(), DebugReportModels.DebugReport.class);
                            String htmlName = p.getFileName().toString().replace(".json", ".html");
                            items.add(new ReportItem(report, p.getFileName().toString(), htmlName));
                        } catch (IOException e) {
                            logger.warn("[frap:debug] Skip invalid debug JSON: {}", p);
                        }
                    });
            }
        }

        Path legacyJson = reportDir.resolve("frap-debug.json");
        if (items.isEmpty() && Files.exists(legacyJson)) {
            DebugReportModels.DebugReport report = MAPPER.readValue(legacyJson.toFile(), DebugReportModels.DebugReport.class);
            writeHtml(report, reportDir.resolve("frap-debug.html"));
            return;
        }

        if (items.isEmpty()) {
            return;
        }

        DebugReportModels.DebugManifest manifest = buildManifest(items);
        MAPPER.writeValue(subDir.resolve("manifest.json").toFile(), manifest);

        for (ReportItem item : items) {
            Path htmlPath = subDir.resolve(item.htmlName());
            writeHtml(item.report(), htmlPath);
        }

        if (items.size() == 1) {
            Files.copy(
                subDir.resolve(items.get(0).htmlName()),
                reportDir.resolve("frap-debug.html"),
                StandardCopyOption.REPLACE_EXISTING
            );
            writeExplorerStub(reportDir, manifest);
        } else {
            writeClassicIndex(reportDir, manifest);
            writeExplorerFull(reportDir, manifest);
        }

        logger.info("[frap:debug] Generated HTML for {} debug report(s) in {}", items.size(), reportDir);
    }

    private static void writeHtml(DebugReportModels.DebugReport report, Path htmlPath) throws IOException {
        Files.createDirectories(htmlPath.getParent());
        Files.writeString(htmlPath, DebugHtmlRenderer.render(report));
        logger.info("[frap:debug] HTML report: {}", htmlPath);
    }

    private static DebugReportModels.DebugManifest buildManifest(List<ReportItem> items) {
        String generatedAt = Instant.now().toString();
        List<DebugReportModels.DebugManifestEntry> entries = new ArrayList<>();
        for (ReportItem item : items) {
            String id = item.jsonFileName().replace(".json", "");
            TestNameParts parts = parseTestNameParts(item.report().testName());
            entries.add(new DebugReportModels.DebugManifestEntry(
                id,
                item.report().testName(),
                parts.groupPath(),
                parts.leafName(),
                "debug-reports/" + item.htmlName(),
                "debug-reports/" + item.htmlName() + "?embed=1",
                DebugStatus.overall(item.report()).name().toLowerCase(),
                item.report().healing().healed(),
                item.report().healing().confidence(),
                item.report().timestamp(),
                item.report().semantics() != null
            ));
        }
        return new DebugReportModels.DebugManifest(generatedAt, entries.size(), entries);
    }

    private static void writeClassicIndex(Path reportDir, DebugReportModels.DebugManifest manifest) throws IOException {
        StringBuilder links = new StringBuilder();
        for (DebugReportModels.DebugManifestEntry entry : manifest.entries()) {
            links.append("""
                <p><a class="nav-view-link" href="%s">%s</a> — <span class="tag tag-%s">%s</span></p>
                """.formatted(
                escapeAttr(entry.htmlHref()),
                DebugHtmlRenderer.escape(entry.leafName()),
                entry.status(),
                fmt(entry.confidence())
            ));
        }
        String html = """
            <!DOCTYPE html>
            <html lang="en" data-theme="light">
            <head>
              <meta charset="UTF-8">
              <title>Frap Debug Reports</title>
              <script>%s</script>
              <style>%s</style>
            </head>
            <body>
              %s
              <div class="container">
                <header class="site-header">
                  <div><h1>Frap</h1><p class="subtitle">%d tests with debug enabled</p></div>
                </header>
                <p class="index-hint"><a href="frap-debug-explorer.html">Explorer view (B)</a></p>
                <div class="panel">%s</div>
              </div>
            </body>
            </html>
            """.formatted(
            DebugHtmlResources.THEME_INIT,
            DebugHtmlResources.reportStyles(),
            DebugHtmlResources.ICONS_SVG,
            manifest.reportCount(),
            links
        );
        Files.writeString(reportDir.resolve("frap-debug.html"), html);
    }

    private static void writeExplorerStub(Path reportDir, DebugReportModels.DebugManifest manifest) throws IOException {
        String testLabel = manifest.entries().isEmpty()
            ? "1 test"
            : DebugHtmlRenderer.escape(manifest.entries().get(0).testName());
        String html = """
            <!DOCTYPE html>
            <html lang="en" data-theme="light">
            <head><meta charset="UTF-8"><title>Frap Debug Explorer</title>
            <script>%s</script><style>%s</style></head>
            <body>
              %s
              <div class="container">
                <div class="callout callout-warning">
                  <span class="callout-title">Explorer needs 2+ debug reports</span>
                  <span class="callout-sub">Only one test (%s). Use <a href="frap-debug.html">Classic view (A)</a>.</span>
                </div>
              </div>
            </body></html>
            """.formatted(DebugHtmlResources.THEME_INIT, DebugHtmlResources.reportStyles(),
            DebugHtmlResources.ICONS_SVG, testLabel);
        Files.writeString(reportDir.resolve("frap-debug-explorer.html"), html);
    }

    private static void writeExplorerFull(Path reportDir, DebugReportModels.DebugManifest manifest) throws IOException {
        String manifestJson = MAPPER.writeValueAsString(manifest).replace("<", "\\u003c");
        String firstEmbed = manifest.entries().isEmpty() ? "" : manifest.entries().get(0).embedHref();
        String html = """
            <!DOCTYPE html>
            <html lang="en" data-theme="light">
            <head><meta charset="UTF-8"><title>Frap Debug Explorer</title>
            <script>%s</script><style>%s</style></head>
            <body class="explorer-layout">
              %s
              <p><a href="frap-debug.html">Classic view (A)</a></p>
              <iframe id="report-frame" title="Debug report" src="%s" style="width:100%%;height:80vh;border:1px solid var(--border-subtle)"></iframe>
              <script type="application/json" id="frap-manifest">%s</script>
            </body></html>
            """.formatted(
            DebugHtmlResources.THEME_INIT,
            DebugHtmlResources.reportStyles(),
            DebugHtmlResources.ICONS_SVG,
            escapeAttr(firstEmbed),
            manifestJson
        );
        Files.writeString(reportDir.resolve("frap-debug-explorer.html"), html);
    }

    private record ReportItem(DebugReportModels.DebugReport report, String jsonFileName, String htmlName) {}

    private record TestNameParts(List<String> groupPath, String leafName) {}

    private static TestNameParts parseTestNameParts(String testName) {
        String sep = " > ";
        if (!testName.contains(sep)) {
            return new TestNameParts(List.of("Other"), testName);
        }
        String[] parts = testName.split(sep);
        String leaf = parts[parts.length - 1];
        List<String> groups = new ArrayList<>();
        for (int i = 0; i < parts.length - 1; i++) {
            groups.add(parts[i]);
        }
        if (groups.isEmpty()) {
            groups.add("Other");
        }
        return new TestNameParts(groups, leaf);
    }

    private static String escapeAttr(String value) {
        return DebugHtmlRenderer.escape(value);
    }

    private static String fmt(double v) {
        return String.format("%.2f", v);
    }
}
