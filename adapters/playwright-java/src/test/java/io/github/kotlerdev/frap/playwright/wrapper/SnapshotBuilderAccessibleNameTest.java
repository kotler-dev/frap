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
 * Accessible name from label[for], aria-labelledby, and sibling labels (C013 / HTML-AAM).
 */
class SnapshotBuilderAccessibleNameTest {

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
    void label_for_sets_accessible_name_on_target() {
        try (Page page = browser.newPage()) {
            page.setContent("""
                <label for="pay-btn">Pay now</label>
                <button id="pay-btn" type="button"></button>
                """);
            DOMSnapshot snapshot = new SnapshotBuilder(page).build();
            DOMElementInfo button = snapshot.elements().stream()
                .filter(e -> "pay-btn".equals(e.attributes().get("id")))
                .findFirst()
                .orElseThrow();
            assertThat(button.accessibleName()).isEqualTo("Pay now");
        }
    }

    @Test
    void child_label_sets_accessible_name_on_wrapper() {
        try (Page page = browser.newPage()) {
            page.setContent("""
                <div id="calc-btn">
                  <div role="button"></div>
                  <label>Калькулятор процентов</label>
                </div>
                """);
            DOMSnapshot snapshot = new SnapshotBuilder(page).build();
            DOMElementInfo wrapper = snapshot.elements().stream()
                .filter(e -> "calc-btn".equals(e.attributes().get("id")))
                .findFirst()
                .orElseThrow();
            assertThat(wrapper.accessibleName()).isEqualTo("Калькулятор процентов");
        }
    }

    @Test
    void sibling_label_sets_accessible_name_on_role_button() {
        try (Page page = browser.newPage()) {
            page.setContent("""
                <div id="calc-btn">
                  <div role="button" tabindex="0"></div>
                  <label for="other">Калькулятор процентов</label>
                </div>
                """);
            DOMSnapshot snapshot = new SnapshotBuilder(page).build();
            DOMElementInfo roleButton = snapshot.elements().stream()
                .filter(e -> "button".equals(e.attributes().get("role")))
                .findFirst()
                .orElseThrow();
            assertThat(roleButton.accessibleName()).isEqualTo("Калькулятор процентов");
        }
    }
}
