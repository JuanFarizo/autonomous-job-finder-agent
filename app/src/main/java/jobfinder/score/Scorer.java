package jobfinder.score;

import java.util.ArrayList;
import java.util.List;

import jobfinder.obs.RunMetrics;
import jobfinder.profile.Profile;
import jobfinder.source.Job;

/** Scores jobs one by one. A failing job is recorded as unscored; it never aborts the batch. */
public final class Scorer {

    private Scorer() {}

    public static ScoreResult score(List<Job> jobs, Profile profile, ScoreModel model) {
        return score(jobs, profile, model, new RunMetrics());
    }

    public static ScoreResult score(List<Job> jobs, Profile profile, ScoreModel model, RunMetrics metrics) {
        String system = PromptBuilder.systemPrompt();
        var scored = new ArrayList<ScoredJob>();
        var failed = new ArrayList<Unscored>();
        for (Job job : jobs) {
            String user = PromptBuilder.userPrompt(job, profile);
            String lastError = null;
            ParsedScore parsed = null;
            for (int attempt = 0; attempt < 2 && parsed == null; attempt++) { // one retry
                long t0 = System.nanoTime();
                ModelAnswer a = null;
                boolean ok = false;
                try {
                    a = model.call(system, user);
                    parsed = ScoreParser.parse(a == null ? null : a.text(), profile);
                    ok = true;
                } catch (RuntimeException e) {
                    lastError = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                }
                boolean reported = a != null && a.inputTokens() != null && a.outputTokens() != null;
                metrics.addLlmCall(new RunMetrics.LlmCallStat(job.source() + ":" + job.id(), attempt + 1,
                        (System.nanoTime() - t0) / 1_000_000,
                        reported ? a.inputTokens() : RunMetrics.estimateTokens(system + user),
                        reported ? a.outputTokens() : RunMetrics.estimateTokens(a == null ? null : a.text()),
                        !reported, ok));
            }
            if (parsed != null) scored.add(new ScoredJob(job, parsed.score(), parsed.reason()));
            else failed.add(new Unscored(job, lastError));
        }
        return new ScoreResult(scored, failed);
    }
}
