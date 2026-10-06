package jobfinder.source;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/** Saves raw payloads before any parsing, so filters and scoring can be replayed without re-scraping. */
public class RawStore {

    private final Path dir;

    public RawStore(Path dir) {
        this.dir = dir;
    }

    public Path save(String source, String label, String rawJson) throws IOException {
        Files.createDirectories(dir);
        String stamp = Instant.now().toString().replace(":", "-");
        Path file = dir.resolve(source + "_" + label + "_" + stamp + ".json");
        Files.writeString(file, rawJson);
        return file;
    }
}
