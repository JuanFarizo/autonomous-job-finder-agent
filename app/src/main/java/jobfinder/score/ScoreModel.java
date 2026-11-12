package jobfinder.score;

/** Abstraction over the LLM. A real adapter (Spring AI, local Ollama) is wired in a later step. */
public interface ScoreModel {
    ModelAnswer call(String systemPrompt, String userPrompt);
}
