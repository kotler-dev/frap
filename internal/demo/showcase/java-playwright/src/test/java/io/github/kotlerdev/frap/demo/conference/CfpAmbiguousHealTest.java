package io.github.kotlerdev.frap.demo.conference;

import com.microsoft.playwright.*;
import io.github.kotlerdev.frap.core.dto.HealResult;
import io.github.kotlerdev.frap.demo.helpers.ConferencePaths;
import io.github.kotlerdev.frap.playwright.extension.FrapExtension;
import io.github.kotlerdev.frap.playwright.wrapper.Frap;
import io.github.kotlerdev.frap.playwright.wrapper.FrapLocator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CFP ambiguous submit buttons — healing refused when top candidates are too close (CONF-SH-CFP-FAIL).
 * Mirrors TypeScript cfp.spec.ts.
 */
@Tag("e2e")
@ExtendWith(FrapExtension.class)
@DisplayName("Conference 2026 Spring - CFP")
class CfpAmbiguousHealTest {

    static Playwright playwright;
    static Browser browser;
    Page page;

    @BeforeAll
    static void beforeAll() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
            new BrowserType.LaunchOptions().setHeadless(true)
        );
    }

    @AfterAll
    static void afterAll() {
        browser.close();
        playwright.close();
        Frap.clearClient();
    }

    @BeforeEach
    void beforeEach() {
        page = browser.newPage();
    }

    @AfterEach
    void afterEach() {
        page.close();
    }

    @Test
    @DisplayName("CONF-SH-CFP-FAIL: ambiguous submit buttons refuse heal")
    void testAmbiguousSubmitRefusesHeal() {
        page.navigate(ConferencePaths.BASE_URL + ConferencePaths.Conf.CFP);

        FrapLocator submit = Frap.withFrap(
            page.locator("[data-testid='cfp-submit-missing']"),
            page,
            ConferencePaths.confFrap()
                .minConfidence(0.85)
                .debug(true)
        );

        assertThatThrownBy(submit::click).isInstanceOf(PlaywrightException.class);

        HealResult result = Frap.getLastHealResult(submit);
        assertThat(result).isNotNull();
        assertThat(result.healed()).isFalse();
        assertThat(result.topCandidates()).hasSizeGreaterThanOrEqualTo(2);
    }
}
