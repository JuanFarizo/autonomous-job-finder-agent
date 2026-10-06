package jobfinder.track;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

/** Plain JSON file persistence with atomic replace (write temp file, then move). */
final class JsonStore {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();

    private JsonStore() {}

    static <T> T read(Path file, TypeReference<T> type, T empty) throws IOException {
        if (!Files.exists(file)) return empty;
        return MAPPER.readValue(Files.readString(file), type);
    }

    static void writeAtomic(Path file, Object value) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value));
        try {
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
