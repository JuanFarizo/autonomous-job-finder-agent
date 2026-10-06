package jobfinder.track;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jobfinder.score.ScoredJob;
import tools.jackson.core.type.TypeReference;

/** Remembers which postings were already shown so none is shown twice. */
public class SeenJobs {

    private static final TypeReference<LinkedHashSet<String>> TYPE = new TypeReference<>() {};

    private final Path file;
    private final Set<String> seen;

    public SeenJobs(Path dataDir) throws IOException {
        this.file = dataDir.resolve("seen_jobs.json");
        this.seen = JsonStore.read(file, TYPE, new LinkedHashSet<>());
    }

    /** Jobs never shown before, in input order, without duplicates inside the input. Does not mark them. */
    public synchronized List<ScoredJob> filterUnseen(List<ScoredJob> jobs) {
        Set<String> batch = new LinkedHashSet<>();
        List<ScoredJob> out = new ArrayList<>();
        for (ScoredJob s : jobs) {
            String key = Tracker.keyOf(s.job());
            if (!seen.contains(key) && batch.add(key)) out.add(s);
        }
        return out;
    }

    public synchronized void markShown(List<ScoredJob> jobs) throws IOException {
        boolean changed = false;
        for (ScoredJob s : jobs) changed |= seen.add(Tracker.keyOf(s.job()));
        if (changed) JsonStore.writeAtomic(file, seen);
    }

    public synchronized boolean isSeen(String key) { return seen.contains(key); }
}
