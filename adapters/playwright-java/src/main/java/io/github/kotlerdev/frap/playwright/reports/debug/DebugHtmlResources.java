package io.github.kotlerdev.frap.playwright.reports.debug;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class DebugHtmlResources {
    static final String ICONS_SVG = """
        <svg xmlns="http://www.w3.org/2000/svg" aria-hidden="true" style="display:none">
          <symbol id="icon-check" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 6L9 17l-5-5"/>
          </symbol>
          <symbol id="icon-warning" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 9v4M12 17h.01"/><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
          </symbol>
          <symbol id="icon-sun" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41"/>
          </symbol>
          <symbol id="icon-moon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
          </symbol>
        </svg>""";

    static final String THEME_INIT = """
        (function () {
          var stored = localStorage.getItem('frap-theme');
          if (!stored && window.matchMedia('(prefers-color-scheme: dark)').matches) {
            stored = 'dark';
          }
          document.documentElement.setAttribute('data-theme', stored || 'light');
        })();""";

    static final String THEME_TOGGLE = """
        (function () {
          var root = document.documentElement;
          var toggle = document.querySelector('.theme-toggle');
          if (!toggle) return;
          function applyTheme(theme) {
            root.setAttribute('data-theme', theme);
            localStorage.setItem('frap-theme', theme);
          }
          toggle.addEventListener('click', function () {
            applyTheme(root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark');
          });
        })();""";

    private static String stylesCache;

    private DebugHtmlResources() {}

    static String reportStyles() {
        if (stylesCache != null) {
            return stylesCache;
        }
        try (InputStream in = DebugHtmlResources.class.getClassLoader()
            .getResourceAsStream("frap-debug-report.css")) {
            if (in == null) {
                throw new IllegalStateException("frap-debug-report.css not found on classpath");
            }
            stylesCache = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return stylesCache;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load frap-debug-report.css", e);
        }
    }
}
