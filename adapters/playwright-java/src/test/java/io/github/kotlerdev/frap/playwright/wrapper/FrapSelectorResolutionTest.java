package io.github.kotlerdev.frap.playwright.wrapper;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.github.kotlerdev.frap.core.dto.DOMElementInfo;
import io.github.kotlerdev.frap.playwright.config.WithFrapOptions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class FrapSelectorResolutionTest {

    @Test
    void resolveSelectorUsesExplicitOptionWhenProvided() {
        Locator locator = locatorProxy("Locator@6f89");
        WithFrapOptions options = new WithFrapOptions().selector("[data-testid='pay-btn']");

        String selector = Frap.resolveSelector(locator, options);

        assertThat(selector).isEqualTo("[data-testid='pay-btn']");
    }

    @Test
    void extractSelectorParsesLocatorString() {
        Locator locator = locatorProxy("locator('[data-testid=\"pay-btn\"]')");

        String selector = Frap.extractSelector(locator);

        assertThat(selector).isEqualTo("[data-testid=\"pay-btn\"]");
    }

    @Test
    void extractSelectorParsesPlaywrightJavaToString() {
        Locator locator = locatorProxy("Locator@[data-testid='talk-open-healing']");

        String selector = Frap.extractSelector(locator);

        assertThat(selector).isEqualTo("[data-testid='talk-open-healing']");
    }

    @Test
    void snapshotBuilderSkipsLocatorObjectStyleSelector() {
        AtomicInteger evaluateCalls = new AtomicInteger();
        Page page = pageProxy(evaluateCalls);
        SnapshotBuilder builder = new SnapshotBuilder(page);

        boolean exists = builder.exists("Locator@123abc");
        DOMElementInfo extracted = builder.extractForSelector("Locator@123abc");

        assertThat(exists).isFalse();
        assertThat(extracted).isNull();
        assertThat(evaluateCalls.get()).isZero();
    }

    private static Locator locatorProxy(String asString) {
        return (Locator) Proxy.newProxyInstance(
            Locator.class.getClassLoader(),
            new Class<?>[] {Locator.class},
            (proxy, method, args) -> {
                if ("toString".equals(method.getName())) {
                    return asString;
                }
                throw new UnsupportedOperationException("Unexpected method: " + method.getName());
            }
        );
    }

    private static Page pageProxy(AtomicInteger evaluateCalls) {
        return (Page) Proxy.newProxyInstance(
            Page.class.getClassLoader(),
            new Class<?>[] {Page.class},
            (proxy, method, args) -> {
                if ("evaluate".equals(method.getName())) {
                    evaluateCalls.incrementAndGet();
                    return null;
                }
                throw new UnsupportedOperationException("Unexpected method: " + method.getName());
            }
        );
    }
}
