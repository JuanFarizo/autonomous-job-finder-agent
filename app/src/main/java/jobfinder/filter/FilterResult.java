package jobfinder.filter;

import jobfinder.source.Job;
import java.util.List;

/** Output of {@link HardFilters#apply}. Warnings are about kept jobs only; they are open questions for the owner. */
public record FilterResult(List<Job> kept, List<Rejected> rejected, List<String> warnings) {}
