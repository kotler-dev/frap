package io.github.kotlerdev.frap.demo.discovery;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.github.kotlerdev.frap.core.dto.ClusterType;
import io.github.kotlerdev.frap.core.dto.ElementMap;
import io.github.kotlerdev.frap.core.dto.MapOptions;
import io.github.kotlerdev.frap.playwright.extension.FrapExtension;
import io.github.kotlerdev.frap.playwright.wrapper.Frap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F019 / C012: live Playwright discover against dom-benchmark HTML pages.
 */
@Tag("e2e")
@Tag("dom-benchmark")
@ExtendWith(FrapExtension.class)
@DisplayName("DOM benchmark discover (F019)")
class DomBenchmarkE2eTest {

    private static final Path BENCH_PAGES = Path.of("fixtures", "dom-benchmark", "pages");

    static Playwright playwright;
    static Browser browser;
    Page page;

    @BeforeAll
    static void beforeAll() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
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
    void catalogListFromFixtureFile() throws IOException {
        Path html = resolvePage("01-catalog-list.html");
        page.navigate("file://" + html.toAbsolutePath());

        ElementMap map = Frap.discover(page, MapOptions.semanticCatalog());

        assertThat(map.elements()).hasSizeGreaterThanOrEqualTo(3);
        long listClusters = map.clusters().stream()
            .filter(c -> c.clusterType() == ClusterType.LIST)
            .filter(c -> c.elementIds().size() >= 2)
            .count();
        assertThat(listClusters).isGreaterThanOrEqualTo(1);

        map.elements().forEach(el ->
            System.out.printf(
                "%s | %s | conf=%.2f | fragile=%s | cluster=%s%n",
                el.id(),
                el.recommendedSelector(),
                el.locator().confidence(),
                el.fragile(),
                el.clusterId()
            )
        );
    }

    @Test
    void discoverToFileWritesJson(@TempDir Path temp) throws IOException {
        Path html = resolvePage("01-catalog-list.html");
        page.navigate("file://" + html.toAbsolutePath());

        Path out = temp.resolve("element-map.json");
        Frap.discoverToFile(page, out, MapOptions.semanticCatalog());

        assertThat(Files.readString(out)).contains("elements").contains("clusters");
    }

    @Test
    void siblingLabelUsesAccessibleName() throws IOException {
        Path html = resolvePage("09-sibling-label.html");
        page.navigate("file://" + html.toAbsolutePath());

        ElementMap map = Frap.discover(page, MapOptions.semanticCatalog());
        assertThat(map.elements()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(map.elements().stream())
            .anyMatch(e ->
                "Калькулятор процентов".equals(e.locator().value())
                    || "Калькулятор процентов".equals(e.accessibleName()));
    }

    @Test
    void ariaOnlyPageFindsLabeledControl() throws IOException {
        Path html = resolvePage("04-aria-only.html");
        page.navigate("file://" + html.toAbsolutePath());

        ElementMap map = Frap.discover(page, MapOptions.semanticCatalog());
        assertThat(map.elements()).isNotEmpty();
        assertThat(map.elements().stream())
            .anyMatch(e -> "Submit order".equals(e.locator().value()));
    }

    private static Path resolvePage(String name) {
        Path fromRepo = Path.of("..", "..", "..", "fixtures", "dom-benchmark", "pages", name).normalize();
        if (Files.isRegularFile(fromRepo)) {
            return fromRepo.toAbsolutePath();
        }
        Path fromCwd = BENCH_PAGES.resolve(name).toAbsolutePath();
        if (Files.isRegularFile(fromCwd)) {
            return fromCwd;
        }
        throw new IllegalStateException("dom-benchmark page not found: " + name);
    }
}
