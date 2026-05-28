/**
 * Frap Core client implementations for communicating with the Rust core.
 *
 * <p>This package provides the primary API for using Frap from Java:
 * <ul>
 *   <li>{@link FrapCoreClient} — Common interface for all clients</li>
 *   <li>{@link FrapRpcClient} — JSON-RPC implementation with bundled binary</li>
 * </ul>
 *
 * <p><strong>Quick start:</strong>
 * <pre>{@code
 * try (FrapCoreClient client = FrapRpcClient.create()) {
 *     HealResult result = client.heal(request);
 *     if (result.healed()) {
 *         System.out.println("New selector: " + result.selector());
 *     }
 * }
 * }</pre>
 *
 * <p>The RPC client automatically extracts the bundled native binary from the JAR
 * for supported platforms (Linux x86_64, macOS x86_64/aarch64). For Windows or
 * other platforms, set the {@code FRAP_CORE_BIN} environment variable.
 *
 * @see io.github.kotlerdev.frap.core.dto
 * @since 1.0.0
 */
package io.github.kotlerdev.frap.core.client;