package jobfinder.source;

/** One search sent to a source. location and jobType may be null. */
public record SearchQuery(String searchTerm, String location, boolean remote, String jobType,
                          int resultsWanted, int hoursOld, boolean fetchDescription) {}
