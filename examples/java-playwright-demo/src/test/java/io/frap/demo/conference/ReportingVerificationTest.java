package io.frap.demo.conference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import io.frap.demo.helpers.ConferencePaths;
import io.frap.playwright.extension.FrapExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * CP005 equivalent: Report artifact verification.
 * Mirrors TypeScript zzz-reporting.spec.ts.
 */
@ExtendWith(FrapExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Conference 2026 Spring - Reporting")
class ReportingVerificationTest {

    static final ObjectMapper objectMapper = new ObjectMapper();
    static Path reportDir;

    @BeforeAll
    static void beforeAll() {
        reportDir = ConferencePaths.CONF_REPORT_DIR;
    }

    @Test
    @Order(100)  // Run last
    @DisplayName("CONF-RPT-RUN-PASS: frap report artifacts exist after suite")
    void testReportArtifactsExist() throws Exception {
        // Verify frap-events.jsonl exists
        Path eventsFile = reportDir.resolve("frap-events.jsonl");
        assertThat(eventsFile)
            .exists()
            .isRegularFile();
        assertThat(eventsFile.toFile().length()).isGreaterThan(0);

        // Verify frap-report.json exists if tests ran with healing
        Path reportFile = reportDir.resolve("frap-report.json");
        if (Files.exists(reportFile)) {
            JsonNode report = objectMapper.readTree(reportFile.toFile());
            assertThat(report.has("summary")).isTrue();
            assertThat(report.has("events")).isTrue();
        }

        // Verify JUnit XML exists
        Path junitFile = reportDir.resolve("junit.xml");
        assertThat(junitFile).exists();
    }

    @Test
    @Order(101)
    @DisplayName("CONF-RPT-JSON: frap-report.json has valid structure")
    void testReportJsonStructure() throws Exception {
        Path reportFile = reportDir.resolve("frap-report.json");
        if (!Files.exists(reportFile)) {
            // Skip if no healing occurred
            return;
        }

        JsonNode report = objectMapper.readTree(reportFile.toFile());

        // Verify timestamp
        assertThat(report.has("timestamp")).isTrue();

        // Verify summary structure
        JsonNode summary = report.get("summary");
        assertThat(summary).isNotNull();
        assertThat(summary.has("totalAttempts")).isTrue();
        assertThat(summary.has("totalHeals")).isTrue();
        assertThat(summary.has("averageConfidence")).isTrue();

        // Verify events is an array
        JsonNode events = report.get("events");
        assertThat(events).isNotNull();
        assertThat(events.isArray()).isTrue();

        // Each event should have required fields
        for (JsonNode event : events) {
            assertThat(event.has("playwrightTestId")).isTrue();
            assertThat(event.has("originalSelector")).isTrue();
            assertThat(event.has("healed")).isTrue();
            assertThat(event.has("confidence")).isTrue();
        }
    }

    @Test
    @Order(102)
    @DisplayName("CONF-RPT-CONTEXT: context timeline exists if captureAll enabled")
    void testContextTimeline() throws Exception {
        Path contextFile = reportDir.resolve("frap-context.json");

        // Only verify if context capture was enabled
        if (Files.exists(contextFile)) {
            JsonNode context = objectMapper.readTree(contextFile.toFile());
            assertThat(context.has("events")).isTrue();

            JsonNode events = context.get("events");
            assertThat(events.isArray()).isTrue();

            for (JsonNode event : events) {
                assertThat(event.has("kind")).isTrue();
                assertThat(event.has("timestamp_ms")).isTrue();
            }
        }
    }

    @Test
    @Order(103)
    @DisplayName("CONF-RPT-JUNIT: JUnit XML is valid")
    void testJUnitXml() throws Exception {
        Path junitFile = reportDir.resolve("junit.xml");

        assertThat(junitFile).exists();

        String xml = Files.readString(junitFile);

        assertThat(xml).contains("<?xml version=");
        assertThat(xml).contains("<testsuite");
        assertThat(xml).contains("</testsuite>");

        // Verify test cases exist
        assertThat(xml).contains("<testcase");
    }

    @Test
    @Order(104)
    @DisplayName("CONF-RPT-RCA: RCA report generated for failures")
    void testRcaReport() throws Exception {
        Path rcaFile = reportDir.resolve("frap-rca.json");

        // RCA report may or may not exist depending on test results
        if (Files.exists(rcaFile)) {
            JsonNode rca = objectMapper.readTree(rcaFile.toFile());
            assertThat(rca.has("version")).isTrue();
            assertThat(rca.has("primary_cause")).isTrue();
            assertThat(rca.has("confidence")).isTrue();
            assertThat(rca.has("recommendation")).isTrue();
        }
    }

    @Test
    @Order(105)
    @DisplayName("CONF-RPT-DEBUG: debug reports generated when debug enabled")
    void testDebugReports() throws Exception {
        Path debugDir = reportDir.resolve("debug-reports");

        // Debug reports may not exist if no healing occurred
        if (!Files.exists(debugDir)) {
            return;
        }

        List<Path> debugFiles = Files.list(debugDir)
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".json"))
            .toList();

        if (!debugFiles.isEmpty()) {
            // Verify first debug report structure
            Path firstDebug = debugFiles.get(0);
            JsonNode debug = objectMapper.readTree(firstDebug.toFile());
            assertThat(debug.has("testName")).isTrue();
            assertThat(debug.has("timestamp")).isTrue();
        }
    }
}
