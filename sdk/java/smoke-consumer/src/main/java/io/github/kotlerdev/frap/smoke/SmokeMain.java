package io.github.kotlerdev.frap.smoke;

import io.github.kotlerdev.frap.core.client.FrapCoreClient;
import io.github.kotlerdev.frap.core.client.FrapRpcClient;
import io.github.kotlerdev.frap.core.dto.DOMElementInfo;
import io.github.kotlerdev.frap.core.dto.DOMSnapshot;
import io.github.kotlerdev.frap.core.dto.ElementMap;
import io.github.kotlerdev.frap.core.dto.GenerateOptions;
import io.github.kotlerdev.frap.core.dto.GeneratedArtifact;
import io.github.kotlerdev.frap.core.dto.MapOptions;

import java.util.List;
import java.util.Map;

/**
 * Minimal smoke: Maven-only consumer of frap-core-java (install locally first).
 */
public final class SmokeMain {

    public static void main(String[] args) throws Exception {
        try (FrapCoreClient client = FrapRpcClient.create()) {
            DOMSnapshot snapshot = new DOMSnapshot(
                "<button data-testid=\"ok\">OK</button>",
                List.of(
                    new DOMElementInfo(
                        "[data-testid=\"ok\"]",
                        "button",
                        Map.of("data-testid", "ok"),
                        "OK",
                        List.of("button:-"),
                        null
                    )
                )
            );

            ElementMap map = client.buildElementMap(snapshot, MapOptions.defaults());
            if (map.elements().isEmpty()) {
                throw new IllegalStateException("expected elements in map");
            }

            GeneratedArtifact po = client.generatePageObject(
                map,
                GenerateOptions.javaPlaywright("SmokePage", "com.example")
            );
            if (po.files().isEmpty() || !po.files().get(0).content().contains("SmokePage")) {
                throw new IllegalStateException("Page Object generation failed");
            }

            System.out.println("frap smoke OK: " + map.elements().size() + " elements");
        }
    }
}
