package io.github.kotlerdev.frap.playwright.wrapper;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.github.kotlerdev.frap.core.client.FrapCoreClient;
import io.github.kotlerdev.frap.core.client.FrapRpcClient;
import io.github.kotlerdev.frap.core.config.FrapConfig;
import io.github.kotlerdev.frap.core.dto.*;
import io.github.kotlerdev.frap.playwright.config.WithFrapOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main entry point for Frap Playwright integration.
 * Provides {@code withFrap()} method to wrap locators with healing capabilities.
 *
 * <p>Mirrors TypeScript {@code withFrap} from wrapper.ts (Proxy-based delegation).</p>
 */
public class Frap {
    private static final Logger logger = LoggerFactory.getLogger(Frap.class);

    private static final Map<String, Signature> recordedSignatures = new ConcurrentHashMap<>();
    private static final ThreadLocal<FrapCoreClient> clientHolder = new ThreadLocal<>();

    private Frap() {}

    public static FrapLocator withFrap(Locator locator, Page page, WithFrapOptions options) {
        FrapConfig config = options != null ? options.toFrapConfig() : FrapConfig.defaults();
        String testName = options != null && options.testContext() != null
            ? options.testContext().getUniqueId()
            : generateTestName();

        String selector = resolveSelector(locator, options);
        logger.debug("[frap] Wrapping locator: {}", selector);

        preRecordSignature(page, locator, selector);
        FrapCoreClient client = getOrCreateClient();

        FrapLocator.FrapData frapData = new FrapLocator.FrapData(
            locator, config, testName, selector, client
        );

        InvocationHandler handler = new FrapHealInvocationHandler(locator, page, frapData);
        return (FrapLocator) Proxy.newProxyInstance(
            Frap.class.getClassLoader(),
            new Class<?>[] {Locator.class, FrapLocator.class},
            handler
        );
    }

    public static FrapLocator withFrap(Locator locator, Page page) {
        return withFrap(locator, page, new WithFrapOptions());
    }

    /**
     * Creates and wraps a locator from an explicit selector string.
     */
    public static FrapLocator withFrap(Page page, String selector, WithFrapOptions options) {
        WithFrapOptions resolvedOptions = options != null ? options : new WithFrapOptions();
        Locator locator = page.locator(selector);
        if (resolvedOptions.selector() == null || resolvedOptions.selector().isBlank()) {
            resolvedOptions.selector(selector);
        }
        return withFrap(locator, page, resolvedOptions);
    }

    public static FrapLocator withFrap(Page page, String selector) {
        return withFrap(page, selector, new WithFrapOptions());
    }

    public static HealResult getLastHealResult(Locator locator) {
        if (locator instanceof FrapLocator fl) {
            return fl.lastHealResult();
        }
        return null;
    }

    public static boolean isHealed(Locator locator) {
        HealResult result = getLastHealResult(locator);
        return result != null && result.healed();
    }

    /**
     * Captures the page DOM and builds a clustered element map via Core.
     */
    public static ElementMap discover(Page page, MapOptions options) throws IOException {
        DOMSnapshot snapshot = new SnapshotBuilder(page).build();
        MapOptions opts = options != null ? options : MapOptions.defaults();
        if (opts.url() == null && page.url() != null) {
            opts = new MapOptions(page.url(), opts.includeNonInteractive(), opts.maxElements());
        }
        return getOrCreateClient().buildElementMap(snapshot, opts);
    }

    public static ElementMap discover(Page page) throws IOException {
        return discover(page, new MapOptions(page.url(), true, null));
    }

    /**
     * Discovers the page, generates Page Object Java sources, and writes files under {@code outputDir}.
     */
    public static List<Path> generatePageObject(Page page, Path outputDir, GenerateOptions options)
        throws IOException {
        ElementMap map = discover(page);
        GeneratedArtifact artifact = getOrCreateClient().generatePageObject(
            map,
            options != null ? options : GenerateOptions.javaPlaywright("GeneratedPage", null)
        );
        List<Path> written = new ArrayList<>();
        for (GeneratedFile file : artifact.files()) {
            Path target = outputDir.resolve(file.path());
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            Files.writeString(target, file.content());
            written.add(target);
            logger.info("[frap] Wrote Page Object: {}", target);
        }
        return written;
    }

    public static void clearSignatures() {
        recordedSignatures.clear();
    }

    public static void clearClient() {
        FrapCoreClient client = clientHolder.get();
        if (client != null) {
            client.close();
            clientHolder.remove();
        }
    }

    static Signature getRecordedSignature(String selector) {
        return recordedSignatures.get(selector);
    }

    /**
     * Resolves the Playwright selector string from {@link Locator#toString()}.
     * <p>Supported formats:</p>
     * <ul>
     *   <li>{@code locator("[data-testid=btn]")} — debug / some bindings</li>
     *   <li>{@code Locator@[data-testid='btn']} — Playwright Java default ({@code Locator@} + selector)</li>
     * </ul>
     * Unrecognized forms (e.g. {@code Locator@6f89}) are returned as-is; use
     * {@link WithFrapOptions#selector(String)} or {@link #withFrap(Page, String, WithFrapOptions)}.
     */
    static String extractSelector(Locator locator) {
        String s = locator.toString();

        java.util.regex.Matcher locatorCall = java.util.regex.Pattern
            .compile("locator\\(['\"](.+)['\"]\\)")
            .matcher(s);
        if (locatorCall.find()) {
            return locatorCall.group(1);
        }

        if (s.startsWith("Locator@")) {
            return s.substring("Locator@".length());
        }

        return s;
    }

    static String resolveSelector(Locator locator, WithFrapOptions options) {
        if (options != null && options.selector() != null && !options.selector().isBlank()) {
            return options.selector();
        }
        return extractSelector(locator);
    }

    static Signature constructSignatureFromElement(DOMElementInfo element) {
        List<DOMToken> path = new ArrayList<>();
        for (String pathPart : element.path()) {
            String[] parts = pathPart.split(":", 2);
            String tag = parts[0];
            String role = parts.length > 1 && !"-".equals(parts[1]) ? parts[1] : null;
            path.add(new DOMToken(tag, role, path.size()));
        }

        String prefix = path.stream()
            .limit(5)
            .map(t -> t.tag() + ":" + (t.role() != null ? t.role() : "-"))
            .reduce((a, b) -> a + ">" + b)
            .orElse("");

        Map<String, String> stableAttrs = new HashMap<>();
        if (element.attributes().containsKey("data-testid")) {
            stableAttrs.put("data-testid", element.attributes().get("data-testid"));
        }
        if (element.attributes().containsKey("id")) {
            stableAttrs.put("id", element.attributes().get("id"));
        }
        if (element.attributes().containsKey("data-id")) {
            stableAttrs.put("data-id", element.attributes().get("data-id"));
        }

        return new Signature(
            path,
            prefix,
            stableAttrs,
            element.textContent(),
            element.positionInParent(),
            0L,
            path.size()
        );
    }

    static Signature constructSignatureFromSelector(String selector, DOMSnapshot snapshot) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[data-testid=[\"']([^\"']+)[\"']\\]");
        java.util.regex.Matcher matcher = pattern.matcher(selector);

        java.util.regex.Pattern attrIdPattern = java.util.regex.Pattern.compile("\\[id=[\"']([^\"']+)[\"']\\]");
        java.util.regex.Matcher attrIdMatcher = attrIdPattern.matcher(selector);
        if (attrIdMatcher.find()) {
            String idValue = attrIdMatcher.group(1);
            for (DOMElementInfo el : snapshot.elements()) {
                String elId = el.attributes().get("id");
                String elDataId = el.attributes().get("data-id");
                if (idValue.equals(elId) || idValue.equals(elDataId)) {
                    Signature sig = constructSignatureFromElement(el);
                    return new Signature(
                        sig.path(),
                        sig.prefix(),
                        Map.of("id", idValue),
                        sig.textContent(),
                        el.positionInParent(),
                        sig.childrenHash(),
                        sig.depth()
                    );
                }
            }
            return new Signature(
                List.of(new DOMToken("li", null, 0)),
                "li:-",
                Map.of("id", idValue),
                null,
                null,
                0L,
                1
            );
        }

        if (matcher.find()) {
            String originalTestId = matcher.group(1);
            String[] originalParts = originalTestId.toLowerCase().split("[-_]");

            for (DOMElementInfo el : snapshot.elements()) {
                String elTestId = el.attributes().get("data-testid");
                if (elTestId == null) continue;

                String[] elParts = elTestId.toLowerCase().split("[-_]");
                for (String part : originalParts) {
                    if (part.length() <= 2) continue;
                    for (String elPart : elParts) {
                        if (elPart.contains(part) || part.contains(elPart)) {
                            return constructSignatureFromElement(el);
                        }
                    }
                }
            }
        }

        for (DOMElementInfo el : snapshot.elements()) {
            if (List.of("button", "input", "a", "select").contains(el.tag())) {
                return constructSignatureFromElement(el);
            }
        }

        return new Signature(
            List.of(new DOMToken("div", 0)),
            "div:-",
            Map.of(),
            null,
            null,
            0L,
            1
        );
    }

    private static void preRecordSignature(Page page, Locator locator, String selector) {
        if (recordedSignatures.containsKey(selector)) {
            return;
        }
        try {
            SnapshotBuilder builder = new SnapshotBuilder(page);
            if (builder.exists(locator)) {
                DOMElementInfo elementInfo = builder.extractForLocator(locator, selector);
                if (elementInfo != null) {
                    recordedSignatures.put(selector, constructSignatureFromElement(elementInfo));
                    logger.debug("[frap] Pre-recorded signature for {}", selector);
                }
            }
        } catch (Exception e) {
            logger.debug("[frap] Failed to pre-record signature for {}: {}", selector, e.getMessage());
        }
    }

    private static FrapCoreClient getOrCreateClient() {
        FrapCoreClient client = clientHolder.get();
        if (client == null) {
            try {
                client = FrapRpcClient.create();
                clientHolder.set(client);
                logger.debug("[frap] Created FrapRpcClient (pid: {})", client.pid());
            } catch (IOException e) {
                throw new RuntimeException("Failed to create FrapRpcClient", e);
            }
        }
        return client;
    }

    private static String generateTestName() {
        return "test-" + System.currentTimeMillis();
    }
}
