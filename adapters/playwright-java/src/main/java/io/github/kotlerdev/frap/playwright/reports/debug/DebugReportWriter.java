package io.github.kotlerdev.frap.playwright.reports.debug;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Writes per-test debug JSON (mirrors TS {@code writeDebugReport}).
 */
public final class DebugReportWriter {
    private static final Logger logger = LoggerFactory.getLogger(DebugReportWriter.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    private DebugReportWriter() {}

    public static Path debugReportsDir(Path reportDir) {
        return reportDir.resolve("debug-reports");
    }

    public static String debugReportSlug(String testName) {
        String base = testName
            .replaceAll("[^a-zA-Z0-9\\u0400-\\u04FF]+", "-")
            .replaceAll("^-+|-+$", "");
        if (base.length() > 60) {
            base = base.substring(0, 60);
        }
        if (base.isEmpty()) {
            base = "test";
        }
        String hash = sha256Hex(testName).substring(0, 8);
        return base + "-" + hash;
    }

    public static void write(Path reportDir, DebugReportModels.DebugReport report) throws IOException {
        Files.createDirectories(reportDir);
        Path subDir = debugReportsDir(reportDir);
        Files.createDirectories(subDir);

        String slug = debugReportSlug(report.testName());
        Path perTest = subDir.resolve(slug + ".json");
        MAPPER.writeValue(perTest.toFile(), report);
        MAPPER.writeValue(reportDir.resolve("frap-debug.json").toFile(), report);

        logger.info("[frap:debug] Report saved for \"{}\" → {}", report.testName(), perTest);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
