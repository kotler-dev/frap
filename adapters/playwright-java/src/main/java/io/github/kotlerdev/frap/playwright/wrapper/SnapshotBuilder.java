package io.github.kotlerdev.frap.playwright.wrapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Page;
import io.github.kotlerdev.frap.core.dto.DOMElementInfo;
import io.github.kotlerdev.frap.core.dto.DOMSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds DOM snapshots from Playwright pages.
 * Mirrors TypeScript {@code buildSnapshotFromPage} from wrapper.ts.
 */
public class SnapshotBuilder {
    private static final Logger logger = LoggerFactory.getLogger(SnapshotBuilder.class);

    private static final String SNAPSHOT_SCRIPT = """
        () => {
            const elements = [];
            // Only interactive elements and elements with data-testid - much faster than querySelectorAll('*')
            const interactiveElements = document.querySelectorAll(
                'button, input, a, select, textarea, [data-testid], [role="button"], [role="link"], [role="input"]'
            );

            interactiveElements.forEach((el) => {
                const attributes = {};
                const attrs = Array.from(el.attributes);
                for (const attr of attrs) {
                    attributes[attr.name] = attr.value;
                }

                const path = [];
                let current = el;
                while (current && current !== document.body) {
                    const role = current.getAttribute('role');
                    path.unshift(`${current.tagName.toLowerCase()}:${role || '-'}`);
                    current = current.parentElement;
                }

                let selector;
                const testId = el.getAttribute('data-testid');
                if (testId) {
                    selector = `[data-testid="${testId}"]`;
                } else if (el.id) {
                    selector = `#${el.id}`;
                } else {
                    const tagName = el.tagName.toLowerCase();
                    const text = (el.textContent || '').substring(0, 20);
                    selector = `${tagName}:contains("${text}")`;
                }

                elements.push({
                    selector: selector,
                    tag: el.tagName.toLowerCase(),
                    attributes: attributes,
                    text_content: (el.textContent || '').substring(0, 100) || undefined,
                    path: path
                });
            });

            return {
                html: (document.documentElement?.outerHTML || '').substring(0, 1000),
                elements: elements
            };
        }
        """;

    private final Page page;
    private final ObjectMapper objectMapper;

    public SnapshotBuilder(Page page) {
        this.page = page;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Builds a DOM snapshot from the current page state.
     *
     * @return DOMSnapshot with interactive elements
     */
    public DOMSnapshot build() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> rawResult = page.evaluate(SNAPSHOT_SCRIPT);

            String html = (String) rawResult.getOrDefault("html", "");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawElements = (List<Map<String, Object>>) rawResult.get("elements");

            List<DOMElementInfo> elements = new ArrayList<>();
            for (Map<String, Object> raw : rawElements) {
                elements.add(parseElement(raw));
            }

            logger.debug("Built DOM snapshot with {} elements", elements.size());
            return new DOMSnapshot(html, elements);

        } catch (Exception e) {
            logger.error("Failed to build DOM snapshot: {}", e.getMessage(), e);
            return new DOMSnapshot("", List.of());
        }
    }

    private DOMElementInfo parseElement(Map<String, Object> raw) {
        String selector = (String) raw.get("selector");
        String tag = (String) raw.get("tag");
        @SuppressWarnings("unchecked")
        Map<String, String> attributes = (Map<String, String>) raw.get("attributes");
        String textContent = (String) raw.get("text_content");
        @SuppressWarnings("unchecked")
        List<String> path = (List<String>) raw.get("path");

        return new DOMElementInfo(
            selector != null ? selector : "",
            tag != null ? tag : "unknown",
            attributes != null ? attributes : Map.of(),
            textContent,
            path != null ? path : List.of()
        );
    }

    /**
     * Extracts signature for a specific selector from the page.
     *
     * @param selector CSS selector
     * @return DOMElementInfo if found, null otherwise
     */
    public DOMElementInfo extractForSelector(String selector) {
        String script = String.format("""
            (selector) => {
                const el = document.querySelector(selector);
                if (!el) return null;

                const attributes = {};
                const attrs = Array.from(el.attributes);
                for (const attr of attrs) {
                    attributes[attr.name] = attr.value;
                }

                const path = [];
                let current = el;
                while (current && current !== document.body) {
                    const role = current.getAttribute('role');
                    path.unshift(`${current.tagName.toLowerCase()}:${role || '-'}`);
                    current = current.parentElement;
                }

                return {
                    selector: selector,
                    tag: el.tagName.toLowerCase(),
                    attributes: attributes,
                    text_content: (el.textContent || '').substring(0, 100) || undefined,
                    path: path
                };
            }
            """);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = (Map<String, Object>) page.evaluate(script, selector);
            if (raw == null) {
                return null;
            }
            return parseElement(raw);
        } catch (Exception e) {
            logger.warn("Failed to extract signature for {}: {}", selector, e.getMessage());
            return null;
        }
    }

    /**
     * Quick check if an element exists for the given selector.
     */
    public boolean exists(String selector) {
        try {
            Boolean exists = page.evaluate(
                "(sel) => document.querySelector(sel) !== null",
                selector
            );
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            logger.warn("Failed to check existence of {}: {}", selector, e.getMessage());
            return false;
        }
    }
}
