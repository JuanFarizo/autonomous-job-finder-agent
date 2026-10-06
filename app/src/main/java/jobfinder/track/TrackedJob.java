package jobfinder.track;

import java.time.LocalDate;

public record TrackedJob(String key, String title, String company, String url, double score, String reason,
                         LocalDate firstSeen, Status status) {}
