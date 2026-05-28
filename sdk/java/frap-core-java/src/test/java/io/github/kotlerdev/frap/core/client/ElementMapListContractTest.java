package io.github.kotlerdev.frap.core.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kotlerdev.frap.core.dto.ClusterType;
import io.github.kotlerdev.frap.core.dto.DOMSnapshot;
import io.github.kotlerdev.frap.core.dto.ElementMap;
import io.github.kotlerdev.frap.core.dto.MapOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ElementMapListContractTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
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

    @Test
    void buildElementMapFindsListClusters() throws IOException {
        Path fixtureDir = Path.of("../../../fixtures/contract/element-map-list");
        JsonNode root = MAPPER.readTree(fixtureDir.resolve("request.json").toFile());
        JsonNode expected = MAPPER.readTree(fixtureDir.resolve("expected.json").toFile());

        DOMSnapshot snapshot = MAPPER.treeToValue(root.get("dom_snapshot"), DOMSnapshot.class);
        MapOptions options = MAPPER.treeToValue(root.get("options"), MapOptions.class);

        ElementMap map = client.buildElementMap(snapshot, options);

        assertThat(map.elements()).hasSizeGreaterThanOrEqualTo(expected.get("min_elements").asInt());

        long listClusters = map.clusters().stream()
            .filter(c -> c.clusterType() == ClusterType.LIST)
            .filter(c -> c.elementIds().size() >= expected.get("min_cluster_size").asInt())
            .count();

        assertThat(listClusters).isGreaterThanOrEqualTo(expected.get("min_list_clusters").asInt());
    }

}
