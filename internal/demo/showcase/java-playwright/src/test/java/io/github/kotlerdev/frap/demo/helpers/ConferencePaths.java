package io.github.kotlerdev.frap.demo.helpers;

import io.github.kotlerdev.frap.playwright.config.WithFrapOptions;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Conference 2026 Spring test paths and utilities.
 * Mirrors TypeScript {@code CONF_PATH} and {@code confFletta} from helpers.ts.
 */
public final class ConferencePaths {

    private ConferencePaths() {}

    /** Base URL for the test server */
    public static final String BASE_URL = System.getProperty("test.server.url", "http://localhost:3000");

    /** Report directory for Conference tests */
    public static final Path CONF_REPORT_DIR = Paths.get(
        System.getProperty("frap.reportDir", "target/frap-reports/conference")
    );

    /** Conference page paths */
    public static final class Conf {
        public static final String INDEX = "/conference/index.html";
        public static final String SCHEDULE_V1 = "/conference/schedule-v1.html";
        public static final String SCHEDULE_V2 = "/conference/schedule-v2.html";
        public static final String SCHEDULE_HEAL = "/conference/schedule-heal.html";
        public static final String REGISTER = "/conference/register.html";
        public static final String CFP = "/conference/cfp.html";
        public static final String SPEAKERS = "/conference/speakers.html";

        public static String speaker(String id) {
            return "/conference/speaker.html?id=" + id;
        }

        public static String talk(String id) {
            return "/conference/talk.html?id=" + id;
        }
    }

    /**
     * Creates Frap options for Conference tests.
     */
    public static WithFrapOptions confFrap(WithFrapOptions options) {
        if (options == null) {
            options = new WithFrapOptions();
        }
        return options
            .reportDir(CONF_REPORT_DIR)
            .minConfidence(0.85)
            .enableHealing(true)
            .enableReporting(true);
    }

    /**
     * Creates default Frap options for Conference tests.
     */
    public static WithFrapOptions confFrap() {
        return confFrap(new WithFrapOptions());
    }
}
