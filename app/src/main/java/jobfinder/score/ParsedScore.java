package jobfinder.score;

import java.util.List;
import java.util.Map;

/** Validated model answer. */
public record ParsedScore(double score, Map<String, Double> dimensions, String reason, List<String> evidence) {}
