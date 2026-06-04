package io.github.kotlerdev.frap.core.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FrapRuntimePathsTest {

    @TempDir
    Path tempDir;

    @Test
    void resolveRuntimeDirHonorsSystemProperty() {
        final String previous = System.getProperty(FrapRuntimePaths.RUNTIME_DIR_PROPERTY);
        try {
            System.setProperty(FrapRuntimePaths.RUNTIME_DIR_PROPERTY, tempDir.resolve("custom-runtime").toString());
            assertThat(FrapRuntimePaths.resolveRuntimeDir())
                .isEqualTo(tempDir.resolve("custom-runtime").toAbsolutePath().normalize());
        } finally {
            restoreProperty(FrapRuntimePaths.RUNTIME_DIR_PROPERTY, previous);
        }
    }

    @Test
    void resolveWorkDirDefaultsUnderRuntimeDir() {
        final String runtimePrevious = System.getProperty(FrapRuntimePaths.RUNTIME_DIR_PROPERTY);
        final String workPrevious = System.getProperty(FrapRuntimePaths.WORK_DIR_PROPERTY);
        try {
            final Path runtime = tempDir.resolve("rt");
            System.setProperty(FrapRuntimePaths.RUNTIME_DIR_PROPERTY, runtime.toString());
            System.clearProperty(FrapRuntimePaths.WORK_DIR_PROPERTY);
            assertThat(FrapRuntimePaths.resolveWorkDir(null))
                .isEqualTo(runtime.resolve("work").toAbsolutePath().normalize());
        } finally {
            restoreProperty(FrapRuntimePaths.RUNTIME_DIR_PROPERTY, runtimePrevious);
            restoreProperty(FrapRuntimePaths.WORK_DIR_PROPERTY, workPrevious);
        }
    }

    @Test
    void resolveWorkDirHonorsExplicitOverride() {
        final Path work = tempDir.resolve("artifacts-only");
        assertThat(FrapRuntimePaths.resolveWorkDir(work.toString()))
            .isEqualTo(work.toAbsolutePath().normalize());
    }

    private static void restoreProperty(final String key, final String previous) {
        if (previous == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, previous);
        }
    }
}
