package jobfinder.source;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.json.JsonMapper;

/** One JobSpy site (linkedin, indeed, glassdoor) through the JobSpy Python sidecar, launched as a subprocess (Q4 decision: subprocess, JSON in/out). */
public class JobSpySource implements JobSource {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final String site;
    private final List<String> command;

    public JobSpySource(String site, List<String> command) {
        this.site = site;
        this.command = command;
    }

    @Override
    public String name() {
        return site;
    }

    @Override
    public String fetchRaw(SearchQuery q) throws IOException, InterruptedException {
        Map<String, Object> in = new LinkedHashMap<>();
        in.put("site", site);
        in.put("search_term", q.searchTerm());
        in.put("location", q.location());
        in.put("is_remote", q.remote());
        in.put("job_type", q.jobType());
        in.put("results_wanted", q.resultsWanted());
        in.put("hours_old", q.hoursOld());
        in.put("fetch_description", q.fetchDescription());

        Process p = new ProcessBuilder(command).start();
        p.getOutputStream().write(MAPPER.writeValueAsBytes(in));
        p.getOutputStream().close();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String err = new String(p.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        int code = p.waitFor();
        if (code != 0) throw new IOException("sidecar exited " + code + ": " + err.strip());
        return out;
    }
}
