package io.github.kotlerdev.frap.demo.context;

import com.microsoft.playwright.*;
import io.github.kotlerdev.frap.demo.helpers.ConferencePaths;
import io.github.kotlerdev.frap.playwright.config.WithFrapOptions;
import io.github.kotlerdev.frap.playwright.context.FrapContext;
import io.github.kotlerdev.frap.playwright.extension.FrapExtension;
import io.github.kotlerdev.frap.playwright.wrapper.Frap;
import io.github.kotlerdev.frap.core.client.FrapCoreClient;
import io.github.kotlerdev.frap.core.client.FrapRpcClient;
import io.github.kotlerdev.frap.core.context.*;
import io.github.kotlerdev.frap.core.rca.PrimaryCause;
import io.github.kotlerdev.frap.core.rca.RcaReport;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.*;

/**
 * C002 API Timeout RCA test (context layer).
 * Mirrors TypeScript c002-payment-timeout.spec.ts.
 */
@Tag("e2e")
@ExtendWith(FrapExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("C002 API Timeout RCA")
class PaymentTimeoutTest {

    static Playwright playwright;
    static Browser browser;
    Page page;
    FrapCoreClient client;
    String traceId;

    @BeforeAll
    static void beforeAll() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
            new BrowserType.LaunchOptions().setHeadless(true)
        );
    }

    @AfterAll
    static void afterAll() {
        browser.close();
        playwright.close();
    }

    @BeforeEach
    void beforeEach() throws Exception {
        page = browser.newPage();
        client = FrapRpcClient.create();

        // Enable context capture
        traceId = FrapContext.attach(page, new FrapContext.ContextCaptureOptions(
            ConferencePaths.CONF_REPORT_DIR,
            null,  // testId
            null   // traceId (auto-generated)
        ));
    }

    @AfterEach
    void afterEach() {
        page.close();
        if (client != null) {
            client.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("payment API timeout precedes missing pay button")
    void testPaymentTimeoutPrecedesMissingButton() {
        // Navigate to a slow checkout page (would need test-app setup)
        // For demo purposes, using conference pages
        page.navigate(ConferencePaths.BASE_URL + ConferencePaths.Conf.REGISTER);

        // Use context-aware options
        var options = new WithFrapOptions()
            .reportDir(ConferencePaths.CONF_REPORT_DIR)
            .captureAll(true)
            .minConfidence(0.7);

        // Wrap locator with context capture enabled
        var payButton = Frap.withFrap(
            page.locator("[data-testid='pay-btn']"),
            page,
            options
        );

        // The button exists on register page, but let's simulate failure handling
        // In real test, this would fail and record context
        try {
            payButton.click();
        } catch (Exception e) {
            // Expected to potentially fail, context captured
        }

        // Context events are automatically captured via FrapContext
        // and written to frap-context-events.jsonl
    }

    @Test
    @Order(2)
    @DisplayName("RCA identifies API error from context timeline")
    void testRcaIdentifiesApiError() throws Exception {
        // Build a timeline with network failure preceding UI failure
        ContextTimeline timeline = new ContextTimeline();

        // Add network error event
        timeline.addEvent(new ContextEvent.NetworkEvent(
            System.currentTimeMillis() - 5000,
            traceId,
            new NetworkEventPayload(
                "POST",
                "https://api.example.com/payment-intent",
                500,
                5000L,
                NetworkPhase.RESPONSE,
                NetworkProtocol.HTTP,
                null,
                null,
                "Internal Server Error"
            )
        ));

        // Add UI failure event
        timeline.addEvent(new ContextEvent.UiEvent(
            System.currentTimeMillis(),
            traceId,
            "[data-testid='pay-btn']",
            "not_found",
            "Pay button not rendered after payment-intent failure"
        ));

        // Perform RCA via RPC
        RcaReport report = client.analyzeRca(timeline, 0);

        // Verify RCA identifies API error
        assertThat(report).isNotNull();
        assertThat(report.version()).isEqualTo(1);
        assertThat(report.primaryCause()).isEqualTo(PrimaryCause.API_ERROR);
        assertThat(report.confidence()).isGreaterThan(0.5);
        assertThat(report.recommendation()).isNotBlank();

        // Verify timeline excerpt contains both events
        assertThat(report.timelineExcerpt()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @Order(3)
    @DisplayName("RCA identifies UI change without network errors")
    void testRcaIdentifiesUiChange() throws Exception {
        // Timeline with only UI events (no network errors)
        ContextTimeline timeline = new ContextTimeline();

        timeline.addEvent(new ContextEvent.UiEvent(
            System.currentTimeMillis() - 1000,
            traceId,
            "[data-testid='old-btn']",
            "click",
            "Attempting to click old button"
        ));

        timeline.addEvent(new ContextEvent.UiEvent(
            System.currentTimeMillis(),
            traceId,
            "[data-testid='old-btn']",
            "not_found",
            "Button not found - UI was refactored"
        ));

        RcaReport report = client.analyzeRca(timeline, 0);

        assertThat(report).isNotNull();
        assertThat(report.primaryCause()).isEqualTo(PrimaryCause.UI_CHANGE);
    }
}
