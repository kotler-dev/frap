package io.frap.core.native_;

import io.frap.core.client.FrapCoreClient;
import io.frap.core.context.ContextTimeline;
import io.frap.core.dto.HealRequest;
import io.frap.core.dto.HealResult;
import io.frap.core.rca.RcaReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

/**
 * JNI-based native client for Frap Core.
 * Uses in-process native library calls instead of subprocess RPC.
 *
 * <p>This implementation provides better performance for high-frequency
 * healing operations by avoiding process spawn overhead.</p>
 *
 * <p>Use this in production environments where:</p>
 * <ul>
 *   <li>JNI is acceptable in the security environment</li>
 *   <li>Low latency is required (healing called many times per test run)</li>
 *   <li>The native library can be bundled with the application</li>
 * </ul>
 *
 * <p>For development or environments where JNI is restricted, use
 * {@link io.frap.core.client.FrapCoreClient} (RPC-based) instead.</p>
 */
public class FrapNativeClient implements FrapCoreClient, AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(FrapNativeClient.class);

    private static volatile boolean libraryLoaded = false;
    private static final Object loadLock = new Object();

    private long nativeHandle;  // Pointer to FrapCoreHandle
    private boolean closed = false;

    /**
     * Creates a new native client with default settings.
     *
     * @throws UnsatisfiedLinkError if the native library cannot be loaded
     * @throws IOException if initialization fails
     */
    public static FrapNativeClient create() throws IOException {
        ensureLibraryLoaded();
        return new FrapNativeClient(nativeCoreNew());
    }

    /**
     * Creates a new native client with custom confidence threshold.
     *
     * @param minConfidence minimum confidence for healing (0.0 - 1.0)
     * @throws UnsatisfiedLinkError if the native library cannot be loaded
     * @throws IOException if initialization fails
     */
    public static FrapNativeClient create(double minConfidence) throws IOException {
        ensureLibraryLoaded();
        return new FrapNativeClient(nativeCoreWithConfidence(minConfidence));
    }

    private FrapNativeClient(long handle) throws IOException {
        if (handle == 0) {
            throw new IOException("Failed to create native FrapCore handle");
        }
        this.nativeHandle = handle;
    }

    private static void ensureLibraryLoaded() {
        if (!libraryLoaded) {
            synchronized (loadLock) {
                if (!libraryLoaded) {
                    loadNativeLibrary();
                    libraryLoaded = true;
                }
            }
        }
    }

    private static void loadNativeLibrary() {
        // Try to load from multiple sources
        String libName = System.mapLibraryName("frap_core");  // frap_core.dll, libfrap_core.so, libfrap_core.dylib

        // 1. Try system property
        String libPath = System.getProperty("frap.native.lib");
        if (libPath != null) {
            try {
                System.load(libPath);
                logger.info("Loaded native library from system property: {}", libPath);
                return;
            } catch (UnsatisfiedLinkError e) {
                logger.warn("Failed to load from system property: {}", e.getMessage());
            }
        }

        // 2. Try java.library.path
        try {
            System.loadLibrary("frap_core");
            logger.info("Loaded native library from java.library.path");
            return;
        } catch (UnsatisfiedLinkError e) {
            logger.debug("Not found in java.library.path: {}", e.getMessage());
        }

        // 3. Extract from JAR to temp and load
        try {
            extractAndLoad(libName);
            logger.info("Extracted and loaded native library from JAR: {}", libName);
            return;
        } catch (IOException e) {
            logger.warn("Failed to extract from JAR: {}", e.getMessage());
        }

        throw new UnsatisfiedLinkError(
            "Could not load frap_core native library. " +
            "Set -Dfrap.native.lib=/path/to/libfrap_core.{so,dylib,dll} " +
            "or ensure the library is in java.library.path"
        );
    }

    private static void extractAndLoad(String libName) throws IOException {
        // Determine platform-specific library name
        String resourcePath = "/META-INF/native/" + getPlatformDir() + "/" + libName;

        // Try to find resource
        try (InputStream is = FrapNativeClient.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Native library not found in JAR: " + resourcePath);
            }

            // Create temp directory
            Path tempDir = Files.createTempDirectory("frap-native-");
            tempDir.toFile().deleteOnExit();

            Path libFile = tempDir.resolve(libName);
            Files.copy(is, libFile, StandardCopyOption.REPLACE_EXISTING);
            libFile.toFile().deleteOnExit();

            System.load(libFile.toAbsolutePath().toString());
        }
    }

    private static String getPlatformDir() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        if (os.contains("linux")) {
            return "linux-" + (arch.contains("aarch64") ? "aarch64" : "x86_64");
        } else if (os.contains("mac") || os.contains("darwin")) {
            return "macos-" + (arch.contains("aarch64") ? "aarch64" : "x86_64");
        } else if (os.contains("windows")) {
            return "windows-" + (arch.contains("aarch64") ? "aarch64" : "x86_64");
        }
        return "unknown";
    }

    @Override
    public HealResult heal(HealRequest request) throws IOException {
        ensureOpen();

        String requestJson = toJson(request);
        String resultJson = nativeHeal(nativeHandle, requestJson);

        if (resultJson == null) {
            String error = nativeLastError();
            throw new IOException("Native healing failed: " + (error != null ? error : "unknown error"));
        }

        return fromJson(resultJson, HealResult.class);
    }

    @Override
    public RcaReport analyzeRca(ContextTimeline timeline, long failureAtMs) throws IOException {
        ensureOpen();

        String timelineJson = toJson(timeline);
        String resultJson = nativeAnalyzeRca(timelineJson, failureAtMs, 30000);

        if (resultJson == null) {
            String error = nativeLastError();
            throw new IOException("Native RCA failed: " + (error != null ? error : "unknown error"));
        }

        return fromJson(resultJson, RcaReport.class);
    }

    @Override
    public boolean isAlive() {
        return !closed && nativeHandle != 0;
    }

    @Override
    public long pid() {
        // Native client doesn't have a separate process
        return ProcessHandle.current().pid();
    }

    @Override
    public void close() {
        if (!closed && nativeHandle != 0) {
            nativeCoreFree(nativeHandle);
            nativeHandle = 0;
            closed = true;
        }
    }

    private void ensureOpen() {
        if (closed || nativeHandle == 0) {
            throw new IllegalStateException("Native client is closed");
        }
    }

    private String toJson(Object obj) throws IOException {
        return new com.fasterxml.jackson.databind.ObjectMapper()
            .setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE)
            .writeValueAsString(obj);
    }

    private <T> T fromJson(String json, Class<T> clazz) throws IOException {
        return new com.fasterxml.jackson.databind.ObjectMapper()
            .setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE)
            .readValue(json, clazz);
    }

    // Native methods
    private static native long nativeCoreNew();
    private static native long nativeCoreWithConfidence(double minConfidence);
    private static native void nativeCoreFree(long handle);
    private static native String nativeHeal(long handle, String requestJson);
    private static native String nativeAnalyzeRca(String timelineJson, long failureAtMs, long windowMs);
    private static native String nativeLastError();
    private static native void nativeClearError();
}
