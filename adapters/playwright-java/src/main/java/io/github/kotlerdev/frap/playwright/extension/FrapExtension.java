package io.github.kotlerdev.frap.playwright.extension;

import io.github.kotlerdev.frap.core.config.FrapConfig;
import io.github.kotlerdev.frap.playwright.config.FrapPlaywrightConfig;
import io.github.kotlerdev.frap.playwright.context.FrapContext;
import io.github.kotlerdev.frap.playwright.reports.FrapReport;
import io.github.kotlerdev.frap.playwright.reports.ReportGenerator;
import org.junit.jupiter.api.extension.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * JUnit 5 Extension for Frap Playwright integration.
 * Provides automatic context capture and report generation.
 *
 * <p>Usage:</p>
 * <pre>{@code
 * @ExtendWith(FrapExtension.class)
 * class MyTest {
 *     @Test
 *     void testWithFrap() {
 *         // Context capture is automatic
 *         var button = Frap.withFrap(page.locator("[data-testid='btn']"), page);
 *         button.click();
 *     }
 * }
 * }</pre>
 */
public class FrapExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback {
    private static final Logger logger = LoggerFactory.getLogger(FrapExtension.class);
    private static final String STORE_NS = "io.github.kotlerdev.frap";
    private static final String KEY_CONFIG = "config";
    private static final String KEY_TRACE_ID = "traceId";

    @Override
    public void beforeAll(ExtensionContext context) {
        FrapPlaywrightConfig config = loadConfig(context);
        getStore(context).put(KEY_CONFIG, config);
        logger.info("[frap] FrapExtension initialized: reportDir={}", config.frap().reportDir());
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        FrapPlaywrightConfig config = getStore(context).get(KEY_CONFIG, FrapPlaywrightConfig.class);
        if (config == null) return;

        // Try to get Page from test instance
        Object testInstance = context.getTestInstance().orElse(null);
        if (testInstance != null) {
            com.microsoft.playwright.Page page = findPageField(testInstance);
            if (page != null && config.frap().captureAll()) {
                String traceId = FrapContext.attach(page, new FrapContext.ContextCaptureOptions(
                    Paths.get(config.frap().reportDir()),
                    context.getUniqueId(),
                    null
                ));
                getStore(context).put(KEY_TRACE_ID, traceId);
                logger.debug("[frap] Context capture attached: traceId={}", traceId);
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        // Context events are written automatically during test execution
        // Additional per-test cleanup if needed
        String traceId = getStore(context).get(KEY_TRACE_ID, String.class);
        if (traceId != null) {
            logger.debug("[frap] Test completed: traceId={}", traceId);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        FrapPlaywrightConfig config = getStore(context).get(KEY_CONFIG, FrapPlaywrightConfig.class);
        if (config == null) return;

        if (config.frap().enableReporting()) {
            generateReports(context, config);
        }
    }

    private FrapPlaywrightConfig loadConfig(ExtensionContext context) {
        // Try to load from system properties or environment
        String reportDir = System.getProperty("frap.reportDir",
            System.getenv().getOrDefault("FRAP_REPORT_DIR", "./frap-reports"));
        String minConfidence = System.getProperty("frap.minConfidence",
            System.getenv().getOrDefault("FRAP_MIN_CONFIDENCE", "0.85"));

        FrapConfig frapConfig = FrapConfig.defaults();

        try {
            double confidence = Double.parseDouble(minConfidence);
            frapConfig = new FrapConfig(
                confidence,
                reportDir,
                frapConfig.enableHealing(),
                frapConfig.enableReporting(),
                frapConfig.debug(),
                frapConfig.healPolicy(),
                frapConfig.captureAll()
            );
        } catch (NumberFormatException e) {
            logger.warn("[frap] Invalid FRAP_MIN_CONFIDENCE: {}", minConfidence);
        }

        return new FrapPlaywrightConfig(frapConfig, null);
    }

    private void generateReports(ExtensionContext context, FrapPlaywrightConfig config) {
        Path reportDir = Paths.get(config.frap().reportDir());
        logger.info("[frap] Generating reports in: {}", reportDir);

        try {
            var generator = new ReportGenerator(reportDir);

            // Load healing events and generate report
            var events = generator.loadHealingEvents();
            var contextTests = loadContextTests(reportDir);
            generator.generateJsonReport(events, contextTests);
            generator.generateJUnitXml(contextTests, events, "frap-conference");

            logger.info("[frap] Reports generated: {} events, {} tests", events.size(), contextTests.size());
        } catch (Exception e) {
            logger.warn("[frap] Failed to generate reports: {}", e.getMessage());
        }
    }

    private List<FrapReport.ContextTestResult> loadContextTests(Path reportDir) {
        List<FrapReport.ContextTestResult> results = new ArrayList<>();

        try {
            Path eventsFile = reportDir.resolve("frap-context-events.jsonl");
            if (!Files.exists(eventsFile)) {
                return results;
            }

            // Parse context events and build test results
            // Implementation would aggregate events by test
        } catch (Exception e) {
            logger.debug("[frap] No context tests to load: {}", e.getMessage());
        }

        return results;
    }

    private com.microsoft.playwright.Page findPageField(Object instance) {
        for (Field field : instance.getClass().getDeclaredFields()) {
            if (com.microsoft.playwright.Page.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                try {
                    return (com.microsoft.playwright.Page) field.get(instance);
                } catch (IllegalAccessException e) {
                    logger.debug("[frap] Could not access Page field: {}", field.getName());
                }
            }
        }
        return null;
    }

    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(STORE_NS));
    }
}
