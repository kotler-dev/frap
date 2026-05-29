package io.github.kotlerdev.frap.playwright.wrapper;

import com.microsoft.playwright.Locator;
import io.github.kotlerdev.frap.core.config.FrapConfig;
import io.github.kotlerdev.frap.core.dto.HealResult;
import io.github.kotlerdev.frap.core.client.FrapCoreClient;

/**
 * Extended Locator with Frap healing metadata.
 * Mirrors TypeScript {@code FrapLocator} from wrapper.ts.
 */
public interface FrapLocator extends Locator {

    /**
     * Returns the original locator before any healing.
     */
    Locator originalLocator();

    /**
     * Returns the Frap configuration for this locator.
     */
    FrapConfig config();

    /**
     * Returns the last heal result, or null if no healing occurred.
     */
    HealResult lastHealResult();

    /**
     * Returns the test name associated with this locator.
     */
    String testName();

    /**
     * Returns the selector string used for this locator.
     */
    String selector();

    /**
     * Returns true if this locator has been healed.
     */
    default boolean isHealed() {
        HealResult result = lastHealResult();
        return result != null && result.healed();
    }

    /**
     * Returns the confidence score from the last healing attempt.
     */
    default double confidence() {
        HealResult result = lastHealResult();
        return result != null ? result.confidence() : 1.0;
    }

    /**
     * Internal data holder for Frap metadata.
     */
    class FrapData {
        public final Locator originalLocator;
        public final FrapConfig config;
        public final String testName;
        public final String selector;
        public final FrapCoreClient client;
        public volatile HealResult lastHealResult;

        public FrapData(Locator originalLocator, FrapConfig config, String testName,
                       String selector, FrapCoreClient client) {
            this.originalLocator = originalLocator;
            this.config = config;
            this.testName = testName;
            this.selector = selector;
            this.client = client;
        }
    }
}
