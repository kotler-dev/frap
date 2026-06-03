package io.github.kotlerdev.frap.playwright.wrapper;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.github.kotlerdev.frap.core.dto.DOMElementInfo;
import io.github.kotlerdev.frap.core.dto.DOMSnapshot;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Issue #14: snapshot text must not include CSS from nested {@code <style>} (uses innerText).
 */
class SnapshotBuilderVisibleTextTest {

    private static Playwright playwright;
    private static Browser browser;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @Test
    void snapshot_uses_visible_text_not_style_content() {
        try (Page page = browser.newPage()) {
            page.setContent("""
                <button id="icon-btn" type="button">
                  <svg><style>.x{clip-path:url(#c)}</style></svg>
                </button>
                """);
            DOMSnapshot snapshot = new SnapshotBuilder(page).build();
            assertThat(snapshot.elements()).isNotEmpty();
            DOMElementInfo button = snapshot.elements().stream()
                .filter(e -> "button".equals(e.tag()))
                .findFirst()
                .orElseThrow();
            String text = button.textContent();
            if (text != null) {
                assertThat(text).doesNotContain("clip-path");
                assertThat(text).doesNotContain("{");
            }
        }
    }
}
