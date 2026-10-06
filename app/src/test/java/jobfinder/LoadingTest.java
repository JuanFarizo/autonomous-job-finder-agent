package jobfinder;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;

import jobfinder.config.JsonFiles;
import org.junit.jupiter.api.Test;

class LoadingTest {

    @Test
    void loadsRealConfig() throws Exception {
        var c = JsonFiles.loadConfig(Path.of("../config/config.json"));
        assertEquals(7, c.maxAgeDays());
        assertEquals(java.util.List.of("linkedin"), c.sources());
        assertNull(c.location().acceptOnsiteInResidence());
    }

    @Test
    void loadsProfile() throws Exception {
        var p = JsonFiles.loadProfile(Path.of("src/test/resources/profile-test.json"));
        assertEquals(1, p.proofPoints().size());
    }

    @Test
    void rejectsUngroundedProofPoint() throws Exception {
        Path bad = Files.createTempFile("profile", ".json");
        Files.writeString(bad, Files.readString(Path.of("src/test/resources/profile-test.json")).replace("\"source\":\"cv\"", "\"source\":\"invented\""));
        assertThrows(IllegalStateException.class, () -> JsonFiles.loadProfile(bad));
    }
}
