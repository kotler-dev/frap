package io.github.kotlerdev.frap.core.runtime;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves on-disk runtime directories for the frap Java stack.
 *
 * <p>Corporate environments often block executing binaries from {@code /tmp}
 * ({@code noexec} or policy). By default, frap uses a {@code .frap} directory
 * next to the running application (JAR parent or {@code user.dir}) for the
 * extracted native binary, MCP file artefacts, and (when configured) logs.</p>
 *
 * <p>Override base directory with {@code -Dfrap.runtime.dir=/path},
 * {@code FRAP_RUNTIME_DIR}, or Spring {@code frap.runtime.dir}. Override only
 * artefacts with {@code frap.io.work-dir} / {@code FRAP_IO_WORK_DIR}.</p>
 */
public final class FrapRuntimePaths {

    /** JVM system property for the frap runtime base directory. */
    public static final String RUNTIME_DIR_PROPERTY = "frap.runtime.dir";

    /** Environment variable for the frap runtime base directory. */
    public static final String RUNTIME_DIR_ENV = "FRAP_RUNTIME_DIR";

    /** Spring / JVM property for MCP file-mode work directory. */
    public static final String WORK_DIR_PROPERTY = "frap.io.work-dir";

    /** Environment variable for MCP file-mode work directory. */
    public static final String WORK_DIR_ENV = "FRAP_IO_WORK_DIR";

    private static final String RUNTIME_FOLDER = ".frap";

    private FrapRuntimePaths() {}

    /**
     * Base runtime directory: {@code <app-base>/.frap} unless overridden.
     */
    public static Path resolveRuntimeDir() {
        final String explicit = firstNonBlank(
            System.getProperty(RUNTIME_DIR_PROPERTY),
            System.getenv(RUNTIME_DIR_ENV)
        );
        if (hasText(explicit)) {
            return Path.of(explicit).toAbsolutePath().normalize();
        }
        return applicationBaseDir().resolve(RUNTIME_FOLDER).toAbsolutePath().normalize();
    }

    /**
     * Directory where the bundled {@code frap-core-rpc} binary is extracted.
     */
    public static Path resolveNativeBinDir() {
        return resolveRuntimeDir().resolve("bin");
    }

    /**
     * MCP file-mode work directory.
     *
     * @param configuredValue value from Spring {@code frap.io.work-dir} (may be blank)
     */
    public static Path resolveWorkDir(final String configuredValue) {
        final String explicit = firstNonBlank(
            configuredValue,
            System.getProperty(WORK_DIR_PROPERTY),
            System.getenv(WORK_DIR_ENV)
        );
        if (hasText(explicit)) {
            return Path.of(explicit).toAbsolutePath().normalize();
        }
        return resolveRuntimeDir().resolve("work").toAbsolutePath().normalize();
    }

    /**
     * Default log file path under the runtime directory.
     */
    public static Path resolveDefaultLogFile() {
        return resolveRuntimeDir().resolve("logs").resolve("frap-mcp.log");
    }

    /**
     * Directory containing the launched JAR, or {@code user.dir} when unknown.
     */
    public static Path applicationBaseDir() {
        final Path fromCodeSource = codeSourceBaseDir();
        if (fromCodeSource != null) {
            return fromCodeSource;
        }
        return Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
    }

    private static Path codeSourceBaseDir() {
        final URL location = FrapRuntimePaths.class.getProtectionDomain().getCodeSource().getLocation();
        if (location == null) {
            return null;
        }
        try {
            final Path path = Path.of(location.toURI()).toAbsolutePath().normalize();
            if (Files.isRegularFile(path)) {
                final Path parent = path.getParent();
                return parent != null ? parent : path;
            }
            if (Files.isDirectory(path)) {
                return Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
            }
        } catch (final URISyntaxException | IllegalArgumentException ignored) {
            // fall through
        }
        return null;
    }

    /**
     * Creates {@code dir} and any missing parents.
     */
    public static Path ensureDirectory(final Path dir) throws IOException {
        return Files.createDirectories(dir);
    }

    private static String firstNonBlank(final String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (final String candidate : candidates) {
            if (hasText(candidate)) {
                return candidate.trim();
            }
        }
        return null;
    }

    private static boolean hasText(final String value) {
        return value != null && !value.isBlank();
    }
}
