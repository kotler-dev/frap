package io.github.kotlerdev.frap.core.client;

import io.github.kotlerdev.frap.core.context.ContextTimeline;
import io.github.kotlerdev.frap.core.dto.DOMSnapshot;
import io.github.kotlerdev.frap.core.dto.ElementMap;
import io.github.kotlerdev.frap.core.dto.FilterSpec;
import io.github.kotlerdev.frap.core.dto.GeneratedArtifact;
import io.github.kotlerdev.frap.core.dto.GenerateOptions;
import io.github.kotlerdev.frap.core.dto.HealRequest;
import io.github.kotlerdev.frap.core.dto.HealResult;
import io.github.kotlerdev.frap.core.dto.MapOptions;
import io.github.kotlerdev.frap.core.rca.RcaReport;

import java.io.IOException;

/**
 * Common interface for Frap Core clients (RPC and Native).
 * Implementations include {@link FrapRpcClient} (subprocess RPC).
 * JNI implementation lives in the optional `frap-core-native` module.
 */
public interface FrapCoreClient extends AutoCloseable {

    /**
     * Performs healing analysis.
     *
     * @param request heal request with selector, signature, and DOM snapshot
     * @return heal result with confidence and candidate selectors
     * @throws IOException if communication fails
     */
    HealResult heal(HealRequest request) throws IOException;

    /**
     * Performs root cause analysis on a timeline.
     *
     * @param timeline context timeline with events
     * @param failureAtMs failure timestamp (0 for auto-detect)
     * @return RCA report with primary cause and recommendation
     * @throws IOException if communication fails
     */
    RcaReport analyzeRca(ContextTimeline timeline, long failureAtMs) throws IOException;

    /**
     * Builds an element map with clusters from a DOM snapshot.
     */
    ElementMap buildElementMap(DOMSnapshot snapshot, MapOptions options) throws IOException;

    /**
     * Filters an element map by interactive tags, cluster size, etc.
     */
    ElementMap filterElementMap(ElementMap map, FilterSpec spec) throws IOException;

    /**
     * Generates Page Object source files from an element map.
     */
    GeneratedArtifact generatePageObject(ElementMap map, GenerateOptions options) throws IOException;

    /**
     * Checks if the client is still connected/available.
     */
    boolean isAlive();

    /**
     * Returns the process ID (for RPC clients) or current process ID (for native clients).
     */
    long pid();

    @Override
    void close();
}
