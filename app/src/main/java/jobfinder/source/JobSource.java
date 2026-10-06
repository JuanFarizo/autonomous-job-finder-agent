package jobfinder.source;

import java.io.IOException;

/** A place jobs come from. Returns the raw payload exactly as received, so it can be stored before parsing. */
public interface JobSource {

    String name();

    String fetchRaw(SearchQuery query) throws IOException, InterruptedException;
}
