package jobfinder.score;

/** Raw text returned by the model, with provider-reported token usage (null when the provider gave none). */
public record ModelAnswer(String text, Integer inputTokens, Integer outputTokens) {

    public ModelAnswer(String text) {
        this(text, null, null);
    }
}
