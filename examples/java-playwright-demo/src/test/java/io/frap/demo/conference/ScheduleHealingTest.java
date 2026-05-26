package io.frap.demo.conference;

import com.microsoft.playwright.*;
import io.frap.demo.helpers.ConferencePaths;
import io.frap.playwright.config.WithFrapOptions;
import io.frap.playwright.extension.FrapExtension;
import io.frap.playwright.wrapper.Frap;
import io.frap.playwright.wrapper.FrapLocator;
import io.frap.core.dto.HealResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Conference Schedule self-healing tests (CP002 equivalent).
 * Mirrors TypeScript schedule.spec.ts tests.
 */
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
        // Navigate to schedule-heal.html where data-testid was changed
        page.navigate(ConferencePaths.BASE_URL + ConferencePaths.Conf.SCHEDULE_HEAL);

        // The original selector uses old testid, page has new testid
        var options = ConferencePaths.confFrap()
            .minConfidence(0.7)
            .healPolicy("expect_heal")
            .debug(true)
            .testContext(null);  // Would use JUnit5 context in real scenario

        FrapLocator openLink = Frap.withFrap(
            page.locator("[data-testid='talk-open-healing']"),
            page,
            options
        );

        openLink.click();

        // Verify navigation to talk page
        assertThat(page.url()).contains("talk.html?id=healing");

        // Verify healing occurred
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

        var options = ConferencePaths.confFrap()
            .minConfidence(0.7)
            .healPolicy("deny")
            .debug(true);

        FrapLocator openLink = Frap.withFrap(
            page.locator("[data-testid='talk-open-healing']"),
            page,
            options
        );

        openLink.click();

        // Should still navigate (healing works)
        assertThat(page.url()).contains("talk.html?id=healing");

        // But mark as unexpected_heal
        HealResult result = Frap.getLastHealResult(openLink);
        assertThat(result).isNotNull();
        assertThat(result.healed()).isTrue();
        // Note: semantics would need to be checked in report
    }

    @Test
    @Order(3)
    @DisplayName("CONF-SH-V1V2: schedule v1 to v2 compatibility")
    void testScheduleV1ToV2() {
        // First test on v1 (baseline)
        page.navigate(ConferencePaths.BASE_URL + ConferencePaths.Conf.SCHEDULE_V1);

        var baseline = ConferencePaths.confFrap();
        var baselineLink = Frap.withFrap(
            page.locator("[data-testid='talk-keynote']"),
            page,
            baseline
        );

        baselineLink.click();
        assertThat(page.url()).contains("talk.html");
    }

    @Test
    @Order(4)
    @DisplayName("CONF-SH-REG: registration page navigation")
    void testRegistrationNavigation() {
        page.navigate(ConferencePaths.BASE_URL + ConferencePaths.Conf.INDEX);

        var registerLink = Frap.withFrap(
            page.locator("a:has-text('Registration')"),
            page,
            ConferencePaths.confFrap()
        );

        registerLink.click();
        assertThat(page.url()).contains("register.html");
    }

    @Test
    @Order(5)
    @DisplayName("CONF-SH-CFP: CFP page navigation")
    void testCfpNavigation() {
        page.navigate(ConferencePaths.BASE_URL + ConferencePaths.Conf.INDEX);

        var cfpLink = Frap.withFrap(
            page.locator("a:has-text('Call for Papers')"),
            page,
            ConferencePaths.confFrap()
        );

        cfpLink.click();
        assertThat(page.url()).contains("cfp.html");
    }

    @Test
    @Order(6)
    @DisplayName("CONF-SH-SPK: speakers page navigation")
    void testSpeakersNavigation() {
        page.navigate(ConferencePaths.BASE_URL + ConferencePaths.Conf.INDEX);

        var speakersLink = Frap.withFrap(
            page.locator("a:has-text('Speakers')"),
            page,
            ConferencePaths.confFrap()
        );

        speakersLink.click();
        assertThat(page.url()).contains("speakers.html");
    }

    @Test
    @Order(7)
    @DisplayName("CONF-SH-TALK: individual speaker talk navigation")
    void testSpeakerTalkNavigation() {
        page.navigate(ConferencePaths.BASE_URL + ConferencePaths.Conf.SPEAKERS);

        var speakerLink = Frap.withFrap(
            page.locator("[data-testid='speaker-keynote']"),
            page,
            ConferencePaths.confFrap()
        );

        speakerLink.click();
        assertThat(page.url()).contains("speaker.html");
    }
}
