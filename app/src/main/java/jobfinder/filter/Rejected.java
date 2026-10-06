package jobfinder.filter;

import jobfinder.source.Job;

/** A job dropped by a hard filter, with a stable reason code (e.g. "duplicate", "stale", "language:fr"). */
public record Rejected(Job job, String reason) {}
