package jobfinder.score;

import jobfinder.source.Job;

/** A job that could not be scored, with the reason. */
public record Unscored(Job job, String reason) {}
