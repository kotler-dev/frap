package io.github.kotlerdev.frap.demo.conference;

import com.microsoft.playwright.*;
import io.github.kotlerdev.frap.demo.helpers.ConferencePaths;
import io.github.kotlerdev.frap.core.dto.HealResult;
import io.github.kotlerdev.frap.playwright.extension.FrapExtension;
import io.github.kotlerdev.frap.playwright.wrapper.Frap;
import io.github.kotlerdev.frap.playwright.wrapper.FrapLocator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Conference Schedule self-healing tests (CP002 equivalent).
 * Mirrors TypeScript schedule.spec.ts.
 */
@Tag("e2e")
@ExtendWith(FrapExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Conference 2026 Spring - Schedule")
class ScheduleHealingTest {

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
    @Order(1)
    @DisplayName("CONF-SH-SCHED-PASS: opens talk after testid refactor")
    void testOpensTalkAfterRefactor() {
        page.navigate(ConferencePaths.BASE_URL + ConferencePaths.Conf.SCHEDULE_HEAL);

        FrapLocator openLink = Frap.withFrap(
            page.locator("[data-testid='talk-open-healing']"),
            page,
            ConferencePaths.confFrap()
                .minConfidence(0.7)
                .healPolicy("expect_heal")
                .debug(true)
        );

        openLink.click();

        assertThat(page.url()).contains("talk.html?id=healing");

        HealResult result = Frap.getLastHealResult(openLink);
        assertThat(result).isNotNull();
        assertThat(result.healed()).isTrue();
        assertThat(result.confidence()).isGreaterThanOrEqualTo(0.7);
    }

    @Test
    @Order(2)
    @DisplayName("CONF-POL-SCHED-WARN: unexpected heal when policy is deny")
    void testUnexpectedHealWithDenyPolicy() {
        page.navigate(ConferencePaths.BASE_URL + ConferencePaths.Conf.SCHEDULE_HEAL);

        FrapLocator openLink = Frap.withFrap(
            page.locator("[data-testid='talk-open-healing']"),
            page,
            ConferencePaths.confFrap()
                .minConfidence(0.7)
                .healPolicy("deny")
                .debug(true)
        );

        openLink.click();
        assertThat(page.url()).contains("talk.html?id=healing");

        HealResult result = Frap.getLastHealResult(openLink);
        assertThat(result).isNotNull();
        assertThat(result.healed()).isTrue();
        assertThat(result.semantics()).isNotNull();
        assertThat(result.semantics().outcome().name()).isEqualTo("UNEXPECTED_HEAL");
    }

    @Test
    @Order(3)
    @DisplayName("CONF-SH-V1: schedule v1 stable testid opens talk")
    void testScheduleV1OpensTalk() {
        page.navigate(ConferencePaths.BASE_URL + ConferencePaths.Conf.SCHEDULE_V1);

        FrapLocator openLink = Frap.withFrap(
            page.locator("[data-testid='talk-open-opening']"),
            page,
            ConferencePaths.confFrap()
        );

        openLink.click();
        assertThat(page.url()).contains("talk.html?id=opening");
    }

    @Test
    @Order(4)
    @DisplayName("CONF-SH-NAV: index links use stable hrefs")
    void testIndexNavigationLinks() {
        page.navigate(ConferencePaths.BASE_URL + ConferencePaths.Conf.INDEX);

        Frap.withFrap(page.locator("a[href='register.html']").first(), page, ConferencePaths.confFrap()).click();
        assertThat(page.url()).contains("register.html");

        page.navigate(ConferencePaths.BASE_URL + ConferencePaths.Conf.INDEX);
        Frap.withFrap(page.locator("a[href='cfp.html']").first(), page, ConferencePaths.confFrap()).click();
        assertThat(page.url()).contains("cfp.html");

        page.navigate(ConferencePaths.BASE_URL + ConferencePaths.Conf.INDEX);
        Frap.withFrap(page.locator("a[href='speakers.html']").first(), page, ConferencePaths.confFrap()).click();
        assertThat(page.url()).contains("speakers.html");
    }
}
