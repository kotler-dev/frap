package io.github.kotlerdev.frap.core.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kotlerdev.frap.core.dto.ClusterType;
import io.github.kotlerdev.frap.core.dto.DOMSnapshot;
import io.github.kotlerdev.frap.core.dto.ElementMap;
import io.github.kotlerdev.frap.core.dto.MapOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Java RPC parity with Rust {@code contract_dom_benchmark}.
 */
class ElementMapDomBenchmarkContractTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Path BENCH = Path.of("../../../fixtures/dom-benchmark");

    private FrapCoreClient client;

    @BeforeEach
    void setUp() throws IOException {
        client = new FrapRpcClient(FrapCoreBinaryResolver.resolve());
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "01-catalog-list",
        "02-no-testid-buttons",
        "03-duplicate-labels",
        "04-aria-only",
        "05-shadow-open",
        "06-contenteditable",
        "07-role-button",
        "08-generated-id",
        "09-sibling-label"
    })
    void domBenchmarkPage(String pageId) throws IOException {
        JsonNode snapshotRoot = MAPPER.readTree(
            BENCH.resolve("snapshots").resolve(pageId + ".snapshot.json").toFile()
        );
        JsonNode expected = MAPPER.readTree(
            BENCH.resolve("ground-truth").resolve(pageId + ".expected.json").toFile()
        );

        DOMSnapshot snapshot = MAPPER.treeToValue(snapshotRoot.get("dom_snapshot"), DOMSnapshot.class);
        MapOptions options = snapshotRoot.has("options")
            ? MAPPER.treeToValue(snapshotRoot.get("options"), MapOptions.class)
            : MapOptions.defaults();

        ElementMap map = client.buildElementMap(snapshot, options);

        assertThat(map.elements()).hasSizeGreaterThanOrEqualTo(expected.get("min_elements").asInt());

        int minList = expected.path("min_list_clusters").asInt(0);
        int minSize = expected.path("min_cluster_size").asInt(2);
        long listClusters = map.clusters().stream()
            .filter(c -> c.clusterType() == ClusterType.LIST)
            .filter(c -> c.elementIds().size() >= minSize)
            .count();
        assertThat(listClusters).isGreaterThanOrEqualTo(minList);

        if (expected.has("max_fragile_ratio")) {
            long fragile = map.elements().stream().filter(e -> e.fragile()).count();
            double ratio = map.elements().isEmpty() ? 0 : (double) fragile / map.elements().size();
            assertThat(ratio).isLessThanOrEqualTo(expected.get("max_fragile_ratio").asDouble());
        }
    }
}
