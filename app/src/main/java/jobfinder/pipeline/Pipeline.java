package jobfinder.pipeline;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import jobfinder.config.AppConfig;
import jobfinder.filter.HardFilters;
import jobfinder.obs.RunMetrics;
import jobfinder.profile.Profile;
import jobfinder.score.Ranker;
import jobfinder.score.ScoreModel;
import jobfinder.score.ScoredJob;
import jobfinder.score.Scorer;
import jobfinder.source.Collector;
import jobfinder.source.Job;
import jobfinder.source.JobSource;
import jobfinder.source.RawStore;
import jobfinder.tailor.CvPdf;
import jobfinder.tailor.Tailorer;
import jobfinder.track.SeenJobs;
import jobfinder.track.ShortlistReport;
import jobfinder.track.Tracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * collect -> hard filters -> skip already tracked -> score -> rank -> track -> tailor PDFs -> shortlist.
 * Discovery only: nothing here applies to a job.
 */
public class Pipeline {

    private static final Logger LOG = LoggerFactory.getLogger(Pipeline.class);
    static final int DEFAULT_RESULTS_PER_SEARCH = 10;

    public record Result(Path shortlist, List<ScoredJob> shown, int cvCount, Path metricsFile) {}

    private final AppConfig config;
    private final Profile profile;
    private final JobSource source;
    private final ScoreModel model;
    private final Path dataDir;
    private final RunMetrics metrics;

    public Pipeline(AppConfig config, Profile profile, JobSource source, ScoreModel model, Path dataDir, RunMetrics metrics) {
        this.config = config;
        this.profile = profile;
        this.source = source;
        this.model = model;
        this.dataDir = dataDir;
        this.metrics = metrics;
    }

    public Result run(LocalDate today) throws IOException, InterruptedException {
        int perSearch = config.resultsPerSearch() == null ? DEFAULT_RESULTS_PER_SEARCH : config.resultsPerSearch();
        double minScore = config.minScore() == null ? Ranker.DEFAULT_MIN_SCORE : config.minScore();
        if (config.minScore() == null) LOG.warn("min_score not set by owner, using placeholder {}", minScore);

        var collector = new Collector(source, new RawStore(dataDir.resolve("raw")), metrics);
        List<Job> collected = collector.collect(Collector.buildQueries(config, perSearch, true, null));
        metrics.addStage("collected", collected.size(), collected.size());

        var filtered = HardFilters.apply(collected, config, today);
        metrics.addStage("hard-filters", collected.size(), filtered.kept().size());
        filtered.warnings().forEach(w -> LOG.info("filter warning: {}", w));

        var tracker = new Tracker(dataDir);
        var seen = new SeenJobs(dataDir);
        List<Job> fresh = filtered.kept().stream().filter(j -> tracker.get(Tracker.keyOf(j)) == null).toList();
        metrics.addStage("not-yet-tracked", filtered.kept().size(), fresh.size());

        var scoring = Scorer.score(fresh, profile, model, metrics);
        metrics.addStage("scored", fresh.size(), scoring.scored().size());
        scoring.failed().forEach(u -> LOG.warn("unscored {}:{} reason={}", u.job().source(), u.job().id(), u.reason()));

        List<ScoredJob> ranked = Ranker.rank(scoring.scored(), minScore);
        metrics.addStage("ranked", scoring.scored().size(), ranked.size());
        for (ScoredJob s : ranked) tracker.record(s, today);

        List<ScoredJob> toShow = seen.filterUnseen(ranked);
        java.util.Set<String> cvFiles = new java.util.HashSet<>();
        for (ScoredJob s : toShow) {
            String name = s.job().source() + "_" + s.job().id() + ".pdf";
            try {
                CvPdf.write(Tailorer.tailor(s.job(), profile), profile, dataDir.resolve("cv").resolve(name));
                cvFiles.add(name);
            } catch (IOException | IllegalStateException e) {
                LOG.warn("CV not written for {}:{}: {}", s.job().source(), s.job().id(), e.getMessage());
            }
        }
        int cvs = cvFiles.size();
        metrics.addStage("cv-pdfs", toShow.size(), cvs);

        Path report = dataDir.resolve("shortlist_" + today + ".md");
        ShortlistReport.write(report, toShow, today, cvFiles);
        seen.markShown(toShow);
        return new Result(report, toShow, cvs, metrics.write(dataDir.resolve("runs")));
    }
}
