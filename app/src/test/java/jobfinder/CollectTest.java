package jobfinder;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import jobfinder.config.JsonFiles;
import jobfinder.source.*;
import org.junit.jupiter.api.Test;

class CollectTest {

    private static final String RAW = read("src/test/resources/raw-sample.json");

    private static String read(String p) {
        try { return Files.readString(Path.of(p)); } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void mapsRawToJobsIgnoringUnknownFields() {
        List<Job> jobs = JobMapper.toJobs("linkedin", RAW);
        assertEquals(2, jobs.size());
        assertEquals(LocalDate.of(2026, 10, 5), jobs.get(0).datePosted());
        assertNull(jobs.get(1).datePosted());
        assertEquals("contract", jobs.get(0).jobType());
    }

    @Test
    void buildsRemoteAndArgentinaQueriesWithOwnFreshnessWindow() throws Exception {
        var cfg = JsonFiles.loadConfig(Path.of("../config/config.json"));
        var qs = Collector.buildQueries(cfg, 10, false, null);
        assertEquals(2, qs.size());
        assertTrue(qs.get(0).remote());
        assertFalse(qs.get(1).remote());
        assertEquals(168, qs.get(0).hoursOld());
    }

    @Test
    void storesRawBeforeParsing() throws Exception {
        Path dir = Files.createTempDirectory("raw");
        JobSource fake = new JobSource() {
            public String name() { return "linkedin"; }
            public String fetchRaw(SearchQuery q) { return RAW; }
        };
        var jobs = new Collector(fake, new RawStore(dir)).collect(List.of(new SearchQuery("x", "Argentina", true, null, 10, 168, false)));
        assertEquals(2, jobs.size());
        try (var s = Files.list(dir)) { assertEquals(1, s.count()); }
    }

    @Test
    void sidecarFailureSurfacesAsException() {
        var src = new JobSpyLinkedInSource(List.of("sh", "-c", "echo boom >&2; exit 2"));
        var e = assertThrows(java.io.IOException.class, () -> src.fetchRaw(new SearchQuery("x", null, false, null, 1, 24, false)));
        assertTrue(e.getMessage().contains("boom"));
    }
}
