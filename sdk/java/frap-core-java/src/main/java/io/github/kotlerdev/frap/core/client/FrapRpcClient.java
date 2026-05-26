package io.github.kotlerdev.frap.core.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.github.kotlerdev.frap.core.context.ContextTimeline;
import io.github.kotlerdev.frap.core.dto.HealRequest;
import io.github.kotlerdev.frap.core.dto.HealResult;
import io.github.kotlerdev.frap.core.rca.RcaReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * JSON-RPC client for frap-core-rpc binary.
 * Communicates with Rust core via stdin/stdout NDJSON protocol.
 *
 * <p>This client manages a long-lived subprocess and is thread-safe for
 * concurrent requests (each request is atomic on the stream).</p>
 *
 * <p>For production use with lower latency requirements, consider
 * {@code FrapNativeClient} (JNI-based) instead.</p>
 */
public class FrapRpcClient implements FrapCoreClient {
    private static final Logger logger = LoggerFactory.getLogger(FrapRpcClient.class);

    private final ObjectMapper objectMapper;
    private final Process process;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final AtomicInteger nextId;
    private final AtomicReference<IOException> lastError;
    private final Object lock;

    /**
     * Creates a client with auto-detection of binary:
     * 1. FRAP_CORE_BIN environment variable (for development)
     * 2. Bundled binary extracted from JAR (for Maven Central distribution)
     *
     * @throws IOException if the binary cannot be started or extracted
     */
    public static FrapRpcClient create() throws IOException {
        // 1. Check FRAP_CORE_BIN env (for development)
        String envPath = System.getenv("FRAP_CORE_BIN");
        if (envPath != null && new File(envPath).exists()) {
            return new FrapRpcClient(envPath);
        }

        // 2. Try development paths (repo structure)
        Path devPath = Paths.get("crates", "target", "release", "frap-core-rpc");
        if (!devPath.toFile().exists()) {
            devPath = Paths.get("crates", "target", "debug", "frap-core-rpc");
        }
        if (devPath.toFile().exists()) {
            return new FrapRpcClient(devPath.toString());
        }

        // 3. Extract bundled binary from JAR (for Maven Central users)
        String binaryName = getBundledBinaryName();
        Path extractedPath = extractBundledBinary(binaryName);
        return new FrapRpcClient(extractedPath.toString());
    }

    /**
     * Returns the bundled binary name for the current platform.
     */
    private static String getBundledBinaryName() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        if (os.contains("linux") && arch.contains("amd64")) {
            return "frap-core-rpc-linux-x86_64";
        } else if (os.contains("mac")) {
            if (arch.contains("aarch64")) {
                return "frap-core-rpc-macos-aarch64";
            }
            return "frap-core-rpc-macos-x86_64";
        }
        throw new UnsupportedOperationException(
            "Platform not supported: " + os + " " + arch +
            ". Supported: Linux x86_64, macOS x86_64/aarch64"
        );
    }

    /**
     * Extracts the bundled binary from JAR to a temporary directory.
     */
    private static Path extractBundledBinary(String binaryName) throws IOException {
        String resourcePath = "/META-INF/native/" + binaryName;

        try (InputStream is = FrapRpcClient.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException(
                    "Bundled binary not found: " + resourcePath +
                    ". This platform may not be supported or the binary was not bundled." +
                    " Set FRAP_CORE_BIN environment variable to use a custom binary."
                );
            }

            // Create temp directory
            Path tempDir = Files.createTempDirectory("frap-rpc-");
            tempDir.toFile().deleteOnExit();

            Path binaryPath = tempDir.resolve("frap-core-rpc");
            Files.copy(is, binaryPath, StandardCopyOption.REPLACE_EXISTING);

            // Make executable (on Unix systems)
            binaryPath.toFile().setExecutable(true);
            binaryPath.toFile().deleteOnExit();

            logger.info("Extracted bundled binary to: {}", binaryPath);
            return binaryPath;
        }
    }

    /**
     * Creates a client with a specific binary path.
     *
     * @param binaryPath path to frap-core-rpc executable
     * @throws IOException if the binary cannot be started
     */
    public FrapRpcClient(String binaryPath) throws IOException {
        this.objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        File binary = new File(binaryPath);
        if (!binary.exists()) {
            throw new FileNotFoundException("frap-core-rpc binary not found: " + binaryPath);
        }

        logger.debug("Starting frap-core-rpc: {}", binaryPath);
        this.process = new ProcessBuilder(binaryPath)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start();

        this.reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        this.nextId = new AtomicInteger(1);
        this.lastError = new AtomicReference<>();
        this.lock = new Object();

        // Verify process is alive
        if (!process.isAlive()) {
            throw new IOException("frap-core-rpc process failed to start");
        }

        logger.info("frap-core-rpc client started (pid: {})", process.pid());
    }

    @Override
    public HealResult heal(HealRequest request) throws IOException {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }

        int id = nextId.getAndIncrement();
        RpcRequest rpcRequest = new RpcRequest(id, "heal", request);

        String responseJson = sendRequest(rpcRequest);
        RpcResponse response = parseResponse(responseJson);

        if (response.isError()) {
            throw new IOException("RPC error [" + response.error().code() + "]: " + response.error().message());
        }

        try {
            return objectMapper.readValue(response.result(), HealResult.class);
        } catch (JsonProcessingException e) {
            throw new IOException("Failed to parse HealResult: " + e.getMessage(), e);
        }
    }

    @Override
    public RcaReport analyzeRca(ContextTimeline timeline, long failureAtMs) throws IOException {
        if (timeline == null) {
            throw new IllegalArgumentException("timeline is required");
        }

        int id = nextId.getAndIncrement();

        // Build params object matching Rust expectations
        var params = new java.util.HashMap<String, Object>();
        params.put("timeline", timeline);
        params.put("failure_at_ms", failureAtMs);
        params.put("window_ms", 30000L); // Default 30s window

        RpcRequest rpcRequest = new RpcRequest(id, "analyze_rca", params);

        String responseJson = sendRequest(rpcRequest);
        RpcResponse response = parseResponse(responseJson);

        if (response.isError()) {
            throw new IOException("RPC error [" + response.error().code() + "]: " + response.error().message());
        }

        try {
            return objectMapper.readValue(response.result(), RcaReport.class);
        } catch (JsonProcessingException e) {
            throw new IOException("Failed to parse RcaReport: " + e.getMessage(), e);
        }
    }

    /**
     * Sends a request and returns the response JSON string.
     */
    private String sendRequest(RpcRequest request) throws IOException {
        String requestJson;
        try {
            requestJson = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new IOException("Failed to serialize request: " + e.getMessage(), e);
        }

        logger.debug("RPC request: {}", requestJson);

        synchronized (lock) {
            // Write request line
            writer.write(requestJson);
            writer.newLine();
            writer.flush();

            // Read response line
            String responseJson = reader.readLine();
            if (responseJson == null) {
                throw new IOException("frap-core-rpc closed connection");
            }

            logger.debug("RPC response: {}", responseJson);
            return responseJson;
        }
    }

    private RpcResponse parseResponse(String json) throws IOException {
        try {
            return objectMapper.readValue(json, RpcResponse.class);
        } catch (JsonProcessingException e) {
            throw new IOException("Failed to parse RPC response: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAlive() {
        return process.isAlive();
    }

    @Override
    public long pid() {
        try {
            return process.pid();
        } catch (UnsupportedOperationException e) {
            return -1;
        }
    }

    @Override
    public void close() {
        logger.debug("Closing frap-core-rpc client");

        try {
            writer.close();
        } catch (IOException e) {
            logger.warn("Error closing writer: {}", e.getMessage());
        }

        try {
            reader.close();
        } catch (IOException e) {
            logger.warn("Error closing reader: {}", e.getMessage());
        }

        process.destroy();

        // Wait briefly for graceful shutdown
        try {
            if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }

        logger.info("frap-core-rpc client closed");
    }
}
