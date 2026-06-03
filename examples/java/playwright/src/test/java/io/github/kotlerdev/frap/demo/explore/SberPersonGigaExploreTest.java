package io.github.kotlerdev.frap.demo.explore;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import io.github.kotlerdev.frap.core.dto.CoverageMode;
import io.github.kotlerdev.frap.core.dto.GeneratedArtifact;
import io.github.kotlerdev.frap.core.dto.GeneratedFile;
import io.github.kotlerdev.frap.core.dto.ElementMap;
import io.github.kotlerdev.frap.core.dto.ElementNode;
import io.github.kotlerdev.frap.core.dto.GenerateOptions;
import io.github.kotlerdev.frap.core.dto.MapOptions;
import io.github.kotlerdev.frap.playwright.extension.FrapExtension;
import io.github.kotlerdev.frap.playwright.wrapper.Frap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Manual live discover against Sber person_giga (C013). Not part of CI ({@code manual-live} tag).
 *
 * <p>Run: {@code ./scripts/run-sber-explore.sh} from {@code frap/} repo root.</p>
 */
@Tag("manual-live")
@ExtendWith(FrapExtension.class)
@DisplayName("Sber person_giga live explore (C013)")
class SberPersonGigaExploreTest {

    private static final String DEFAULT_URL = "https://www.sberbank.ru/ru/person_giga";
    private static final int NAV_TIMEOUT_MS = 120_000;
    private static final int SPA_WAIT_MS = 90_000;
    private static final int MODAL_WAIT_MS = 45_000;

    private static final List<String> ANCHORS_TILES = List.of(
        "Калькулятор процентов",
        "СберБанк Онлайн",
        "Офисы и банкоматы",
        "Курсы валют"
    );

    private static final List<String> ANCHORS_MODAL = List.of(
        "Поможем рассчитать",
        "Автокредит",
        "Сколько вам нужно",
        "На срок"
    );

    static Playwright playwright;
    static Browser browser;
    static Path outputDir;

    @BeforeAll
    static void beforeAll() throws IOException {
        outputDir = resolveOutputDir();
        Files.createDirectories(outputDir);

        boolean headed = !"false".equalsIgnoreCase(System.getProperty("frap.explore.headed", "true"));
        playwright = Playwright.create();
        int slowMo = Integer.parseInt(System.getProperty("frap.explore.slowMo", headed ? "0" : "0"));
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
            .setHeadless(!headed)
            .setSlowMo(slowMo);
        resolveBrowserExecutable().ifPresent(path -> {
            launchOptions.setExecutablePath(path);
            System.out.println("[explore] browserExecutable=" + path);
        });
        browser = playwright.chromium().launch(launchOptions);
        System.out.println("[explore] outputDir=" + outputDir.toAbsolutePath());
        System.out.println("[explore] headed=" + headed);
    }

    /**
     * Use installed Chromium-based browser (Yandex, Chrome) so OS trust store / NUC certs apply.
     * Set {@code -Dfrap.explore.browserExecutable=/path/to/binary} or env via launch script.
     */
    private static Optional<Path> resolveBrowserExecutable() {
        String configured = System.getProperty("frap.explore.browserExecutable");
        if (configured != null && !configured.isBlank()) {
            Path path = Path.of(configured);
            if (Files.isExecutable(path)) {
                return Optional.of(path.toAbsolutePath());
            }
            System.out.println("[explore] WARN: browserExecutable not found: " + path);
            return Optional.empty();
        }
        String auto = System.getProperty("frap.explore.browser", "auto");
        if ("chromium".equalsIgnoreCase(auto) || "bundled".equalsIgnoreCase(auto)) {
            return Optional.empty();
        }
        for (String candidate : defaultBrowserCandidates()) {
            Path path = Path.of(candidate);
            if (Files.isExecutable(path)) {
                return Optional.of(path.toAbsolutePath());
            }
        }
        return Optional.empty();
    }

    private static List<String> defaultBrowserCandidates() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) {
            return List.of(
                "/Applications/Yandex.app/Contents/MacOS/Yandex",
                "/Applications/Yandex Browser.app/Contents/MacOS/Yandex",
                "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
            );
        }
        if (os.contains("win")) {
            String local = System.getenv("LOCALAPPDATA");
            if (local != null) {
                return List.of(
                    local + "\\Yandex\\YandexBrowser\\Application\\browser.exe",
                    local + "\\Google\\Chrome\\Application\\chrome.exe"
                );
            }
        }
        return List.of("/usr/bin/yandex-browser", "/usr/bin/google-chrome");
    }

    @AfterAll
    static void afterAll() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @Test
    @DisplayName("discover tiles then modal (live URL)")
    void explorePersonGigaCalculatorFlow() throws IOException {
        String url = System.getProperty("frap.explore.url", DEFAULT_URL);
        Page page = browser.newPage();
        page.setDefaultNavigationTimeout(NAV_TIMEOUT_MS);
        page.setDefaultTimeout(60_000);

        boolean strict = !"false".equalsIgnoreCase(System.getProperty("frap.explore.strictAssertions", "true"));
        MapOptions mapOptions = exploreMapOptions(page);
        try {
            logStep("1/8 navigate " + url);
            page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            logStep("2/8 wait for SPA content (up to " + (SPA_WAIT_MS / 1000) + "s)");
            dismissCookieBannerIfPresent(page);
            Locator calcTile = waitForCalculatorTile(page);

            logStep("3/8 scroll to calculator tile");
            calcTile.scrollIntoViewIfNeeded();
            saveProbe(page, "00-after-scroll");

            if (strict && !landingVisible(page)) {
                logStep("WARN: landing hero texts not in DOM yet; continuing if calculator visible");
            }

            logStep("4/8 Frap.discover (snapshot + map — can take 1–5 min on large pages)");
            long t0 = System.currentTimeMillis();
            ElementMap mapTiles = Frap.discover(page, mapOptions);
            long discoverTilesMs = System.currentTimeMillis() - t0;
            logStep("4/8 discover done in " + (discoverTilesMs / 1000) + "s");
            logStep("    Elements total: " + mapTiles.elements().size());
            logStep("    Clusters: " + mapTiles.clusters().size());
            long actionable = mapTiles.elements().stream()
                .filter(e -> e.fragile() == false && e.locator().confidence() > 0.6).count();
            logStep("    Actionable (conf>0.6, not fragile): " + actionable);
            logStep("    Fragile only: " + mapTiles.elements().stream().filter(ElementNode::fragile).count());
            Frap.writeElementMap(mapTiles, outputDir.resolve("01-after-scroll.element-map.json"));
            List<ExploreAnchorReport.AnchorMatch> tileMatches =
                ExploreAnchorReport.findMatches(mapTiles, ANCHORS_TILES);
            ExploreAnchorReport.writeSummary(outputDir, "01-after-scroll", tileMatches);

            logStep("5/8 click calculator tile");
            Optional<ElementNode> calcNode = findNodeForKey(mapTiles, "Калькулятор процентов");
            if (calcNode.isPresent()) {
                ElementNode node = calcNode.get();
                String clickSelector = node.recommendedSelector();
                System.out.println("[explore] click tile via map: " + clickSelector
                    + " (strategy=" + node.locator().strategy() + ", accessibleName="
                    + node.accessibleName() + ")");
                page.locator(clickSelector).first().click();
            } else {
                System.out.println("[explore] No map match for tile; clicking getByText fallback");
                calcTile.click();
            }

            logStep("6/8 wait for modal (up to " + (MODAL_WAIT_MS / 1000) + "s, no networkidle)");
            Locator modalTitle = page.getByText("Поможем рассчитать").first();
            try {
                modalTitle.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(MODAL_WAIT_MS));
            } catch (Exception e) {
                saveProbe(page, "02-modal-missing");
                String msg = "Modal title «Поможем рассчитать» not visible after tile click: " + e.getMessage();
                appendStatus(msg);
                if (strict) {
                    throw new AssertionError(msg, e);
                }
                logExploreFailure(msg);
                return;
            }

            saveProbe(page, "02-modal-visible");
            pauseAfterModalIfConfigured(page);

            logStep("7/8 Frap.discover modal");
            t0 = System.currentTimeMillis();
            ElementMap mapModal = Frap.discover(page, mapOptions);
            long discoverModalMs = System.currentTimeMillis() - t0;
            logStep("7/8 modal discover done in " + (discoverModalMs / 1000) + "s");
            logStep("    Elements total: " + mapModal.elements().size());
            logStep("    Clusters: " + mapModal.clusters().size());
            Frap.writeElementMap(mapModal, outputDir.resolve("02-modal-open.element-map.json"));
            List<ExploreAnchorReport.AnchorMatch> modalMatches =
                ExploreAnchorReport.findMatches(mapModal, ANCHORS_MODAL);
            ExploreAnchorReport.writeSummary(outputDir, "02-modal-open", modalMatches);

            if (Boolean.parseBoolean(System.getProperty("frap.explore.generatePo", "true"))) {
                logStep("8/8 generate Page Object from modal map");
                Path poDir = outputDir.resolve("generated-po");
                List<Path> written = Frap.generatePageObject(
                    mapModal,
                    poDir,
                    GenerateOptions.javaPlaywright("SberPersonGigaModalPage", "io.github.kotlerdev.frap.explore.generated")
                );
                assertThat(written).isNotEmpty();
                System.out.println("[explore] Page Object files: " + written.size());
            } else {
                logStep("8/8 skip Page Object (frap.explore.generatePo=false)");
            }

            writeRunStatusSuccess(discoverTilesMs, discoverModalMs, mapTiles, mapModal);
            logExploreSuccess(tileMatches, modalMatches, outputDir);
        } finally {
            page.close();
        }
    }

    private static void logStep(String message) {
        System.out.println("[explore] " + message);
        System.out.flush();
    }

    /** Hold headed browser on open modal; set {@code -Dfrap.explore.pauseAfterModalMs=5000} (or env via script). */
    private static void pauseAfterModalIfConfigured(Page page) {
        int pauseMs = Integer.parseInt(System.getProperty("frap.explore.pauseAfterModalMs", "0"));
        boolean headed = !"false".equalsIgnoreCase(System.getProperty("frap.explore.headed", "true"));
        if (pauseMs <= 0) {
            return;
        }
        if (!headed) {
            logStep("skip modal pause (headed=false; pause needs visible window)");
            return;
        }
        logStep("PAUSE " + (pauseMs / 1000) + "s — modal open, inspect in browser (frap.explore.pauseAfterModalMs)");
        page.waitForTimeout(pauseMs);
    }

    private static void logExploreSuccess(
        List<ExploreAnchorReport.AnchorMatch> tileMatches,
        List<ExploreAnchorReport.AnchorMatch> modalMatches,
        Path artifactsDir
    ) {
        long tilesOk = tileMatches.stream().filter(ExploreAnchorReport.AnchorMatch::found).count();
        long modalOk = modalMatches.stream().filter(ExploreAnchorReport.AnchorMatch::found).count();
        boolean allAnchors = tilesOk == tileMatches.size() && modalOk == modalMatches.size();

        System.out.println();
        System.out.println("[explore] ========== SUCCESS ==========");
        System.out.println("[explore] Flow completed: landing → tile click → modal → discover → artifacts");
        System.out.printf("[explore] Tiles anchors: %d/%d matched%n", tilesOk, tileMatches.size());
        System.out.printf("[explore] Modal anchors: %d/%d matched%n", modalOk, modalMatches.size());
        if (allAnchors) {
            System.out.println("[explore] All C013 anchor keys found in element maps.");
        } else {
            System.out.println("[explore] WARN: some anchor keys missing — see explore-summary.md");
        }
        System.out.println("[explore] Artifacts: " + artifactsDir.toAbsolutePath());
        System.out.println("[explore] ==============================");
        System.out.flush();
    }

    private static void logExploreFailure(String reason) {
        System.out.println();
        System.out.println("[explore] ========== FAILED ==========");
        System.out.println("[explore] " + reason);
        System.out.println("[explore] =============================");
        System.out.flush();
    }

    private static void writeRunStatusSuccess(
        long discoverTilesMs,
        long discoverModalMs,
        ElementMap mapTiles,
        ElementMap mapModal
    ) throws IOException {
        Files.writeString(
            outputDir.resolve("explore-run-status.md"),
            "# Run status\n\n**SUCCESS**: artifacts written.\n\n## Stats\n\n"
                + "| Phase | Time | Elements | Clusters |\n"
                + "|-------|------|----------|----------|\n"
                + "| After scroll | " + (discoverTilesMs / 1000) + "s | " + mapTiles.elements().size() + " | "
                + mapTiles.clusters().size() + " |\n"
                + "| Modal open | " + (discoverModalMs / 1000) + "s | " + mapModal.elements().size() + " | "
                + mapModal.clusters().size() + " |\n"
        );
    }

    private static MapOptions exploreMapOptions(Page page) {
        int maxElements = Integer.parseInt(System.getProperty("frap.explore.maxElements", "12000"));
        return new MapOptions(page.url(), true, maxElements, CoverageMode.SEMANTIC);
    }

    private static Locator waitForCalculatorTile(Page page) {
        Locator calc = page.getByText("Калькулятор процентов").first();
        Locator.WaitForOptions opts = new Locator.WaitForOptions()
            .setState(WaitForSelectorState.VISIBLE)
            .setTimeout(SPA_WAIT_MS);
        try {
            calc.waitFor(opts);
            return calc;
        } catch (Exception first) {
            logStep("calculator tile not visible yet, retry after load...");
            page.waitForLoadState(com.microsoft.playwright.options.LoadState.LOAD);
            calc.waitFor(opts);
            return calc;
        }
    }

    private static void dismissCookieBannerIfPresent(Page page) {
        for (String label : List.of("Accept", "Принять", "Согласен", "Хорошо", "OK")) {
            Locator btn = page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON, new Page.GetByRoleOptions().setName(label));
            if (btn.count() > 0) {
                try {
                    btn.first().click(new Locator.ClickOptions().setTimeout(3_000));
                    logStep("dismissed banner: " + label);
                    page.waitForTimeout(500);
                    return;
                } catch (Exception ignored) {
                    // try next label
                }
            }
        }
    }

    private static boolean landingVisible(Page page) {
        return page.getByText("ИИ ответит на любой вопрос").count() > 0
            || page.getByText("Сберу — 185").count() > 0
            || page.getByText("Сберу - 185").count() > 0;
    }

    private static void saveProbe(Page page, String name) throws IOException {
        page.screenshot(new Page.ScreenshotOptions().setPath(outputDir.resolve(name + ".png")));
        String html = page.content();
        int limit = Math.min(html.length(), 50_000);
        Files.writeString(outputDir.resolve(name + ".html-snippet"), html.substring(0, limit));
    }

    private static void appendStatus(String line) throws IOException {
        Path status = outputDir.resolve("explore-run-status.md");
        String existing = Files.exists(status) ? Files.readString(status) : "# Run status\n\n";
        Files.writeString(status, existing + line + "\n");
    }

    private static void writeProbeDiscover(Page page, String basename) throws IOException {
        MapOptions opts = exploreMapOptions(page);
        ElementMap map = Frap.discover(page, opts);
        Frap.writeElementMap(map, outputDir.resolve(basename + ".element-map.json"));
        ExploreAnchorReport.writeSummary(
            outputDir,
            basename,
            ExploreAnchorReport.findMatches(map, ANCHORS_TILES)
        );
        System.out.println("[explore] Probe discover: " + map.elements().size() + " elements");
    }

    private static Optional<ElementNode> findNodeForKey(ElementMap map, String key) {
        return map.elements().stream()
            .filter(el -> ExploreAnchorReport.matchScore(el, key) > 0)
            .max(java.util.Comparator.comparingInt(el -> ExploreAnchorReport.matchScore(el, key)));
    }

    private static Path resolveOutputDir() throws IOException {
        String configured = System.getProperty("frap.explore.outputDir");
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path workspaceArtifacts = Path.of("../../../../project/artifacts/sber-person-giga/run-" + ts)
            .normalize()
            .toAbsolutePath();
        return workspaceArtifacts;
    }
}
