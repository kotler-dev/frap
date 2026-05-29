package io.github.kotlerdev.frap.core.snapshot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.github.kotlerdev.frap.core.dto.DOMSnapshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persists DOM snapshots as JSON files (SDK-only; Core remains stateless).
 */
public final class SnapshotStore {

    private final ObjectMapper mapper;

    public SnapshotStore() {
        this.mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    public void save(DOMSnapshot snapshot, Path path) throws IOException {
        Files.createDirectories(path.getParent() != null ? path.getParent() : Path.of("."));
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), snapshot);
    }

    public DOMSnapshot load(Path path) throws IOException {
        return mapper.readValue(path.toFile(), DOMSnapshot.class);
    }
}
