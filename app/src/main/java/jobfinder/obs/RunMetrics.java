package jobfinder.obs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

/**
 * Collects what happened in one run: searches, LinkedIn result quality, pipeline funnel, LLM tokens.
 * Every record call also logs one line. The summary is written as JSON to data/runs/ (no personal data, no job text).
 */
public class RunMetrics {

    private static final Logger LOG = LoggerFactory.getLogger(RunMetrics.class);
    private static final JsonMapper MAPPER = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();

    /** One search sent to a source. error is null on success. */
    public record SearchStat(String source, String term, String location, boolean remote, int hoursOld,
                             int requested, int returned, int withDescription, int withDate, int withJobType,
                             int distinctCompanies, long durationMs, String error) {}

    /** One LLM call. estimated=true when the provider reported no usage and tokens are chars/4. */
    public record LlmCallStat(String jobKey, int attempt, long durationMs, int inputTokens, int outputTokens,
                              boolean estimated, boolean parsedOk) {}

    /** Pipeline funnel step: how many jobs went in and out of a stage. */
    public record StageStat(String stage, int in, int out) {}

    private final Instant startedAt = Instant.now();
    private final List<SearchStat> searches = Collections.synchronizedList(new ArrayList<>());
    private final List<LlmCallStat> llmCalls = Collections.synchronizedList(new ArrayList<>());
    private final List<StageStat> stages = Collections.synchronizedList(new ArrayList<>());

    public void addSearch(SearchStat s) {
        searches.add(s);
        if (s.error() == null) {
            LOG.info("search source={} term=\"{}\" location={} remote={} hours_old={} requested={} returned={} "
                            + "with_description={} with_date={} with_job_type={} companies={} ms={}",
                    s.source(), s.term(), s.location(), s.remote(), s.hoursOld(), s.requested(), s.returned(),
                    s.withDescription(), s.withDate(), s.withJobType(), s.distinctCompanies(), s.durationMs());
        } else {
            LOG.warn("search FAILED source={} term=\"{}\" location={} remote={} ms={} error={}",
                    s.source(), s.term(), s.location(), s.remote(), s.durationMs(), s.error());
        }
    }

    public void addLlmCall(LlmCallStat c) {
        llmCalls.add(c);
        LOG.info("llm job={} attempt={} in_tokens={} out_tokens={} estimated={} parsed_ok={} ms={}",
                c.jobKey(), c.attempt(), c.inputTokens(), c.outputTokens(), c.estimated(), c.parsedOk(), c.durationMs());
    }

    public void addStage(String stage, int in, int out) {
        stages.add(new StageStat(stage, in, out));
        LOG.info("stage {} in={} out={} dropped={}", stage, in, out, in - out);
    }

    public Map<String, Object> summary() {
        int inTok = 0, outTok = 0, estimatedCalls = 0, failedParses = 0;
        synchronized (llmCalls) {
            for (LlmCallStat c : llmCalls) {
                inTok += c.inputTokens();
                outTok += c.outputTokens();
                if (c.estimated()) estimatedCalls++;
                if (!c.parsedOk()) failedParses++;
            }
        }
        int returned = 0, failedSearches = 0;
        synchronized (searches) {
            for (SearchStat s : searches) {
                returned += s.returned();
                if (s.error() != null) failedSearches++;
            }
        }
        Map<String, Object> tokens = new LinkedHashMap<>();
        tokens.put("calls", llmCalls.size());
        tokens.put("input_tokens", inTok);
        tokens.put("output_tokens", outTok);
        tokens.put("total_tokens", inTok + outTok);
        tokens.put("calls_with_estimated_tokens", estimatedCalls);
        tokens.put("calls_with_unparsed_answer", failedParses);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("started_at", startedAt.toString());
        m.put("finished_at", Instant.now().toString());
        m.put("searches_total", searches.size());
        m.put("searches_failed", failedSearches);
        m.put("jobs_returned_by_sources", returned);
        m.put("searches", List.copyOf(searches));
        m.put("stages", List.copyOf(stages));
        m.put("tokens", tokens);
        m.put("llm_calls", List.copyOf(llmCalls));
        return m;
    }

    /** Logs a one-line total and writes the JSON summary. Returns the file written. */
    public Path write(Path dir) throws IOException {
        Map<String, Object> m = summary();
        LOG.info("run summary searches={} failed={} jobs_returned={} tokens={}", m.get("searches_total"),
                m.get("searches_failed"), m.get("jobs_returned_by_sources"), m.get("tokens"));
        Files.createDirectories(dir);
        Path file = dir.resolve("run_" + startedAt.toString().replace(":", "-") + ".json");
        Files.writeString(file, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(m));
        return file;
    }

    /** Rough token estimate when the provider reports none. */
    public static int estimateTokens(String text) {
        return text == null ? 0 : (text.length() + 3) / 4;
    }
}
