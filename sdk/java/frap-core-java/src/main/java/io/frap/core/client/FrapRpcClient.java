package io.frap.core.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.frap.core.context.ContextTimeline;
import io.frap.core.dto.HealRequest;
import io.frap.core.dto.HealResult;
import io.frap.core.rca.RcaReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
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
     * Creates a client with the default binary path (from FRAP_CORE_BIN env or repo root).
     *
     * @throws IOException if the binary cannot be started
     */
    public static FrapRpcClient create() throws IOException {
        String binaryPath = System.getenv("FRAP_CORE_BIN");
        if (binaryPath == null) {
            // Try to find relative to repo root (common development case)
            Path defaultPath = Paths.get("crates", "target", "release", "frap-core-rpc");
            if (!defaultPath.toFile().exists()) {
                defaultPath = Paths.get("crates", "target", "debug", "frap-core-rpc");
            }
            binaryPath = defaultPath.toString();
        }
        return new FrapRpcClient(binaryPath);
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
