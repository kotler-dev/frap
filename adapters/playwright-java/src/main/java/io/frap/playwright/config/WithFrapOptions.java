package io.frap.playwright.config;

import io.frap.core.config.FrapConfig;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.nio.file.Path;

/**
 * Options for {@code Frap.withFrap()} method.
 * Mirrors TypeScript {@code WithFrapOptions} from config.ts.
 */
public class WithFrapOptions {
    private Double minConfidence;
    private Path reportDir;
    private Boolean enableHealing;
    private Boolean enableReporting;
    private Boolean debug;
    private String healPolicy;
    private Boolean captureAll;
    private ExtensionContext testContext;

    public WithFrapOptions() {}

    public Double minConfidence() {
        return minConfidence;
    }

    public WithFrapOptions minConfidence(Double minConfidence) {
        this.minConfidence = minConfidence;
        return this;
    }

    public Path reportDir() {
        return reportDir;
    }

    public WithFrapOptions reportDir(Path reportDir) {
        this.reportDir = reportDir;
        return this;
    }

    public Boolean enableHealing() {
        return enableHealing;
    }

    public WithFrapOptions enableHealing(Boolean enableHealing) {
        this.enableHealing = enableHealing;
        return this;
    }

    public Boolean enableReporting() {
        return enableReporting;
    }

    public WithFrapOptions enableReporting(Boolean enableReporting) {
        this.enableReporting = enableReporting;
        return this;
    }

    public Boolean debug() {
        return debug;
    }

    public WithFrapOptions debug(Boolean debug) {
        this.debug = debug;
        return this;
    }

    public String healPolicy() {
        return healPolicy;
    }

    public WithFrapOptions healPolicy(String healPolicy) {
        this.healPolicy = healPolicy;
        return this;
    }

    public Boolean captureAll() {
        return captureAll;
    }

    public WithFrapOptions captureAll(Boolean captureAll) {
        this.captureAll = captureAll;
        return this;
    }

    /**
     * JUnit5 extension context for test identification.
     */
    public ExtensionContext testContext() {
        return testContext;
    }

    public WithFrapOptions testContext(ExtensionContext testContext) {
        this.testContext = testContext;
        return this;
    }

    /**
     * Builds a FrapConfig from these options.
     */
    public FrapConfig toFrapConfig() {
        FrapConfig defaults = FrapConfig.defaults();
        return new FrapConfig(
            minConfidence != null ? minConfidence : defaults.minConfidence(),
            reportDir != null ? reportDir.toString() : defaults.reportDir(),
            enableHealing != null ? enableHealing : defaults.enableHealing(),
            enableReporting != null ? enableReporting : defaults.enableReporting(),
            debug != null ? debug : defaults.debug(),
            healPolicy != null ? io.frap.core.semantics.HealPolicy.valueOf(healPolicy.toUpperCase()) : defaults.healPolicy(),
            captureAll != null ? captureAll : defaults.captureAll()
        );
    }
}
