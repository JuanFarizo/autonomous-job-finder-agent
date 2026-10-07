package jobfinder.pipeline;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jobfinder.config.JsonFiles;
import jobfinder.obs.RunMetrics;
import jobfinder.profile.Profile;
import jobfinder.score.ModelAnswer;
import jobfinder.source.*;
import org.junit.jupiter.api.Test;

class PipelineTest {

    static final Profile PROFILE = new Profile("Ana Perez", "Java Developer", "Backend dev",
            List.of(new Profile.Skill("Java", null, null, List.of("pp1"))),
            List.of(new Profile.Experience("Acme", "Dev", "2022-01", null, List.of("Built Java services"))),
            List.of(new Profile.ProofPoint("pp1", "Built Java services at scale", "cv", true)), List.of("ana@example.test"));

    static String row(String id, String title, String date) {
        return "{\"id\":\"" + id + "\",\"job_url\":\"https://x.test/" + id + "\",\"title\":\"" + title + "\",\"company\":\"Co" + id
                + "\",\"location\":\"Argentina\",\"date_posted\":\"" + date + "\",\"is_remote\":true,\"description\":\"Java backend work, remote, Argentina\"}";
    }

    static final String RAW = "[" + row("1", "Senior Java Developer", "2026-10-05T00:00:00.000") + ","
            + row("2", "Junior Java Developer", "2026-10-05T00:00:00.000") + ","
            + row("3", "Senior Java Engineer", "2026-09-01T00:00:00.000") + "]";

    static final String ANSWER = "{\"score\":4.5,\"dimensions\":{\"cvMatch\":5,\"northStar\":4,\"comp\":4,\"culture\":4,\"redFlags\":5},\"reason\":\"Good Java fit\",\"evidence\":[\"pp1\"]}";

    @Test
    void endToEndWithFakesFiltersScoresTailorsAndNeverRepeatsAJob() throws Exception {
        var config = JsonFiles.loadConfig(Path.of("../config/config.json"));
        Path data = Files.createTempDirectory("data");
        var calls = new ArrayList<String>();
        JobSource source = new JobSource() {
            public String name() { return "linkedin"; }
            public String fetchRaw(SearchQuery q) { calls.add(q.searchTerm()); return RAW; }
        };
        var today = LocalDate.of(2026, 10, 6);

        var metrics = new RunMetrics();
        var result = new Pipeline(config, PROFILE, source, (s, u) -> new ModelAnswer(ANSWER, 800, 60), data, metrics).run(today);

        assertEquals(2, calls.size());                 // remote + Argentina searches
        assertEquals(1, result.shown().size());        // junior and stale rejected, duplicate across searches removed
        assertEquals("Senior Java Developer", result.shown().get(0).job().title());
        assertEquals(1, result.cvCount());
        assertTrue(Files.size(data.resolve("cv/linkedin_1.pdf")) > 1000);
        assertTrue(Files.readString(result.shortlist()).contains("Senior Java Developer"));
        assertTrue(Files.readString(result.metricsFile()).contains("\"input_tokens\" : 800"));

        // second run: same postings are already tracked, nothing is scored or shown again
        var again = new Pipeline(config, PROFILE, source, (s, u) -> { throw new AssertionError("model must not be called"); }, data, new RunMetrics()).run(today);
        assertEquals(0, again.shown().size());
    }

    @Test
    void retryingSourceBacksOffThenGivesUp() {
        var delays = new ArrayList<Long>();
        int[] n = {0};
        JobSource flaky = new JobSource() {
            public String name() { return "linkedin"; }
            public String fetchRaw(SearchQuery q) throws IOException { n[0]++; throw new IOException("429"); }
        };
        var src = new RetryingSource(flaky, 3, 100, delays::add);
        var e = assertThrows(IOException.class, () -> src.fetchRaw(new SearchQuery("x", null, false, null, 1, 24, false)));
        assertEquals(3, n[0]);
        assertEquals(List.of(100L, 200L), delays);
        assertTrue(e.getMessage().contains("3 attempts"));
    }

    @Test
    void retryingSourceReturnsOnSuccessWithoutSleeping() throws Exception {
        var delays = new ArrayList<Long>();
        JobSource ok = new JobSource() {
            public String name() { return "linkedin"; }
            public String fetchRaw(SearchQuery q) { return "[]"; }
        };
        assertEquals("[]", new RetryingSource(ok, 3, 100, delays::add).fetchRaw(new SearchQuery("x", null, false, null, 1, 24, false)));
        assertTrue(delays.isEmpty());
    }
}
