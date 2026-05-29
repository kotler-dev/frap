package io.github.kotlerdev.frap.playwright.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.microsoft.playwright.options.LoadState;
import io.github.kotlerdev.frap.core.config.FrapConfig;

/**
 * Configuration for Frap Playwright adapter.
 * Mirrors TypeScript {@code FrapPlaywrightConfig} from config.ts.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FrapPlaywrightConfig(
    FrapConfig frap,
    PlaywrightOptions playwright
) {

    public FrapPlaywrightConfig {
        if (frap == null) {
            frap = FrapConfig.defaults();
        }
        if (playwright == null) {
            playwright = PlaywrightOptions.defaults();
        }
    }

    public static FrapPlaywrightConfig defaults() {
        return new FrapPlaywrightConfig(FrapConfig.defaults(), PlaywrightOptions.defaults());
    }

    /**
     * Merges user configuration with defaults.
     */
    public static FrapPlaywrightConfig merge(FrapPlaywrightConfig userConfig) {
        if (userConfig == null) {
            return defaults();
        }
        return new FrapPlaywrightConfig(
            FrapConfig.merge(userConfig.frap()),
            PlaywrightOptions.merge(userConfig.playwright())
        );
    }

    /**
     * Playwright-specific options.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PlaywrightOptions(
        Integer navigationTimeout,
        Integer actionTimeout,
        LoadState[] waitUntil
    ) {
        public static PlaywrightOptions defaults() {
            return new PlaywrightOptions(30000, 5000, new LoadState[]{LoadState.NETWORKIDLE});
        }

        public static PlaywrightOptions merge(PlaywrightOptions userOptions) {
            if (userOptions == null) {
                return defaults();
            }
            PlaywrightOptions defaults = defaults();
            return new PlaywrightOptions(
                userOptions.navigationTimeout() != null ? userOptions.navigationTimeout() : defaults.navigationTimeout(),
                userOptions.actionTimeout() != null ? userOptions.actionTimeout() : defaults.actionTimeout(),
                userOptions.waitUntil() != null ? userOptions.waitUntil() : defaults.waitUntil()
            );
        }
    }
}
