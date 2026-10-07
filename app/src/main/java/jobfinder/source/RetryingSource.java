package jobfinder.source;

import java.io.IOException;
import java.util.function.LongConsumer;

/** Retries a failing source with exponential backoff, then gives up (safe failure when blocked: no endless hammering). */
public class RetryingSource implements JobSource {

    private final JobSource inner;
    private final int maxAttempts;
    private final long baseDelayMs;
    private final LongConsumer sleeper;

    public RetryingSource(JobSource inner, int maxAttempts, long baseDelayMs, LongConsumer sleeper) {
        this.inner = inner;
        this.maxAttempts = maxAttempts;
        this.baseDelayMs = baseDelayMs;
        this.sleeper = sleeper;
    }

    public static RetryingSource withRealSleep(JobSource inner, int maxAttempts, long baseDelayMs) {
        return new RetryingSource(inner, maxAttempts, baseDelayMs, ms -> {
            try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
    }

    @Override
    public String name() {
        return inner.name();
    }

    @Override
    public String fetchRaw(SearchQuery q) throws IOException, InterruptedException {
        IOException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return inner.fetchRaw(q);
            } catch (IOException e) {
                last = e;
                if (attempt < maxAttempts) sleeper.accept(baseDelayMs << (attempt - 1));
            }
        }
        throw new IOException("failed after " + maxAttempts + " attempts: " + last.getMessage(), last);
    }
}
