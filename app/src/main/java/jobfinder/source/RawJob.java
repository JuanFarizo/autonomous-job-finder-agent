package jobfinder.source;

/** Fields we read from a raw JobSpy row. The full raw payload is stored separately, untouched. */
public record RawJob(String id, String jobUrl, String title, String company, String location,
                     String datePosted, Boolean isRemote, String jobType, String jobLevel, String description) {}
