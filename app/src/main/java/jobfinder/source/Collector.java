package jobfinder.source;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import jobfinder.config.AppConfig;
import jobfinder.obs.RunMetrics;

/** Builds searches from config, fetches raw per search, stores raw, converts to Job. Records metrics per search. */
public class Collector {

    private final JobSource source;
    private final RawStore store;
    private final RunMetrics metrics;

    public Collector(JobSource source, RawStore store) {
        this(source, store, new RunMetrics());
    }

    public Collector(JobSource source, RawStore store, RunMetrics metrics) {
        this.source = source;
        this.store = store;
        this.metrics = metrics;
    }

    /**
     * Search 1 adds the word remote/remoto to the query. Search 2 is the plain query for jobs based in Argentina.
     * The LinkedIn remote filter (is_remote) and the contract filter (job_type) are NOT sent: in live tests they had no
     * effect (same results with and without; contract returned fulltime rows). Remote is expressed by wording instead.
     */
    public static List<SearchQuery> buildQueries(AppConfig c, int resultsWanted, boolean fetchDescription, String jobType) {
        int hours = c.maxAgeDays() * 24;
        List<SearchQuery> out = new ArrayList<>();
        for (String role : c.targetRoles()) {
            out.add(new SearchQuery(role + " AND (remote OR remoto)", "Argentina", false, jobType, resultsWanted, hours, fetchDescription));
            if (Boolean.TRUE.equals(c.location().acceptOnsiteInResidence())) {
                out.add(new SearchQuery(role, "Argentina", false, jobType, resultsWanted, hours, fetchDescription));
            }
        }
        return out;
    }

    /**
     * A failed search is recorded and skipped so the other searches still run.
     * Throws only if every search failed (blocked or sidecar broken), so the caller can stop safely.
     */
    public List<Job> collect(List<SearchQuery> queries) throws IOException, InterruptedException {
        List<Job> all = new ArrayList<>();
        int n = 0, failures = 0;
        IOException last = null;
        for (SearchQuery q : queries) {
            long t0 = System.nanoTime();
            try {
                String raw = source.fetchRaw(q);
                store.save(source.name(), "q" + (++n), raw);
                List<Job> jobs = JobMapper.toJobs(source.name(), raw);
                all.addAll(jobs);
                metrics.addSearch(stat(q, jobs, ms(t0), null));
            } catch (IOException e) {
                failures++;
                last = e;
                metrics.addSearch(stat(q, List.of(), ms(t0), e.getMessage()));
            }
        }
        if (!queries.isEmpty() && failures == queries.size()) throw last;
        return all;
    }

    private RunMetrics.SearchStat stat(SearchQuery q, List<Job> jobs, long ms, String error) {
        int desc = 0, date = 0, type = 0;
        var companies = new HashSet<String>();
        for (Job j : jobs) {
            if (j.description() != null && !j.description().isBlank()) desc++;
            if (j.datePosted() != null) date++;
            if (j.jobType() != null) type++;
            if (j.company() != null) companies.add(j.company());
        }
        return new RunMetrics.SearchStat(source.name(), q.searchTerm(), q.location(), q.remote(), q.hoursOld(),
                q.resultsWanted(), jobs.size(), desc, date, type, companies.size(), ms, error);
    }

    private static long ms(long t0) {
        return (System.nanoTime() - t0) / 1_000_000;
    }
}
