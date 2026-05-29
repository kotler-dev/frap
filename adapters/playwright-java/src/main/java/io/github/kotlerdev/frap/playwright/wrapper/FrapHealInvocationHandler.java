package io.github.kotlerdev.frap.playwright.wrapper;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import io.github.kotlerdev.frap.core.config.FrapConfig;
import io.github.kotlerdev.frap.core.dto.*;
import io.github.kotlerdev.frap.core.events.HealingEvent;
import io.github.kotlerdev.frap.core.semantics.*;
import io.github.kotlerdev.frap.playwright.reports.ReportGenerator;
import io.github.kotlerdev.frap.playwright.reports.debug.DebugReportBuilder;
import io.github.kotlerdev.frap.playwright.reports.debug.DebugReportWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Set;

/**
 * JDK proxy handler: intercepts healable Locator actions, delegates everything else.
 * Mirrors TypeScript Proxy in wrapper.ts.
 */
final class FrapHealInvocationHandler implements InvocationHandler {
    private static final Logger logger = LoggerFactory.getLogger(FrapHealInvocationHandler.class);

    private static final Set<String> HEALABLE_METHODS = Set.of(
        "click", "fill", "check", "uncheck", "selectOption", "press", "type", "tap"
    );

    private final Locator delegate;
    private final Page page;
    private final FrapLocator.FrapData frapData;
    private final SnapshotBuilder snapshotBuilder;

    FrapHealInvocationHandler(Locator delegate, Page page, FrapLocator.FrapData frapData) {
        this.delegate = delegate;
        this.page = page;
        this.frapData = frapData;
        this.snapshotBuilder = new SnapshotBuilder(page);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();

        if (method.getDeclaringClass() == Object.class) {
            if ("toString".equals(name)) {
                return "FrapLocator[" + delegate + "]";
            }
            return method.invoke(delegate, args);
        }

        if (method.getDeclaringClass() == FrapLocator.class) {
            return switch (name) {
                case "originalLocator" -> frapData.originalLocator;
                case "config" -> frapData.config;
                case "lastHealResult" -> frapData.lastHealResult;
                case "testName" -> frapData.testName;
                case "selector" -> frapData.selector;
                case "isHealed" -> {
                    HealResult r = frapData.lastHealResult;
                    yield r != null && r.healed();
                }
                case "confidence" -> {
                    HealResult r = frapData.lastHealResult;
                    yield r != null ? r.confidence() : 1.0;
                }
                default -> method.invoke(frapData, args);
            };
        }

        if (HEALABLE_METHODS.contains(name)) {
            try {
                return performWithHealing(name, args, () -> method.invoke(delegate, args));
            } catch (PlaywrightException e) {
                throw e;
            } catch (Throwable t) {
                throw wrapThrowable(t);
            }
        }

        try {
            return method.invoke(delegate, args);
        } catch (Throwable t) {
            throw wrapThrowable(t);
        }
    }

    private static RuntimeException wrapThrowable(Throwable t) {
        if (t instanceof RuntimeException re) {
            return re;
        }
        if (t instanceof Error err) {
            throw err;
        }
        return new PlaywrightException(t.getMessage(), t);
    }

    private boolean elementExists() {
        try {
            return delegate.count() > 0;
        } catch (PlaywrightException e) {
            return false;
        }
    }

    private Object performWithHealing(String action, Object[] invokeArgs, ThrowingSupplier actionFn) throws Throwable {
        String selector = frapData.selector;
        FrapConfig config = frapData.config;

        if (!elementExists()) {
            logger.info("[frap] Element not found (quick check): {}", selector);
            if (!config.enableHealing()) {
                throw new PlaywrightException("Element not found: " + selector);
            }
            return attemptHealing(action, selector, config, invokeArgs, actionFn);
        }

        try {
            return actionFn.get();
        } catch (PlaywrightException e) {
            if (config.enableHealing()) {
                logger.info("[frap] Action failed, attempting healing: {}", e.getMessage());
                return attemptHealing(action, selector, config, invokeArgs, actionFn);
            }
            throw e;
        }
    }

    private void writeDebugReportIfEnabled(DOMSnapshot snapshot, HealResult result,
                                           HealingSemantics semantics) {
        if (!frapData.config.isDebugEnabled()) {
            return;
        }
        try {
            Path reportDir = Paths.get(frapData.config.reportDir());
            var report = DebugReportBuilder.fromHealResult(
                frapData.testName,
                result,
                snapshot,
                semantics,
                0
            );
            DebugReportWriter.write(reportDir, report);
        } catch (IOException e) {
            logger.warn("[frap] Failed to write debug report: {}", e.getMessage());
        }
    }

    private void recordHealingEvent(HealResult result, HealTrigger trigger, boolean attempted) {
        if (!frapData.config.enableReporting() || !attempted) {
            return;
        }
        try {
            ReportGenerator generator = new ReportGenerator(Path.of(frapData.config.reportDir()));
            generator.recordHealingEvent(new HealingEvent(
                Instant.now().toString(),
                frapData.testName,
                frapData.testName,
                frapData.selector,
                result.healed() ? result.selector() : null,
                result.healed(),
                result.confidence(),
                trigger,
                frapData.config.healPolicy()
            ));
        } catch (IOException e) {
            logger.warn("[frap] Failed to record healing event: {}", e.getMessage());
        }
    }

    private Object attemptHealing(String action, String selector, FrapConfig config, Object[] invokeArgs,
                                  ThrowingSupplier actionFn) throws Throwable {
        logger.info("[frap] Attempting healing...");

        DOMSnapshot snapshot = snapshotBuilder.build();
        logger.info("[frap] DOM snapshot built: {} elements", snapshot.elements().size());

        Signature originalSig = Frap.getRecordedSignature(selector);
        if (originalSig == null) {
            logger.debug("[frap] No pre-recorded signature, constructing from selector...");
            originalSig = Frap.constructSignatureFromSelector(selector, snapshot);
        }

        HealRequest request = new HealRequest(
            selector,
            originalSig,
            snapshot,
            config.minConfidence()
        );

        try {
            HealResult result = frapData.client.heal(request);
            HealTrigger trigger = HealTrigger.SELECTOR_MISSING;
            HealingSemantics semantics = HealingSemantics.classify(
                trigger, config.healPolicy(), result.healed(), true);
            result = new HealResult(
                result.healed(),
                result.selector(),
                result.confidence(),
                result.diff(),
                result.topCandidates(),
                result.originalSignature(),
                semantics
            );
            frapData.lastHealResult = result;
            recordHealingEvent(result, trigger, true);
            writeDebugReportIfEnabled(snapshot, result, semantics);

            logger.info("[frap] Healing result: healed={}, confidence={}, selector=\"{}\"",
                result.healed(), result.confidence(), result.selector());

            if (result.healed() && result.selector() != null && !result.selector().isBlank()) {
                Locator healedLocator = page.locator(result.selector());
                if (healedLocator.count() == 0) {
                    throw new PlaywrightException("Healed selector not found: " + result.selector());
                }
                if (healedLocator.count() > 1) {
                    healedLocator = healedLocator.first();
                }
                Method healedMethod = Locator.class.getMethod(action, parameterTypes(invokeArgs));
                return healedMethod.invoke(healedLocator, invokeArgs);
            }

            throw new PlaywrightException(
                String.format("Healing failed for %s (confidence=%.2f)", selector, result.confidence())
            );
        } catch (IOException e) {
            throw new PlaywrightException("Failed to heal: " + e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            throw new PlaywrightException("Failed to invoke healed action: " + action, e);
        }
    }

    private static Class<?>[] parameterTypes(Object[] args) {
        if (args == null || args.length == 0) {
            return new Class<?>[0];
        }
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i] != null ? args[i].getClass() : Object.class;
        }
        return types;
    }

    @FunctionalInterface
    private interface ThrowingSupplier {
        Object get() throws Throwable;
    }
}
