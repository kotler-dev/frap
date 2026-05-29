package io.github.kotlerdev.frap.playwright.context;

import com.microsoft.playwright.*;
import io.github.kotlerdev.frap.core.context.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Context capture for Playwright pages (network, console, UI events).
 * Mirrors TypeScript {@code attachFrapContext} from capture.ts.
 */
public class FrapContext {
    private static final Logger logger = LoggerFactory.getLogger(FrapContext.class);
    private static final int PAYLOAD_PREVIEW_MAX = 256;

    private final Page page;
    private final Path reportDir;
    private final String testId;
    private final String traceId;
    private final Map<String, RequestTiming> pendingRequests = new ConcurrentHashMap<>();

    private static class RequestTiming {
        final long startMs;
        final String method;
        final String url;

        RequestTiming(long startMs, String method, String url) {
            this.startMs = startMs;
            this.method = method;
            this.url = url;
        }
    }

    private FrapContext(Page page, Path reportDir, String testId, String traceId) {
        this.page = page;
        this.reportDir = reportDir;
        this.testId = testId;
        this.traceId = traceId;
    }

    /**
     * Attaches context capture to a Playwright page.
     *
     * @param page the page to monitor
     * @param options capture options
     * @return traceId for this capture session
     */
    public static String attach(Page page, ContextCaptureOptions options) {
        Path reportDir = options.reportDir();
        String testId = options.testId() != null ? options.testId() : generateTestId();
        String traceId = options.traceId() != null ? options.traceId() : UUID.randomUUID().toString();

        FrapContext context = new FrapContext(page, reportDir, testId, traceId);
        context.attachListeners();

        logger.debug("[frap] Context attached: traceId={}, testId={}", traceId, testId);
        return traceId;
    }

    private void attachListeners() {
        // Network: request
        page.onRequest(request -> {
            if (!shouldCaptureUrl(request.url())) return;

            String url = request.url();
            String method = request.method();
            String requestId = request.url() + "#" + System.currentTimeMillis();

            pendingRequests.put(requestId, new RequestTiming(System.currentTimeMillis(), method, url));

            pushEvent(new ContextEvent.NetworkEvent(
                System.currentTimeMillis(),
                traceId,
                new NetworkEventPayload(
                    method,
                    url,
                    NetworkPhase.REQUEST
                )
            ));
        });

        // Network: response
        page.onResponse(response -> {
            if (!shouldCaptureUrl(response.url())) return;

            Request request = response.request();
            String url = response.url();
            String requestId = request.url() + "#" + System.currentTimeMillis();

            RequestTiming timing = pendingRequests.remove(requestId);
            long durationMs = timing != null ? System.currentTimeMillis() - timing.startMs : 0;

            pushEvent(new ContextEvent.NetworkEvent(
                System.currentTimeMillis(),
                traceId,
                new NetworkEventPayload(
                    request.method(),
                    url,
                    response.status(),
                    durationMs,
                    NetworkPhase.RESPONSE,
                    NetworkProtocol.HTTP,
                    null,
                    null,
                    response.status() >= 400 ? "HTTP " + response.status() : null
                )
            ));
        });

        // Network: request failed
        page.onRequestFailed(request -> {
            if (!shouldCaptureUrl(request.url())) return;

            String url = request.url();
            String requestId = request.url() + "#" + System.currentTimeMillis();
            pendingRequests.remove(requestId);

            pushEvent(new ContextEvent.NetworkEvent(
                System.currentTimeMillis(),
                traceId,
                new NetworkEventPayload(
                    request.method(),
                    url,
                    null,
                    null,
                    NetworkPhase.FAILED,
                    NetworkProtocol.HTTP,
                    null,
                    null,
                    "Request failed"
                )
            ));
        });

        // Console events
        page.onConsoleMessage(message -> {
            LogLevel level = mapConsoleLevel(message.type());
            String text = message.text();
            String location = message.location() != null ? message.location() : "";

            pushEvent(new ContextEvent.LogEvent(
                System.currentTimeMillis(),
                traceId,
                new LogEventPayload(level, text, location)
            ));
        });

        // Page errors
        page.onPageError(error -> {
            pushEvent(new ContextEvent.LogEvent(
                System.currentTimeMillis(),
                traceId,
                new LogEventPayload(LogLevel.ERROR, error, "page")
            ));
        });

        // WebSocket events
        page.onWebSocket(webSocket -> {
            pushEvent(new ContextEvent.NetworkEvent(
                System.currentTimeMillis(),
                traceId,
                new NetworkEventPayload(
                    "WS",
                    webSocket.url(),
                    null,
                    null,
                    NetworkPhase.OPEN,
                    NetworkProtocol.WEBSOCKET,
                    null,
                    null,
                    null
                )
            ));

            webSocket.onFrameSent(frame -> {
                pushEvent(new ContextEvent.NetworkEvent(
                    System.currentTimeMillis(),
                    traceId,
                    new NetworkEventPayload(
                        "WS",
                        webSocket.url(),
                        null,
                        null,
                        NetworkPhase.MESSAGE,
                        NetworkProtocol.WEBSOCKET,
                        MessageDirection.SENT,
                        truncatePayload(frame.text()),
                        null
                    )
                ));
            });

            webSocket.onFrameReceived(frame -> {
                pushEvent(new ContextEvent.NetworkEvent(
                    System.currentTimeMillis(),
                    traceId,
                    new NetworkEventPayload(
                        "WS",
                        webSocket.url(),
                        null,
                        null,
                        NetworkPhase.MESSAGE,
                        NetworkProtocol.WEBSOCKET,
                        MessageDirection.RECEIVED,
                        truncatePayload(frame.text()),
                        null
                    )
                ));
            });

            webSocket.onClose(reason -> {
                pushEvent(new ContextEvent.NetworkEvent(
                    System.currentTimeMillis(),
                    traceId,
                    new NetworkEventPayload(
                        "WS",
                        webSocket.url(),
                        null,
                        null,
                        NetworkPhase.CLOSE,
                        NetworkProtocol.WEBSOCKET,
                        null,
                        null,
                        null
                    )
                ));
            });
        });
    }

    private void pushEvent(ContextEvent event) {
        try {
            Files.createDirectories(reportDir);
            Path eventsFile = reportDir.resolve("frap-context-events.jsonl");

            String json = io.github.kotlerdev.frap.playwright.reports.JsonlReporter.toJson(event);
            Files.writeString(eventsFile, json + "\n",
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);

        } catch (Exception e) {
            logger.warn("[frap] Failed to record context event: {}", e.getMessage());
        }
    }

    private boolean shouldCaptureUrl(String url) {
        // Skip data URLs, blob URLs, etc.
        return !url.startsWith("data:") && !url.startsWith("blob:") && !url.startsWith("javascript:");
    }

    private LogLevel mapConsoleLevel(String type) {
        return switch (type) {
            case "error" -> LogLevel.ERROR;
            case "warning" -> LogLevel.WARN;
            case "info" -> LogLevel.INFO;
            case "debug" -> LogLevel.DEBUG;
            default -> LogLevel.LOG;
        };
    }

    private String truncatePayload(String payload) {
        if (payload == null) return null;
        if (payload.length() <= PAYLOAD_PREVIEW_MAX) return payload;
        return payload.substring(0, PAYLOAD_PREVIEW_MAX) + "...";
    }

    private static String generateTestId() {
        return "test-" + System.currentTimeMillis();
    }

    /**
     * Options for context capture.
     */
    public record ContextCaptureOptions(
        Path reportDir,
        String testId,
        String traceId
    ) {
        public ContextCaptureOptions {
            if (reportDir == null) {
                throw new IllegalArgumentException("reportDir is required");
            }
        }

        public ContextCaptureOptions(Path reportDir) {
            this(reportDir, null, null);
        }
    }
}
