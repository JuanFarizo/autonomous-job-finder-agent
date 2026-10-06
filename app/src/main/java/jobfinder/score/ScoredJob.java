package jobfinder.score;

import jobfinder.source.Job;

/** Shared contract between scoring, tailoring and tracking. Do not change without the orchestrator. */
public record ScoredJob(Job job, double score, String reason) {}
