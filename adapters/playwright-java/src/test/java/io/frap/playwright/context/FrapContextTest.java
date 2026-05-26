package io.frap.playwright.context;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for FrapContext.
 */
class FrapContextTest {

    @Test
    void testContextCaptureOptionsRequired() {
        Path reportDir = Paths.get("./frap-reports");

        var options = new FrapContext.ContextCaptureOptions(reportDir);

        assertThat(options.reportDir()).isEqualTo(reportDir);
        assertThat(options.testId()).isNull();
        assertThat(options.traceId()).isNull();
    }

    @Test
    void testContextCaptureOptionsFull() {
        Path reportDir = Paths.get("./frap-reports");

        var options = new FrapContext.ContextCaptureOptions(
            reportDir,
            "test-123",
            "trace-456"
        );

        assertThat(options.reportDir()).isEqualTo(reportDir);
        assertThat(options.testId()).isEqualTo("test-123");
        assertThat(options.traceId()).isEqualTo("trace-456");
    }

    @Test
    void testContextCaptureOptionsNullReportDirThrows() {
        assertThatThrownBy(() -> new FrapContext.ContextCaptureOptions(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("reportDir");
    }
}
