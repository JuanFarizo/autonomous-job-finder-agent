package jobfinder.track;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jobfinder.score.ScoredJob;
import jobfinder.source.Job;
import tools.jackson.core.type.TypeReference;

/** Tracker of shortlisted jobs. Status changes only via an explicit setStatus call. */
public class Tracker {

    private static final TypeReference<LinkedHashMap<String, TrackedJob>> TYPE = new TypeReference<>() {};

    private final Path file;
    private final Map<String, TrackedJob> entries;

    public Tracker(Path dataDir) throws IOException {
        this.file = dataDir.resolve("tracker.json");
        this.entries = JsonStore.read(file, TYPE, new LinkedHashMap<>());
    }

    public static String keyOf(Job job) { return job.source() + ":" + job.id(); }

    /** Creates a NEW entry; an existing entry (and its status) is left untouched. */
    public synchronized TrackedJob record(ScoredJob s) throws IOException { return record(s, LocalDate.now()); }

    public synchronized TrackedJob record(ScoredJob s, LocalDate today) throws IOException {
        String key = keyOf(s.job());
        TrackedJob existing = entries.get(key);
        if (existing != null) return existing;
        Job j = s.job();
        TrackedJob created = new TrackedJob(key, j.title(), j.company(), j.url(), s.score(), s.reason(), today, Status.NEW);
        entries.put(key, created);
        save();
        return created;
    }

    public synchronized void setStatus(String key, Status status) throws IOException {
        TrackedJob e = entries.get(key);
        if (e == null) throw new IllegalArgumentException("Unknown job key: " + key);
        entries.put(key, new TrackedJob(e.key(), e.title(), e.company(), e.url(), e.score(), e.reason(), e.firstSeen(), status));
        save();
    }

    public synchronized TrackedJob get(String key) { return entries.get(key); }

    public synchronized List<TrackedJob> all() { return new ArrayList<>(entries.values()); }

    private void save() throws IOException { JsonStore.writeAtomic(file, entries); }
}
