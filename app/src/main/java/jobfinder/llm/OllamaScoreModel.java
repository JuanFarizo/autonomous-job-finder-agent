package jobfinder.llm;

import java.util.List;

import jobfinder.score.ModelAnswer;
import jobfinder.score.ScoreModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaChatOptions;

/** ScoreModel backed by Spring AI (local Ollama). Reports provider token usage when present. */
public class OllamaScoreModel implements ScoreModel {

    private final ChatModel chat;
    private final OllamaChatOptions options;

    public OllamaScoreModel(ChatModel chat, String model) {
        this.chat = chat;
        this.options = OllamaChatOptions.builder().model(model).disableThinking().build();
    }

    @Override
    public ModelAnswer call(String systemPrompt, String userPrompt) {
        ChatResponse r = chat.call(new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)), options));
        var usage = r.getMetadata() == null ? null : r.getMetadata().getUsage();
        Integer in = usage == null || usage.getPromptTokens() == null || usage.getPromptTokens() == 0 ? null : usage.getPromptTokens().intValue();
        Integer out = usage == null || usage.getCompletionTokens() == null || usage.getCompletionTokens() == 0 ? null : usage.getCompletionTokens().intValue();
        return new ModelAnswer(r.getResult().getOutput().getText(), in, out);
    }
}
