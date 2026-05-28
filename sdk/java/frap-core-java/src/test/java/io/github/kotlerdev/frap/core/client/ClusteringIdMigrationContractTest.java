package io.github.kotlerdev.frap.core.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kotlerdev.frap.core.dto.Candidate;
import io.github.kotlerdev.frap.core.dto.HealRequest;
import io.github.kotlerdev.frap.core.dto.HealResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cross-language contract: id → data-id migration in the same list (fixtures/contract).
 */
class ClusteringIdMigrationContractTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private FrapCoreClient client;

    @BeforeEach
    void setUp() throws IOException {
        client = new FrapRpcClient(FrapCoreBinaryResolver.resolve());
    }

  @org.junit.jupiter.api.AfterEach
  void tearDown() {
    if (client != null) {
      client.close();
    }
  }

    @Test
    void healsWhenLiIdMigratesToDataId() throws IOException {
        Path fixtureDir = Path.of("../../../fixtures/contract/clustering-id-migration");
        HealRequest request = MAPPER.readValue(fixtureDir.resolve("request.json").toFile(), HealRequest.class);
        JsonNode expected = MAPPER.readTree(fixtureDir.resolve("expected.json").toFile());

        HealResult result = client.heal(request);

        assertThat(result.healed()).isEqualTo(expected.get("healed").asBoolean());
        assertThat(result.confidence()).isGreaterThanOrEqualTo(expected.get("min_confidence").asDouble());
        assertThat(result.topCandidates()).isNotEmpty();

        Candidate best = result.topCandidates().get(0);
        String attr = expected.get("best_candidate_attribute").asText();
        String value = expected.get("best_candidate_value").asText();
        assertThat(best.selector()).contains(attr + "=\"" + value + "\"");

        if (result.healed()) {
            assertThat(result.selector()).contains(attr + "=\"" + value + "\"");
        }
    }
}
