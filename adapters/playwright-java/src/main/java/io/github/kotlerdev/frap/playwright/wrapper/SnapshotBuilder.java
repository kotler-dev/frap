package io.github.kotlerdev.frap.playwright.wrapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
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

    /**
     * Visible label for snapshots — skips nested {@code <style>}/{@code <script>} text.
     * Mirrors TypeScript {@code wrapper.ts} (issue #14).
     */
    /** JS expression: visible text for snapshot fields (issue #14). */
    private static final String VISIBLE_TEXT_EXPR =
        "(((el.innerText ?? el.textContent) || '').replace(/\\s+/g, ' ').trim().substring(0, 100)) || undefined";

    private static final String ACCESSIBLE_NAME_JS = """
        function buildLabelMap() {
            const labelMap = new Map();
            document.querySelectorAll('label[for]').forEach((label) => {
                const forId = label.getAttribute('for');
                const text = (label.innerText || '').replace(/\\s+/g, ' ').trim();
                if (forId && text) labelMap.set(forId, text);
            });
            document.querySelectorAll('label').forEach((label) => {
                if (!label.hasAttribute('for')) {
                    const control = label.querySelector('input, button, select, textarea, [role="button"]');
                    if (control && control.id) {
                        const text = (label.innerText || '').replace(/\\s+/g, ' ').trim();
                        if (text) labelMap.set(control.id, text);
                    }
                }
            });
            return labelMap;
        }
        function computeAccessibleName(el, labelMap) {
            const ariaLabel = el.getAttribute('aria-label');
            if (ariaLabel && ariaLabel.trim()) return ariaLabel.trim();
            const labelledBy = el.getAttribute('aria-labelledby');
            if (labelledBy) {
                const texts = labelledBy.split(/\\s+/).map((id) => document.getElementById(id))
                    .filter(Boolean)
                    .map((ref) => (ref.innerText || '').replace(/\\s+/g, ' ').trim())
                    .filter(Boolean);
                if (texts.length) return texts.join(' ');
            }
            if (el.id && labelMap.has(el.id)) return labelMap.get(el.id);
            const parentLabel = el.closest('label');
            if (parentLabel && !parentLabel.hasAttribute('for')) {
                const clone = parentLabel.cloneNode(true);
                clone.querySelectorAll('input, button, select, textarea, [role="button"]').forEach((e) => e.remove());
                const text = (clone.innerText || '').replace(/\\s+/g, ' ').trim();
                if (text) return text;
            }
            const directCaption = el.querySelector(':scope > label');
            if (directCaption) {
                const text = (directCaption.innerText || '').replace(/\\s+/g, ' ').trim();
                if (text) return text;
            }
            const parent = el.parentElement;
            if (parent) {
                const siblingLabels = Array.from(parent.children).filter(
                    (child) => child.tagName === 'LABEL' && !child.contains(el)
                );
                for (const label of siblingLabels) {
                    const text = (label.innerText || '').replace(/\\s+/g, ' ').trim();
                    if (text) return text;
                }
            }
            return undefined;
        }
        """;

    private static final String SNAPSHOT_SCRIPT = """
        () => {
        """ + ACCESSIBLE_NAME_JS + """
            const elements = [];
            const seen = new Set();
            const labelMap = buildLabelMap();
            const SELECTOR = 'button, input, a, select, textarea, [contenteditable="true"], [contenteditable=""], [data-testid], [data-id], [id], li[id], [role="button"], [role="link"], [role="textbox"], [role="checkbox"]';

            function pushElement(el) {
                if (!el || seen.has(el)) return;
                seen.add(el);
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
                const dataId = el.getAttribute('data-id');
                const tagName = el.tagName.toLowerCase();
                if (testId) {
                    selector = `[data-testid="${testId}"]`;
                } else if (dataId) {
                    selector = `${tagName}[data-id="${dataId}"]`;
                } else if (el.id) {
                    selector = `${tagName}[id="${el.id}"]`;
                } else {
                    selector = tagName;
                }

                let positionInParent = undefined;
                const parent = el.parentElement;
                if (parent) {
                    const siblings = Array.from(parent.children).filter(
                        (child) => child.tagName === el.tagName
                    );
                    const idx = siblings.indexOf(el);
                    if (idx >= 0) positionInParent = idx;
                }

                elements.push({
                    selector: selector,
                    tag: tagName,
                    attributes: attributes,
                    text_content: __VISIBLE_TEXT__,
                    accessible_name: computeAccessibleName(el, labelMap),
                    path: path,
                    position_in_parent: positionInParent
                });
            }

            function collectFromRoot(root) {
                root.querySelectorAll(SELECTOR).forEach((el) => pushElement(el));
                root.querySelectorAll('*').forEach((node) => {
                    if (node.shadowRoot) {
                        collectFromRoot(node.shadowRoot);
                    }
                });
            }

            collectFromRoot(document);

            return {
                html: (document.documentElement?.outerHTML || '').substring(0, 1000),
                elements: elements
            };
        }
        """.replace("__VISIBLE_TEXT__", VISIBLE_TEXT_EXPR);

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
            Map<String, Object> rawResult = (Map<String, Object>) page.evaluate(SNAPSHOT_SCRIPT);

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
        String accessibleName = (String) raw.get("accessible_name");
        @SuppressWarnings("unchecked")
        List<String> path = (List<String>) raw.get("path");

        Integer positionInParent = null;
        Object rawPosition = raw.get("position_in_parent");
        if (rawPosition instanceof Number number) {
            positionInParent = number.intValue();
        }

        return new DOMElementInfo(
            selector != null ? selector : "",
            tag != null ? tag : "unknown",
            attributes != null ? attributes : Map.of(),
            textContent,
            accessibleName,
            path != null ? path : List.of(),
            positionInParent
        );
    }

    /**
     * Extracts signature for a specific selector from the page.
     *
     * @param selector CSS selector
     * @return DOMElementInfo if found, null otherwise
     */
    public DOMElementInfo extractForSelector(String selector) {
        if (!isLikelyCssSelector(selector)) {
            logger.debug("Skipping signature extraction for non-CSS selector: {}", selector);
            return null;
        }
        String script = """
            (selector) => {
            """ + ACCESSIBLE_NAME_JS + """
                const el = document.querySelector(selector);
                if (!el) return null;
                const labelMap = buildLabelMap();

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
                    text_content: __VISIBLE_TEXT__,
                    accessible_name: computeAccessibleName(el, labelMap),
                    path: path
                };
            }
            """.replace("__VISIBLE_TEXT__", VISIBLE_TEXT_EXPR);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = (Map<String, Object>) page.evaluate(script, selector);
            if (raw == null) {
                return null;
            }
            return parseElement(raw);
        } catch (Exception e) {
            if (isInvalidSelectorError(e)) {
                logger.debug("Skipping signature extraction for invalid CSS selector {}: {}", selector, e.getMessage());
            } else {
                logger.warn("Failed to extract signature for {}: {}", selector, e.getMessage());
            }
            return null;
        }
    }

    /**
     * Quick check if an element exists for the given selector.
     */
    public boolean exists(String selector) {
        if (!isLikelyCssSelector(selector)) {
            logger.debug("Skipping CSS existence check for non-CSS selector: {}", selector);
            return false;
        }
        try {
            Boolean exists = (Boolean) page.evaluate(
                "(sel) => document.querySelector(sel) !== null",
                selector
            );
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            if (isInvalidSelectorError(e)) {
                logger.debug("Skipping invalid CSS selector {}: {}", selector, e.getMessage());
            } else {
                logger.warn("Failed to check existence of {}: {}", selector, e.getMessage());
            }
            return false;
        }
    }

    public boolean exists(Locator locator) {
        try {
            return locator.count() > 0;
        } catch (PlaywrightException e) {
            logger.debug("Failed locator existence check: {}", e.getMessage());
            return false;
        }
    }

    public DOMElementInfo extractForLocator(Locator locator, String selectorForReporting) {
        try {
            Locator first = locator.first();
            if (first.count() == 0) {
                return null;
            }
            ElementHandle handle = first.elementHandle();
            if (handle == null) {
                return null;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = (Map<String, Object>) handle.evaluate("""
                (el) => {
                """ + ACCESSIBLE_NAME_JS + """
                    const labelMap = buildLabelMap();
                    const attributes = {};
                    const attrs = Array.from(el.attributes || []);
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
                        selector: null,
                        tag: el.tagName.toLowerCase(),
                        attributes: attributes,
                        text_content: __VISIBLE_TEXT__,
                        accessible_name: computeAccessibleName(el, labelMap),
                        path: path
                    };
                }
                """.replace("__VISIBLE_TEXT__", VISIBLE_TEXT_EXPR));
            if (raw == null) {
                return null;
            }
            if (selectorForReporting != null && !selectorForReporting.isBlank()) {
                raw.put("selector", selectorForReporting);
            }
            return parseElement(raw);
        } catch (PlaywrightException e) {
            logger.debug("Failed to extract signature via locator {}: {}", selectorForReporting, e.getMessage());
            return null;
        }
    }

    private static boolean isInvalidSelectorError(Exception e) {
        String msg = e.getMessage();
        return msg != null && msg.contains("SyntaxError") && msg.contains("querySelector");
    }

    private static boolean isLikelyCssSelector(String selector) {
        if (selector == null || selector.isBlank()) {
            return false;
        }
        if (selector.startsWith("Locator@")) {
            return false;
        }
        return !selector.startsWith("internal:");
    }
}
