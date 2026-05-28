/**
 * Data Transfer Objects (DTOs) for Frap Core API.
 *
 * <p>This package contains all request/response types used by the Frap Core client:
 * <ul>
 *   <li><strong>Healing:</strong> {@link HealRequest}, {@link HealResult}, {@link Candidate}, {@link Signature}</li>
 *   <li><strong>DOM:</strong> {@link DOMSnapshot}, {@link DOMElementInfo}, {@link DOMToken}</li>
 *   <li><strong>Discovery:</strong> {@link ElementMap}, {@link ElementNode}, {@link Cluster}, {@link ClusterType}</li>
 *   <li><strong>Page Object:</strong> {@link GenerateOptions}, {@link GeneratedArtifact}, {@link GeneratedFile}</li>
 *   <li><strong>RCA:</strong> RcaReport (in io.github.kotlerdev.frap.core.rca)</li>
 * </ul>
 *
 * <p>All DTOs are immutable records with Jackson annotations for JSON serialization.
 * They mirror the JSON-RPC protocol types used by the Rust core.
 *
 * <p><strong>Example:</strong>
 * <pre>{@code
 * HealRequest request = new HealRequest(
 *     "[data-testid='pay-btn']",
 *     originalSignature,
 *     currentSnapshot,
 *     0.85
 * );
 * }</pre>
 *
 * @see io.github.kotlerdev.frap.core.client.FrapCoreClient
 * @since 1.0.0
 */
package io.github.kotlerdev.frap.core.dto;