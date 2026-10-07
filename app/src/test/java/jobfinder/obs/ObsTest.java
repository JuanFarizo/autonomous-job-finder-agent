package jobfinder.obs;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import jobfinder.profile.Profile;
import jobfinder.score.ModelAnswer;
import jobfinder.score.Scorer;
import jobfinder.source.*;
import org.junit.jupiter.api.Test;

class ObsTest {

    private static final String RAW = "[{\"id\":\"1\",\"job_url\":\"u\",\"title\":\"t\",\"company\":\"A\",\"date_posted\":\"2026-10-05T00:00:00.000\",\"description\":\"d\"},"
            + "{\"id\":\"2\",\"job_url\":\"u2\",\"title\":\"t\",\"company\":\"A\",\"date_posted\":null,\"description\":null}]";

    @SuppressWarnings("unchecked")
    @Test
    void searchStatsCountFieldFillAndFailuresAreRecordedAndWritten() throws Exception {
        var metrics = new RunMetrics();
        JobSource src = new JobSource() {
            public String name() { return "linkedin"; }
            public String fetchRaw(SearchQuery q) throws IOException {
                if (q.remote()) throw new IOException("blocked 429");
                return RAW;
            }
        };
        var c = new Collector(src, new RawStore(Files.createTempDirectory("raw")), metrics);
        var jobs = c.collect(List.of(new SearchQuery("x", "AR", true, null, 10, 168, false),
                new SearchQuery("x", "AR", false, null, 10, 168, false)));
        assertEquals(2, jobs.size());

        Map<String, Object> s = metrics.summary();
        assertEquals(2, s.get("searches_total"));
        assertEquals(1, s.get("searches_failed"));
        var stats = (List<RunMetrics.SearchStat>) s.get("searches");
        assertEquals("blocked 429", stats.get(0).error());
        assertEquals(2, stats.get(1).returned());
        assertEquals(1, stats.get(1).withDescription());
        assertEquals(1, stats.get(1).withDate());
        assertEquals(1, stats.get(1).distinctCompanies());

        Path file = metrics.write(Files.createTempDirectory("runs"));
        assertTrue(Files.readString(file).contains("\"searches_failed\" : 1"));
    }

    @Test
    void allSearchesFailingThrows() {
        JobSource src = new JobSource() {
            public String name() { return "linkedin"; }
            public String fetchRaw(SearchQuery q) throws IOException { throw new IOException("down"); }
        };
        var c = new Collector(src, new RawStore(Path.of("unused")));
        assertThrows(IOException.class, () -> c.collect(List.of(new SearchQuery("x", null, false, null, 1, 24, false))));
    }

    @SuppressWarnings("unchecked")
    @Test
    void tokensUseProviderUsageOrEstimate() {
        var profile = new Profile("n", "h", "s", List.of(), List.of(), List.of(new Profile.ProofPoint("pp1", "c", "cv", true)), null);
        var job = new Job("linkedin", "1", "u", "t", "c", "l", null, null, null, null, "d");
        String ok = "{\"score\":4,\"dimensions\":{\"cvMatch\":4,\"northStar\":4,\"comp\":4,\"culture\":4,\"redFlags\":4},\"reason\":\"r\",\"evidence\":[\"pp1\"]}";
        var metrics = new RunMetrics();
        Scorer.score(List.of(job), profile, (sys, user) -> new ModelAnswer(ok, 1000, 50), metrics);
        Scorer.score(List.of(job), profile, (sys, user) -> new ModelAnswer(ok), metrics);
        Scorer.score(List.of(job), profile, (sys, user) -> new ModelAnswer("not json", 10, 5), metrics);

        var tokens = (Map<String, Object>) metrics.summary().get("tokens");
        assertEquals(1000 + 10 * 2, ((int) tokens.get("input_tokens")) - estimatedInput(profile, job));
        assertEquals(1, tokens.get("calls_with_estimated_tokens"));
        assertEquals(2, tokens.get("calls_with_unparsed_answer")); // bad answer tried twice
    }

    private static int estimatedInput(Profile p, Job j) {
        return RunMetrics.estimateTokens(jobfinder.score.PromptBuilder.systemPrompt() + jobfinder.score.PromptBuilder.userPrompt(j, p));
    }
}
