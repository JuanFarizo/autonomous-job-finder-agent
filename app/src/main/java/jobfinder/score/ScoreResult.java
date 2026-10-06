package jobfinder.score;

import java.util.List;

public record ScoreResult(List<ScoredJob> scored, List<Unscored> failed) {}
