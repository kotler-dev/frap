package io.frap.core.client;

import io.frap.core.context.ContextEvent;
import io.frap.core.context.ContextTimeline;
import io.frap.core.context.NetworkEventPayload;
import io.frap.core.context.NetworkPhase;
import io.frap.core.dto.*;
import io.frap.core.rca.PrimaryCause;
import io.frap.core.rca.RcaReport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for FrapCoreClient with frap-core-rpc binary.
 * Requires the binary to be built: cargo build --release -p frap-core --bin frap-core-rpc
 */
class FrapCoreClientTest {

    private FrapCoreClient client;

    @BeforeEach
    void setUp() throws IOException {
        String binaryPath = findBinaryPath();
        client = new FrapCoreClient(binaryPath);
        assertThat(client.isAlive()).isTrue();
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    private String findBinaryPath() {
        // Try env var first
        String envPath = System.getenv("FRAP_CORE_BIN");
        if (envPath != null) {
            return envPath;
        }

        // Try common locations relative to repo root
        String[] candidates = {
            "crates/target/release/frap-core-rpc",
            "crates/target/debug/frap-core-rpc",
            "../crates/target/release/frap-core-rpc",
            "../crates/target/debug/frap-core-rpc",
            "../../crates/target/release/frap-core-rpc",
            "../../crates/target/debug/frap-core-rpc",
        };

        for (String candidate : candidates) {
            File f = new File(candidate);
            if (f.exists() && f.canExecute()) {
                return candidate;
            }
        }

        throw new RuntimeException(
            "frap-core-rpc binary not found. Set FRAP_CORE_BIN env var or build with: " +
            "cargo build --release -p frap-core --bin frap-core-rpc"
        );
    }

    @Test
    void testHealWithBrokenSelector() throws IOException {
        // Setup: original signature for "pay-btn", but DOM has "checkout-pay"
        DOMToken token = new DOMToken("button", "submit", 0);
        Signature originalSig = new Signature(
            List.of(token),
            "button:submit",
            Map.of("data-testid", "pay-btn"),
            1
        );

        DOMElementInfo element = new DOMElementInfo(
            "[data-testid='checkout-pay']",
            "button",
            Map.of("data-testid", "checkout-pay"),
            "Pay",
            List.of("button:submit")
        );

        DOMSnapshot snapshot = new DOMSnapshot(
            "<button data-testid='checkout-pay'>Pay</button>",
            List.of(element)
        );

        HealRequest request = new HealRequest(
            "[data-testid='pay-btn']",
            originalSig,
            snapshot,
            0.7  // Lower threshold for test
        );

        HealResult result = client.heal(request);

        assertThat(result.healed()).isTrue();
        assertThat(result.selector()).isEqualTo("[data-testid='checkout-pay']");
        assertThat(result.confidence()).isGreaterThanOrEqualTo(0.7);
        assertThat(result.topCandidates()).isNotEmpty();
    }

    @Test
    void testHealWithFoundSelector() throws IOException {
        // Setup: selector matches exactly
        DOMToken token = new DOMToken("button", "submit", 0);
        Signature originalSig = new Signature(
            List.of(token),
            "button:submit",
            Map.of("data-testid", "pay-btn"),
            1
        );

        DOMElementInfo element = new DOMElementInfo(
            "[data-testid='pay-btn']",
            "button",
            Map.of("data-testid", "pay-btn"),
            "Pay",
            List.of("button:submit")
        );

        DOMSnapshot snapshot = new DOMSnapshot(
            "<button data-testid='pay-btn'>Pay</button>",
            List.of(element)
        );

        HealRequest request = new HealRequest(
            "[data-testid='pay-btn']",
            originalSig,
            snapshot,
            0.85
        );

        HealResult result = client.heal(request);

        // Should not heal when element is found
        assertThat(result.healed()).isFalse();
        assertThat(result.confidence()).isEqualTo(1.0);
    }

    @Test
    void testAnalyzeRcaWithUiFailure() throws IOException {
        ContextTimeline timeline = new ContextTimeline();
        timeline.addEvent(new ContextEvent.UiEvent(
            1000L,
            "test-trace",
            "[data-testid='pay-btn']",
            "click"
        ));
        timeline.addEvent(new ContextEvent.UiEvent(
            2000L,
            "test-trace",
            "[data-testid='pay-btn']",
            "failure",
            "Element not found"
        ));

        RcaReport report = client.analyzeRca(timeline, 0);

        assertThat(report).isNotNull();
        assertThat(report.version()).isEqualTo(1);
        assertThat(report.primaryCause()).isEqualTo(PrimaryCause.UI_CHANGE);
        assertThat(report.confidence()).isGreaterThan(0);
        assertThat(report.recommendation()).isNotBlank();
    }

    @Test
    void testAnalyzeRcaWithNetworkError() throws IOException {
        ContextTimeline timeline = new ContextTimeline();
        timeline.addEvent(new ContextEvent.NetworkEvent(
            1000L,
            "test-trace",
            new NetworkEventPayload(
                "POST",
                "https://api.example.com/payment",
                500,
                null,
                NetworkPhase.RESPONSE,
                null,
                null,
                null,
                null
            )
        ));
        timeline.addEvent(new ContextEvent.UiEvent(
            2000L,
            "test-trace",
            "[data-testid='pay-btn']",
            "failure"
        ));

        RcaReport report = client.analyzeRca(timeline, 0);

        assertThat(report).isNotNull();
        assertThat(report.primaryCause()).isEqualTo(PrimaryCause.API_ERROR);
    }

    @Test
    void testClientLifecycle() throws IOException {
        assertThat(client.isAlive()).isTrue();
        assertThat(client.pid()).isGreaterThan(0);

        client.close();
        assertThat(client.isAlive()).isFalse();
    }

    @Test
    void testHealWithNullRequest() {
        assertThatThrownBy(() -> client.heal(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("request is required");
    }

    @Test
    void testAnalyzeRcaWithNullTimeline() {
        assertThatThrownBy(() -> client.analyzeRca(null, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("timeline is required");
    }
}
