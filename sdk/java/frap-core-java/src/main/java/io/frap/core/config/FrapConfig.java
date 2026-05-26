package io.frap.core.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.frap.core.semantics.HealPolicy;

/**
 * Configuration for Frap SDK.
 * Mirrors TypeScript {@code FrapConfig} from config.ts.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FrapConfig(
    @JsonProperty("minConfidence") double minConfidence,
    @JsonProperty("reportDir") String reportDir,
    @JsonProperty("enableHealing") boolean enableHealing,
    @JsonProperty("enableReporting") boolean enableReporting,
    @JsonProperty("debug") Object debug, // boolean or "verbose"
    @JsonProperty("healPolicy") HealPolicy healPolicy,
    @JsonProperty("captureAll") boolean captureAll
) {
    public static final double DEFAULT_MIN_CONFIDENCE = 0.85;
    public static final String DEFAULT_REPORT_DIR = "./frap-reports";

    public FrapConfig {
        if (minConfidence <= 0.0 || minConfidence > 1.0) {
            throw new IllegalArgumentException("minConfidence must be between 0.0 and 1.0");
        }
        if (reportDir == null || reportDir.isBlank()) {
            reportDir = DEFAULT_REPORT_DIR;
        }
        if (healPolicy == null) {
            healPolicy = HealPolicy.ALLOW;
        }
    }

    /**
     * Creates a default configuration.
     */
    public static FrapConfig defaults() {
        return new FrapConfig(
            DEFAULT_MIN_CONFIDENCE,
            DEFAULT_REPORT_DIR,
            true,
            true,
            true,
            HealPolicy.ALLOW,
            false
        );
    }

    /**
     * Merges user configuration with defaults.
     */
    public static FrapConfig merge(FrapConfig userConfig) {
        if (userConfig == null) {
            return defaults();
        }
        return new FrapConfig(
            userConfig.minConfidence() > 0.0 ? userConfig.minConfidence() : DEFAULT_MIN_CONFIDENCE,
            userConfig.reportDir() != null ? userConfig.reportDir() : DEFAULT_REPORT_DIR,
            userConfig.enableHealing(),
            userConfig.enableReporting(),
            userConfig.debug(),
            userConfig.healPolicy() != null ? userConfig.healPolicy() : HealPolicy.ALLOW,
            userConfig.captureAll()
        );
    }

    /**
     * Returns true if debug mode is enabled (either boolean true or "verbose").
     */
    public boolean isDebugEnabled() {
        if (debug == null) {
            return false;
        }
        if (debug instanceof Boolean) {
            return (Boolean) debug;
        }
        return "verbose".equals(debug);
    }

    /**
     * Returns true if verbose debug mode is enabled.
     */
    public boolean isVerboseDebug() {
        return "verbose".equals(debug);
    }
}
