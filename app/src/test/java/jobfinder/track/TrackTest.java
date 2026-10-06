package jobfinder.track;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import jobfinder.score.ScoredJob;
import jobfinder.source.Job;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TrackTest {

    @TempDir Path dir;

    static ScoredJob sj(String id, String title, double score) {
        return new ScoredJob(new Job("jobspy", id, "https://x.com/" + id, title, "Acme_Co", "Remote",
                LocalDate.of(2026, 10, 1), true, null, null, "d"), score, "good *fit*");
    }

    @Test
    void recordCreatesNewAndStatusOnlyChangesExplicitly() throws Exception {
        Tracker t = new Tracker(dir);
        t.record(sj("1", "Dev", 8), LocalDate.of(2026, 10, 6));
        assertEquals(Status.NEW, t.get("jobspy:1").status());
        assertEquals(LocalDate.of(2026, 10, 6), t.get("jobspy:1").firstSeen());
        t.record(sj("1", "Dev", 9));
        assertEquals(Status.NEW, t.get("jobspy:1").status());
        t.setStatus("jobspy:1", Status.APPLIED_MANUALLY);
        t.record(sj("1", "Dev", 9));
        assertEquals(Status.APPLIED_MANUALLY, t.get("jobspy:1").status());
        assertThrows(IllegalArgumentException.class, () -> t.setStatus("nope", Status.REVIEWED));
    }

    @Test
    void trackerPersistsAcrossInstances() throws Exception {
        Tracker t = new Tracker(dir);
        t.record(sj("1", "Dev", 8));
        t.setStatus("jobspy:1", Status.REVIEWED);
        Tracker t2 = new Tracker(dir);
        TrackedJob e = t2.get("jobspy:1");
        assertEquals(Status.REVIEWED, e.status());
        assertEquals("Dev", e.title());
        assertEquals(8.0, e.score());
        assertFalse(Files.exists(dir.resolve("tracker.json.tmp")));
    }

    @Test
    void seenJobsNotShownTwice() throws Exception {
        SeenJobs seen = new SeenJobs(dir);
        List<ScoredJob> day1 = List.of(sj("1", "A", 5), sj("2", "B", 6), sj("1", "A", 5));
        List<ScoredJob> shown = seen.filterUnseen(day1);
        assertEquals(2, shown.size());
        seen.markShown(shown);
        SeenJobs reloaded = new SeenJobs(dir);
        List<ScoredJob> day2 = reloaded.filterUnseen(List.of(sj("1", "A", 5), sj("3", "C", 7)));
        assertEquals(1, day2.size());
        assertEquals("3", day2.get(0).job().id());
    }

    @Test
    void reportIsReadableMarkdownInOrderAndEscaped() throws Exception {
        List<ScoredJob> jobs = List.of(sj("1", "Java [Dev]", 9.25), sj("2", "Other", 7));
        String md = ShortlistReport.render(jobs, LocalDate.of(2026, 10, 6));
        assertTrue(md.startsWith("# Job shortlist 2026-10-06"));
        assertTrue(md.contains("Jobs: 2"));
        assertTrue(md.contains("Java \\[Dev\\] at Acme\\_Co"));
        assertTrue(md.contains("good \\*fit\\*"));
        assertTrue(md.contains("<https://x.com/1>"));
        assertTrue(md.contains("Posted: 2026-10-01"));
        assertTrue(md.indexOf("Java") < md.indexOf("Other"));
        Path out = dir.resolve("sub/report.md");
        ShortlistReport.write(out, jobs, LocalDate.of(2026, 10, 6));
        assertEquals(md, Files.readString(out));
    }
}
