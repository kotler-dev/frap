package io.frap.playwright.reports;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.frap.core.context.ContextEvent;
import io.frap.core.context.ContextTimeline;
import io.frap.core.events.HealingEvent;
import io.frap.core.rca.RcaReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates Frap reports (JSON, JUnit XML, HTML debug).
 * Mirrors TypeScript reporter functionality from reporter.ts.
 */
public class ReportGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ReportGenerator.class);

    private final Path reportDir;
    private final ObjectMapper objectMapper;

    public ReportGenerator(Path reportDir) {
        this.reportDir = reportDir;
        this.objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Clears the healing events file.
     */
    public void clearHealingEvents() throws IOException {
        Path eventsFile = reportDir.resolve("frap-events.jsonl");
        Files.deleteIfExists(eventsFile);
    }

    /**
     * Records a healing event to the JSONL file.
     */
    public void recordHealingEvent(HealingEvent event) throws IOException {
        Files.createDirectories(reportDir);
        Path eventsFile = reportDir.resolve("frap-events.jsonl");

        String json = JsonlReporter.toJson(event);
        Files.writeString(eventsFile, json + "\n",
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND);
    }

    /**
     * Loads all healing events from the JSONL file.
     */
    public List<HealingEvent> loadHealingEvents() throws IOException {
        Path eventsFile = reportDir.resolve("frap-events.jsonl");
        if (!Files.exists(eventsFile)) {
            return List.of();
        }

        return Files.readAllLines(eventsFile).stream()
            .filter(line -> !line.isBlank())
            .map(line -> {
                try {
                    return objectMapper.readValue(line, HealingEvent.class);
                } catch (IOException e) {
                    logger.warn("Failed to parse healing event: {}", line);
                    return null;
                }
            })
            .filter(e -> e != null)
            .toList();
    }

    /**
     * Generates the main frap-report.json file.
     */
    public void generateJsonReport(List<HealingEvent> events, List<FrapReport.ContextTestResult> contextTests) throws IOException {
        Files.createDirectories(reportDir);

        // Calculate summary
        int totalAttempts = events.size();
        int totalHeals = (int) events.stream().filter(HealingEvent::healed).count();
        int expectedHeals = (int) events.stream()
            .filter(e -> e.healed() && "expect_heal".equals(e.policy().value()))
            .count();
        int unexpectedHeals = (int) events.stream()
            .filter(e -> e.healed() && !"expect_heal".equals(e.policy().value()))
            .count();
        int rejectedHeals = (int) events.stream()
            .filter(e -> !e.healed())
            .count();

        double avgConfidence = events.isEmpty() ? 0.0 :
            events.stream().mapToDouble(HealingEvent::confidence).average().orElse(0.0);

        // Convert events to records
        List<FrapReport.HealingEventRecord> eventRecords = events.stream()
            .map(e -> new FrapReport.HealingEventRecord(
                e.testId() != null ? e.testId() : "unknown",
                e.originalSelector(),
                e.newSelector(),
                e.healed(),
                e.confidence(),
                e.trigger() != null ? e.trigger().value() : "selector_missing",
                e.policy() != null ? e.policy().value() : "allow",
                e.policy() != null ? inferOutcome(e) : "no_heal",
                e.timestamp()
            ))
            .toList();

        FrapReport report = new FrapReport(
            DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
            new FrapReport.Summary(
                totalAttempts,
                totalHeals,
                expectedHeals,
                unexpectedHeals,
                rejectedHeals,
                avgConfidence
            ),
            eventRecords,
            contextTests
        );

        Path reportFile = reportDir.resolve("frap-report.json");
        objectMapper.writeValue(reportFile.toFile(), report);

        logger.info("Generated frap-report.json: {} events, {} heals", totalAttempts, totalHeals);
    }

    /**
     * Generates JUnit XML report.
     */
    public void generateJUnitXml(List<FrapReport.ContextTestResult> tests, String suiteName) throws IOException {
        Files.createDirectories(reportDir);

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append(String.format(
            "<testsuite name=\"%s\" tests=\"%d\" errors=\"0\" failures=\"%d\" skipped=\"0\" time=\"0\">\n",
            escapeXml(suiteName),
            tests.size(),
            (int) tests.stream().filter(t -> "failed".equals(t.status())).count()
        ));

        for (FrapReport.ContextTestResult test : tests) {
            xml.append(String.format(
                "  <testcase name=\"%s\" classname=\"%s\" time=\"%s\">\n",
                escapeXml(test.playwrightTestId()),
                escapeXml(suiteName),
                test.durationMs() / 1000.0
            ));

            if (test.rca() != null) {
                xml.append(String.format(
                    "    <properties>\n" +
                    "      <property name=\"Frap.healed\" value=\"true\"/>\n" +
                    "      <property name=\"Frap.confidence\" value=\"%.2f\"/>\n" +
                    "      <property name=\"Frap.primary_cause\" value=\"%s\"/>\n" +
                    "    </properties>\n",
                    test.rca().confidence(),
                    test.rca().primaryCause().value()
                ));
            }

            if ("failed".equals(test.status()) && test.message() != null) {
                xml.append(String.format(
                    "    <failure message=\"%s\" type=\"AssertionError\">\n",
                    escapeXml(test.message())
                ));
                xml.append(escapeXml(test.message()));
                xml.append("\n    </failure>\n");
            }

            xml.append("  </testcase>\n");
        }

        xml.append("</testsuite>\n");

        Path junitFile = reportDir.resolve("junit.xml");
        Files.writeString(junitFile, xml.toString());
    }

    /**
     * Generates context report from timeline events.
     */
    public void generateContextReport(ContextTimeline timeline) throws IOException {
        Files.createDirectories(reportDir);

        Path contextFile = reportDir.resolve("frap-context.json");
        objectMapper.writeValue(contextFile.toFile(), timeline);

        logger.info("Generated frap-context.json: {} events", timeline.events().size());
    }

    /**
     * Generates RCA report.
     */
    public void generateRcaReport(RcaReport report) throws IOException {
        Files.createDirectories(reportDir);

        Path rcaFile = reportDir.resolve("frap-rca.json");
        objectMapper.writeValue(rcaFile.toFile(), report);

        logger.info("Generated frap-rca.json: {} cause, confidence={}",
            report.primaryCause(), report.confidence());
    }

    /**
     * Clears all debug reports.
     */
    public void clearDebugReports() throws IOException {
        Path debugDir = reportDir.resolve("debug-reports");
        if (Files.exists(debugDir)) {
            Files.walk(debugDir)
                .filter(Files::isRegularFile)
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        logger.warn("Failed to delete debug report: {}", p);
                    }
                });
        }
    }

    /**
     * Writes a debug report for a specific test.
     */
    public void writeDebugReport(String testName, DebugReport report) throws IOException {
        Path debugDir = reportDir.resolve("debug-reports");
        Files.createDirectories(debugDir);

        String slug = testName.replaceAll("[^a-zA-Z0-9-]", "_").replaceAll("_+", "_");
        Path reportFile = debugDir.resolve("debug-" + slug + ".json");

        objectMapper.writeValue(reportFile.toFile(), report);
    }

    private String inferOutcome(HealingEvent event) {
        if (!event.healed()) return "rejected";
        if (event.policy() != null && "deny".equals(event.policy().value())) return "unexpected_heal";
        if (event.policy() != null && "expect_heal".equals(event.policy().value())) return "healed";
        return "healed";
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }

    /**
     * Debug report structure.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DebugReport(
        @JsonProperty("testName") String testName,
        @JsonProperty("timestamp") String timestamp,
        @JsonProperty("duration_ms") int durationMs,
        @JsonProperty("elements_scanned") int elementsScanned,
        @JsonProperty("steps") List<DebugStep> steps
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DebugStep(
        @JsonProperty("step") String step,
        @JsonProperty("status") String status,
        @JsonProperty("details") Object details
    ) {}
}
