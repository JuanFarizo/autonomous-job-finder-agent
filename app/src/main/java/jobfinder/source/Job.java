package jobfinder.source;

import java.time.LocalDate;

/** Internal job model, independent of any source. */
public record Job(String source, String id, String url, String title, String company, String location,
                  LocalDate datePosted, Boolean remoteHint, String jobType, String jobLevel, String description) {}
