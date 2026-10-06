package jobfinder.source;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jobfinder.config.AppConfig;

/** Builds searches from config, fetches raw per search, stores raw, converts to Job. */
public class Collector {

    private final JobSource source;
    private final RawStore store;

    public Collector(JobSource source, RawStore store) {
        this.source = source;
        this.store = store;
    }

    /** Remote-in-Argentina search plus Argentina search. Remote-worldwide search is not defined yet (needs a live test). */
    public static List<SearchQuery> buildQueries(AppConfig c, int resultsWanted, boolean fetchDescription, String jobType) {
        int hours = c.maxAgeDays() * 24;
        List<SearchQuery> out = new ArrayList<>();
        for (String role : c.targetRoles()) {
            out.add(new SearchQuery(role, "Argentina", true, jobType, resultsWanted, hours, fetchDescription));
            if (Boolean.TRUE.equals(c.location().acceptOnsiteInResidence())) {
                out.add(new SearchQuery(role, "Argentina", false, jobType, resultsWanted, hours, fetchDescription));
            }
        }
        return out;
    }

    public List<Job> collect(List<SearchQuery> queries) throws IOException, InterruptedException {
        List<Job> all = new ArrayList<>();
        int n = 0;
        for (SearchQuery q : queries) {
            String raw = source.fetchRaw(q);
            store.save(source.name(), "q" + (++n), raw);
            all.addAll(JobMapper.toJobs(source.name(), raw));
        }
        return all;
    }
}
