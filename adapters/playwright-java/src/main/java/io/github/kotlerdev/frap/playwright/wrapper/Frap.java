package io.github.kotlerdev.frap.playwright.wrapper;

import com.microsoft.playwright.*;
import io.github.kotlerdev.frap.core.config.FrapConfig;
import io.github.kotlerdev.frap.core.context.*;
import io.github.kotlerdev.frap.core.dto.*;
import io.github.kotlerdev.frap.core.client.FrapCoreClient;
import io.github.kotlerdev.frap.core.semantics.*;
import io.github.kotlerdev.frap.playwright.config.WithFrapOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main entry point for Frap Playwright integration.
 * Provides {@code withFrap()} method to wrap locators with healing capabilities.
 *
 * <p>Mirrors TypeScript {@code withFrap} from wrapper.ts.</p>
 */
public class Frap {
    private static final Logger logger = LoggerFactory.getLogger(Frap.class);

    // Thread-local storage of pre-recorded signatures
    private static final Map<String, Signature> recordedSignatures = new ConcurrentHashMap<>();

    // Thread-local storage of FrapCoreClient per thread
    private static final ThreadLocal<FrapCoreClient> clientHolder = new ThreadLocal<>();

    private Frap() {}

    /**
     * Wraps a Playwright locator with Frap healing capabilities.
     *
     * @param locator the original Playwright locator
     * @param page the page instance (used for DOM snapshots)
     * @param options Frap options
     * @return wrapped locator with healing
     */
    public static FrapLocator withFrap(Locator locator, Page page, WithFrapOptions options) {
        FrapConfig config = options != null ? options.toFrapConfig() : FrapConfig.defaults();
        String testName = options != null && options.testContext() != null
            ? options.testContext().getUniqueId()
            : generateTestName();

        String selector = extractSelector(locator);
        logger.debug("[frap] Wrapping locator: {}", selector);

        // Pre-record signature if element exists
        preRecordSignature(page, selector);

        // Get or create client
        FrapCoreClient client = getOrCreateClient();

        // Create wrapped locator
        FrapLocator.FrapData frapData = new FrapLocator.FrapData(
            locator, config, testName, selector, client
        );

        return new FrapLocatorImpl(locator, page, frapData);
    }

    /**
     * Overload with default options.
     */
    public static FrapLocator withFrap(Locator locator, Page page) {
        return withFrap(locator, page, new WithFrapOptions());
    }

    /**
     * Gets the last heal result for a wrapped locator.
     *
     * @param locator must be a FrapLocator returned by withFrap()
     * @return last heal result, or null if not a FrapLocator or no healing occurred
     */
    public static HealResult getLastHealResult(Locator locator) {
        if (locator instanceof FrapLocator fl) {
            return fl.lastHealResult();
        }
        return null;
    }

    /**
     * Returns true if the locator was healed in the last action.
     */
    public static boolean isHealed(Locator locator) {
        HealResult result = getLastHealResult(locator);
        return result != null && result.healed();
    }

    /**
     * Clears all recorded signatures.
     */
    public static void clearSignatures() {
        recordedSignatures.clear();
    }

    /**
     * Clears the thread-local client (useful for testing).
     */
    public static void clearClient() {
        FrapCoreClient client = clientHolder.get();
        if (client != null) {
            client.close();
            clientHolder.remove();
        }
    }

    private static void preRecordSignature(Page page, String selector) {
        if (recordedSignatures.containsKey(selector)) {
            return;
        }

        try {
            SnapshotBuilder builder = new SnapshotBuilder(page);
            if (builder.exists(selector)) {
                DOMElementInfo elementInfo = builder.extractForSelector(selector);
                if (elementInfo != null) {
                    Signature sig = constructSignatureFromElement(elementInfo);
                    recordedSignatures.put(selector, sig);
                    logger.debug("[frap] Pre-recorded signature for {}", selector);
                }
            }
        } catch (Exception e) {
            // Ignore errors during pre-recording
            logger.debug("[frap] Failed to pre-record signature for {}: {}", selector, e.getMessage());
        }
    }

    private static FrapCoreClient getOrCreateClient() {
        FrapCoreClient client = clientHolder.get();
        if (client == null) {
            try {
                client = FrapCoreClient.create();
                clientHolder.set(client);
                logger.debug("[frap] Created FrapCoreClient (pid: {})", client.pid());
            } catch (IOException e) {
                throw new RuntimeException("Failed to create FrapCoreClient", e);
            }
        }
        return client;
    }

    static String extractSelector(Locator locator) {
        String s = locator.toString();
        // Extract from "locator('...')" string
        int start = s.indexOf("'");
        int end = s.lastIndexOf("'");
        if (start > 0 && end > start) {
            return s.substring(start + 1, end);
        }
        return s;
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

        return new Signature(
            path,
            prefix,
            new HashMap<>(element.attributes()),
            element.textContent(),
            null,
            0L,
            path.size()
        );
    }

    static Signature constructSignatureFromSelector(String selector, DOMSnapshot snapshot) {
        // Try to find similar elements by data-testid pattern
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[data-testid=[\"']([^\"']+)[\"']\\]");
        java.util.regex.Matcher matcher = pattern.matcher(selector);

        if (matcher.find()) {
            String originalTestId = matcher.group(1);
            String[] originalParts = originalTestId.toLowerCase().split("[-_]");

            // Find similar elements
            for (DOMElementInfo el : snapshot.elements()) {
                String elTestId = el.attributes().get("data-testid");
                if (elTestId == null) continue;

                String[] elParts = elTestId.toLowerCase().split("[-_]");

                // Check for word overlap
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

        // Fallback: use first interactive element
        for (DOMElementInfo el : snapshot.elements()) {
            if (List.of("button", "input", "a", "select").contains(el.tag())) {
                return constructSignatureFromElement(el);
            }
        }

        // Last resort: minimal signature
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

    static Signature getRecordedSignature(String selector) {
        return recordedSignatures.get(selector);
    }

    private static String generateTestName() {
        return "test-" + System.currentTimeMillis();
    }

    /**
     * Implementation of FrapLocator that wraps Playwright's Locator.
     */
    private static class FrapLocatorImpl implements FrapLocator {
        private final Locator delegate;
        private final Page page;
        private final FrapData frapData;
        private final SnapshotBuilder snapshotBuilder;

        FrapLocatorImpl(Locator delegate, Page page, FrapData frapData) {
            this.delegate = delegate;
            this.page = page;
            this.frapData = frapData;
            this.snapshotBuilder = new SnapshotBuilder(page);
        }

        @Override
        public Locator originalLocator() {
            return frapData.originalLocator;
        }

        @Override
        public FrapConfig config() {
            return frapData.config;
        }

        @Override
        public HealResult lastHealResult() {
            return frapData.lastHealResult;
        }

        @Override
        public String testName() {
            return frapData.testName;
        }

        @Override
        public String selector() {
            return frapData.selector;
        }

        @Override
        public Page page() {
            return page;
        }

        // === Action methods with healing ===

        @Override
        public void click() {
            performWithHealing("click", () -> { delegate.click(); return null; });
        }

        @Override
        public void click(ClickOptions options) {
            performWithHealing("click", () -> { delegate.click(options); return null; });
        }

        @Override
        public void fill(String value) {
            performWithHealing("fill", () -> { delegate.fill(value); return null; });
        }

        @Override
        public void check() {
            performWithHealing("check", () -> { delegate.check(); return null; });
        }

        @Override
        public void check(CheckOptions options) {
            performWithHealing("check", () -> { delegate.check(options); return null; });
        }

        @Override
        public void uncheck() {
            performWithHealing("uncheck", () -> { delegate.uncheck(); return null; });
        }

        @Override
        public void uncheck(UncheckOptions options) {
            performWithHealing("uncheck", () -> { delegate.uncheck(options); return null; });
        }

        @Override
        public void selectOption(String value) {
            performWithHealing("selectOption", () -> { delegate.selectOption(value); return null; });
        }

        @Override
        public void selectOption(String value, SelectOptionOptions options) {
            performWithHealing("selectOption", () -> { delegate.selectOption(value, options); return null; });
        }

        @Override
        public void press(String key) {
            performWithHealing("press", () -> { delegate.press(key); return null; });
        }

        @Override
        public void press(String key, PressOptions options) {
            performWithHealing("press", () -> { delegate.press(key, options); return null; });
        }

        @Override
        public void type(String text) {
            performWithHealing("type", () -> { delegate.type(text); return null; });
        }

        @Override
        public void type(String text, TypeOptions options) {
            performWithHealing("type", () -> { delegate.type(text, options); return null; });
        }

        private <T> T performWithHealing(String action, java.util.function.Supplier<T> actionFn) {
            String selector = frapData.selector;
            FrapConfig config = frapData.config;

            // Quick check: if element doesn't exist, attempt healing
            if (!snapshotBuilder.exists(selector)) {
                logger.info("[frap] Element not found (quick check): {}", selector);

                if (!config.enableHealing()) {
                    throw new PlaywrightException("Element not found: " + selector);
                }

                return attemptHealing(action, selector, config, actionFn);
            }

            // Element exists, try the action directly
            try {
                return actionFn.get();
            } catch (PlaywrightException e) {
                // Action failed, try healing
                if (config.enableHealing()) {
                    logger.info("[frap] Action failed, attempting healing: {}", e.getMessage());
                    return attemptHealing(action, selector, config, actionFn);
                }
                throw e;
            }
        }

        private <T> T attemptHealing(String action, String selector, FrapConfig config,
                                      java.util.function.Supplier<T> actionFn) {
            logger.info("[frap] Attempting healing...");

            DOMSnapshot snapshot = snapshotBuilder.build();
            logger.info("[frap] DOM snapshot built: {} elements", snapshot.elements().size());

            // Get original signature
            Signature originalSig = getRecordedSignature(selector);
            if (originalSig == null) {
                logger.debug("[frap] No pre-recorded signature, constructing from selector...");
                originalSig = constructSignatureFromSelector(selector, snapshot);
            }

            // Build heal request
            HealRequest request = new HealRequest(
                selector,
                originalSig,
                snapshot,
                config.minConfidence()
            );

            try {
                HealResult result = frapData.client.heal(request);

                // Store result
                frapData.lastHealResult = result;

                // Build semantics
                HealingSemantics semantics = HealingSemantics.classify(
                    HealTrigger.SELECTOR_MISSING,
                    HealPolicy.valueOf(config.healPolicy().name().toUpperCase()),
                    result.healed(),
                    true
                );

                logger.info("[frap] Healing result: healed={}, confidence={:.2f}, selector=\"{}\"",
                    result.healed(), result.confidence(), result.selector());

                if (result.healed() && result.topCandidates() != null) {
                    logger.debug("[frap] Top candidates: {}", result.topCandidates().size());
                    for (int i = 0; i < result.topCandidates().size(); i++) {
                        Candidate c = result.topCandidates().get(i);
                        logger.debug("[frap]   Candidate {}: \"{}\" confidence={:.2f}",
                            i, c.selector(), c.confidence());
                    }
                }

                if (result.healed()) {
                    // Retry with healed selector
                    Locator healedLocator = page.locator(result.selector());
                    java.lang.reflect.Method method = findMethod(healedLocator, action);
                    if (method != null) {
                        try {
                            @SuppressWarnings("unchecked")
                            T result2 = (T) method.invoke(healedLocator);
                            return result2;
                        } catch (Exception e) {
                            throw new PlaywrightException("Failed to invoke healed action", e);
                        }
                    }
                }

                // Healing failed or no healed selector
                throw new PlaywrightException(
                    String.format("Healing failed for %s (confidence=%.2f)", selector, result.confidence())
                );

            } catch (IOException e) {
                throw new PlaywrightException("Failed to heal: " + e.getMessage(), e);
            }
        }

        private java.lang.reflect.Method findMethod(Locator locator, String action) {
            // Map action names to method names
            String methodName = action;
            try {
                return locator.getClass().getMethod(methodName);
            } catch (NoSuchMethodException e) {
                return null;
            }
        }

        // === Delegate all other methods ===

        @Override
        public String toString() {
            return "FrapLocator[" + delegate.toString() + "]";
        }

        // Implement remaining Locator methods by delegation
        @Override public Locator filter(FilterOptions options) { return delegate.filter(options); }
        @Override public Locator first() { return delegate.first(); }
        @Override public Locator last() { return delegate.last(); }
        @Override public Locator nth(int index) { return delegate.nth(index); }
        @Override public Locator or(Locator locator) { return delegate.or(locator); }
        @Override public Locator and(Locator locator) { return delegate.and(locator); }
        @Override public Locator locator(String selector) { return delegate.locator(selector); }
        @Override public Locator locator(Locator locator) { return delegate.locator(locator); }
        @Override public Locator getByAltText(String text) { return delegate.getByAltText(text); }
        @Override public Locator getByAltText(String text, GetByAltTextOptions options) { return delegate.getByAltText(text, options); }
        @Override public Locator getByLabel(String text) { return delegate.getByLabel(text); }
        @Override public Locator getByLabel(String text, GetByLabelOptions options) { return delegate.getByLabel(text, options); }
        @Override public Locator getByPlaceholder(String text) { return delegate.getByPlaceholder(text); }
        @Override public Locator getByPlaceholder(String text, GetByPlaceholderOptions options) { return delegate.getByPlaceholder(text, options); }
        @Override public Locator getByRole(AriaRole role) { return delegate.getByRole(role); }
        @Override public Locator getByRole(AriaRole role, GetByRoleOptions options) { return delegate.getByRole(role, options); }
        @Override public Locator getByTestId(String testId) { return delegate.getByTestId(testId); }
        @Override public Locator getByText(String text) { return delegate.getByText(text); }
        @Override public Locator getByText(String text, GetByTextOptions options) { return delegate.getByText(text, options); }
        @Override public Locator getByTitle(String text) { return delegate.getByTitle(text); }
        @Override public Locator getByTitle(String text, GetByTitleOptions options) { return delegate.getByTitle(text, options); }

        @Override public List<String> allInnerTexts() { return delegate.allInnerTexts(); }
        @Override public List<String> allTextContents() { return delegate.allTextContents(); }
        @Override public BoundingBox boundingBox() { return delegate.boundingBox(); }
        @Override public BoundingBox boundingBox(BoundingBoxOptions options) { return delegate.boundingBox(options); }
        @Override public void blur() { delegate.blur(); }
        @Override public void blur(BlurOptions options) { delegate.blur(options); }
        @Override public ElementHandle elementHandle() { return delegate.elementHandle(); }
        @Override public ElementHandle elementHandle(ElementHandleOptions options) { return delegate.elementHandle(options); }
        @Override public List<ElementHandle> elementHandles() { return delegate.elementHandles(); }
        @Override public Object evaluate(String expression, Object arg) { return delegate.evaluate(expression, arg); }
        @Override public Object evaluateHandle(String expression, Object arg) { return delegate.evaluateHandle(expression, arg); }
        @Override public void focus() { delegate.focus(); }
        @Override public void focus(FocusOptions options) { delegate.focus(options); }
        @Override public FrameLocator contentFrame() { return delegate.contentFrame(); }
        @Override public void dblclick() { delegate.dblclick(); }
        @Override public void dblclick(DblclickOptions options) { delegate.dblclick(options); }
        @Override public void dragTo(Locator target) { delegate.dragTo(target); }
        @Override public void dragTo(Locator target, DragToOptions options) { delegate.dragTo(target, options); }
        @Override public void highlight() { delegate.highlight(); }
        @Override public void hover() { delegate.hover(); }
        @Override public void hover(HoverOptions options) { delegate.hover(options); }
        @Override public String innerHTML() { return delegate.innerHTML(); }
        @Override public String innerHTML(InnerHTMLOptions options) { return delegate.innerHTML(options); }
        @Override public String innerText() { return delegate.innerText(); }
        @Override public String innerText(InnerTextOptions options) { return delegate.innerText(options); }
        @Override public String inputValue() { return delegate.inputValue(); }
        @Override public String inputValue(InputValueOptions options) { return delegate.inputValue(options); }
        @Override public boolean isChecked() { return delegate.isChecked(); }
        @Override public boolean isChecked(IsCheckedOptions options) { return delegate.isChecked(options); }
        @Override public boolean isDisabled() { return delegate.isDisabled(); }
        @Override public boolean isDisabled(IsDisabledOptions options) { return delegate.isDisabled(options); }
        @Override public boolean isEditable() { return delegate.isEditable(); }
        @Override public boolean isEditable(IsEditableOptions options) { return delegate.isEditable(options); }
        @Override public boolean isEnabled() { return delegate.isEnabled(); }
        @Override public boolean isEnabled(IsEnabledOptions options) { return delegate.isEnabled(options); }
        @Override public boolean isHidden() { return delegate.isHidden(); }
        @Override public boolean isHidden(IsHiddenOptions options) { return delegate.isHidden(options); }
        @Override public boolean isVisible() { return delegate.isVisible(); }
        @Override public boolean isVisible(IsVisibleOptions options) { return delegate.isVisible(options); }
        @Override public String textContent() { return delegate.textContent(); }
        @Override public String textContent(TextContentOptions options) { return delegate.textContent(options); }
        @Override public void setChecked(boolean checked) { delegate.setChecked(checked); }
        @Override public void setChecked(boolean checked, SetCheckedOptions options) { delegate.setChecked(checked, options); }
        @Override public void setInputFiles(Path files) { delegate.setInputFiles(files); }
        @Override public void setInputFiles(Path[] files) { delegate.setInputFiles(files); }
        @Override public void setInputFiles(Path files, SetInputFilesOptions options) { delegate.setInputFiles(files, options); }
        @Override public void setInputFiles(Path[] files, SetInputFilesOptions options) { delegate.setInputFiles(files, options); }
        @Override public void setInputFiles(FilePayload files) { delegate.setInputFiles(files); }
        @Override public void setInputFiles(FilePayload[] files) { delegate.setInputFiles(files); }
        @Override public void setInputFiles(FilePayload files, SetInputFilesOptions options) { delegate.setInputFiles(files, options); }
        @Override public void setInputFiles(FilePayload[] files, SetInputFilesOptions options) { delegate.setInputFiles(files, options); }
        @Override public void tap() { delegate.tap(); }
        @Override public void tap(TapOptions options) { delegate.tap(options); }
        @Override public void scrollIntoViewIfNeeded() { delegate.scrollIntoViewIfNeeded(); }
        @Override public void scrollIntoViewIfNeeded(ScrollIntoViewIfNeededOptions options) { delegate.scrollIntoViewIfNeeded(options); }
        @Override public void screenshot() { delegate.screenshot(); }
        @Override public void screenshot(Path path) { delegate.screenshot(path); }
        @Override public byte[] screenshot(ScreenshotOptions options) { return delegate.screenshot(options); }
        @Override public List<String> selectOption(SelectOption[] value) { return delegate.selectOption(value); }
        @Override public List<String> selectOption(String value) { return delegate.selectOption(value); }
        @Override public List<String> selectOption(String[] value) { return delegate.selectOption(value); }
        @Override public List<String> selectOption(ElementHandle[] value) { return delegate.selectOption(value); }
        @Override public List<String> selectOption(SelectOption value) { return delegate.selectOption(value); }
        @Override public List<String> selectOption(SelectOption[] value, SelectOptionOptions options) { return delegate.selectOption(value, options); }
        @Override public List<String> selectOption(String value, SelectOptionOptions options) { return delegate.selectOption(value, options); }
        @Override public List<String> selectOption(String[] value, SelectOptionOptions options) { return delegate.selectOption(value, options); }
        @Override public List<String> selectOption(ElementHandle[] value, SelectOptionOptions options) { return delegate.selectOption(value, options); }
        @Override public List<String> selectOption(SelectOption value, SelectOptionOptions options) { return delegate.selectOption(value, options); }
        @Override public void waitFor() { delegate.waitFor(); }
        @Override public void waitFor(WaitForOptions options) { delegate.waitFor(options); }
        @Override public JSHandle evaluateHandle(String expression) { return delegate.evaluateHandle(expression); }
        @Override public Object evaluate(String expression) { return delegate.evaluate(expression); }
        @Override public Locator that(FilterOptions.HasTextOptions hasTextOptions) { return delegate.that(hasTextOptions); }
        @Override public Locator that(FilterOptions.HasNotTextOptions hasNotTextOptions) { return delegate.that(hasNotTextOptions); }
    }
}
