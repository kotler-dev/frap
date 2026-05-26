package io.frap.playwright;

import io.frap.playwright.config.WithFrapOptions;
import io.frap.playwright.wrapper.Frap;
import io.frap.playwright.wrapper.SnapshotBuilder;
import org.junit.jupiter.api.*;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Frap Playwright integration.
 * Note: These tests require Playwright browsers to be installed:
 * {@code mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install"}
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FrapTest {

    private static final Path REPORT_DIR = Paths.get("build/test-frap-reports");

    @BeforeAll
    static void beforeAll() {
        // Ensure frap-core-rpc binary is available
        String binaryPath = System.getenv("FRAP_CORE_BIN");
        if (binaryPath == null) {
            // Try default locations
            String[] candidates = {
                "../../../crates/target/release/frap-core-rpc",
                "../../../crates/target/debug/frap-core-rpc",
            };
            for (String candidate : candidates) {
                if (java.io.File(candidate).exists()) {
                    System.setProperty("FRAP_CORE_BIN", candidate);
                    break;
                }
            }
        }
    }

    @AfterAll
    static void afterAll() {
        // Cleanup
        Frap.clearClient();
    }

    @Test
    @Order(1)
    @DisplayName("Extract selector from locator string")
    void testExtractSelector() {
        // Simulate locator.toString() output
        String locatorStr = "locator('[data-testid=\"pay-btn\"]')";
        String selector = extractTestSelector(locatorStr);
        assertThat(selector).isEqualTo("[data-testid=\"pay-btn\"]");
    }

    @Test
    @Order(2)
    @DisplayName("Build options with defaults")
    void testWithFrapOptionsDefaults() {
        var options = new WithFrapOptions()
            .reportDir(REPORT_DIR)
            .minConfidence(0.90);

        var config = options.toFrapConfig();

        assertThat(config.minConfidence()).isEqualTo(0.90);
        assertThat(config.reportDir()).isEqualTo(REPORT_DIR.toString());
        assertThat(config.enableHealing()).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("Options chain builder")
    void testOptionsChaining() {
        var options = new WithFrapOptions()
            .minConfidence(0.95)
            .reportDir(REPORT_DIR)
            .enableHealing(true)
            .enableReporting(true)
            .debug(true)
            .healPolicy("allow")
            .captureAll(true);

        var config = options.toFrapConfig();

        assertThat(config.minConfidence()).isEqualTo(0.95);
        assertThat(config.isDebugEnabled()).isTrue();
        assertThat(config.captureAll()).isTrue();
    }

    // Helper to test selector extraction without real Locator
    private String extractTestSelector(String locatorStr) {
        int start = locatorStr.indexOf("'");
        int end = locatorStr.lastIndexOf("'");
        if (start > 0 && end > start) {
            return locatorStr.substring(start + 1, end);
        }
        return locatorStr;
    }
}
