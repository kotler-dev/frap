/**
 * Playwright integration for Frap self-healing selectors.
 *
 * <p>This package provides the main API for using Frap with Playwright Java:
 * <ul>
 *   <li>{@link Frap} — Static API for healing, discovery, and Page Object generation</li>
 *   <li>{@link FrapLocator} — Wrapped Playwright locator with healing on failure</li>
 *   <li>{@link SnapshotBuilder} — Build DOM snapshots from Playwright pages</li>
 * </ul>
 *
 * <p><strong>Self-healing example:</strong>
 * <pre>{@code
 * FrapLocator button = Frap.withFrap(
 *     page.locator("[data-testid='pay-btn']"),
 *     page
 * );
 * button.click(); // Automatically heals if selector changed
 * }</pre>
 *
 * <p><strong>Discovery example:</strong>
 * <pre>{@code
 * ElementMap map = Frap.discover(page);
 * List<Path> files = Frap.generatePageObject(
 *     page,
 *     Path.of("target/generated"),
 *     GenerateOptions.javaPlaywright("HomePage", "com.example")
 * );
 * }</pre>
 *
 * @see io.github.kotlerdev.frap.playwright.extension.FrapExtension
 * @see io.github.kotlerdev.frap.playwright.context.FrapContext
 * @since 1.0.0
 */
package io.github.kotlerdev.frap.playwright.wrapper;