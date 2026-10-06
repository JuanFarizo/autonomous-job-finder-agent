package jobfinder.score;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public final class Ranker {

    /**
     * Fallback threshold used only when the owner has not set minScore (D24: skipped for now).
     * Not an owner decision: 3.5 is the "acceptable with caveats" line in the scoring prompt. Orchestrator to confirm.
     */
    public static final double DEFAULT_MIN_SCORE = 3.5;

    private Ranker() {}

    /** Keeps score >= minScore; sorts by score descending, then datePosted newest first (null dates last). */
    public static List<ScoredJob> rank(List<ScoredJob> jobs, double minScore) {
        return jobs.stream()
                .filter(j -> j.score() >= minScore)
                .sorted(Comparator.comparingDouble(ScoredJob::score).reversed()
                        .thenComparing((ScoredJob j) -> j.job().datePosted(),
                                Comparator.nullsLast(Comparator.<LocalDate>reverseOrder())))
                .toList();
    }
}
