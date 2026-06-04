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

/** Collector parity with {@code frap-mcp-tools/.../snapshot.js} (semantic pipeline). */
class SnapshotBuilderCollectorFieldsTest {

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
    void landmark_sets_scope_hint() {
        try (Page page = browser.newPage()) {
            page.setContent("""
                <nav id="main-nav" aria-label="Primary">
                  <a href="/home">Home</a>
                </nav>
                """);
            DOMSnapshot snapshot = new SnapshotBuilder(page).build();
            DOMElementInfo link = snapshot.elements().stream()
                .filter(e -> "a".equals(e.tag()))
                .findFirst()
                .orElseThrow();
            assertThat(link.scopeHint()).isEqualTo("nav#main-nav[aria-label=\"Primary\"]");
        }
    }

    @Test
    void button_sets_computed_role() {
        try (Page page = browser.newPage()) {
            page.setContent("<button type=\"submit\">Go</button>");
            DOMSnapshot snapshot = new SnapshotBuilder(page).build();
            DOMElementInfo button = snapshot.elements().stream()
                .filter(e -> "button".equals(e.tag()))
                .findFirst()
                .orElseThrow();
            assertThat(button.computedRole()).isEqualTo("button");
        }
    }

    @Test
    void hidden_element_is_not_visible() {
        try (Page page = browser.newPage()) {
            page.setContent("""
                <button id="shown">Shown</button>
                <button id="hidden" style="display:none">Hidden</button>
                """);
            DOMSnapshot snapshot = new SnapshotBuilder(page).build();
            DOMElementInfo hidden = snapshot.elements().stream()
                .filter(e -> "hidden".equals(e.attributes().get("id")))
                .findFirst()
                .orElseThrow();
            DOMElementInfo shown = snapshot.elements().stream()
                .filter(e -> "shown".equals(e.attributes().get("id")))
                .findFirst()
                .orElseThrow();
            assertThat(hidden.visible()).isFalse();
            assertThat(shown.visible()).isTrue();
        }
    }
}
